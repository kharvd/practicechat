package com.dataart.vkharitonov.practicechat.server.net;

import com.dataart.vkharitonov.practicechat.common.json.*;
import com.dataart.vkharitonov.practicechat.common.util.JsonUtils;
import com.dataart.vkharitonov.practicechat.server.db.*;
import com.dataart.vkharitonov.practicechat.server.event.*;
import com.dataart.vkharitonov.practicechat.server.queue.EventListener;
import com.dataart.vkharitonov.practicechat.server.queue.EventQueue;
import com.dataart.vkharitonov.practicechat.server.queue.Subscribe;
import com.dataart.vkharitonov.practicechat.server.utils.HashUtils;
import org.apache.commons.net.io.Util;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Maintains a list of connected clients. Passes messages between users.
 * <p>
 * Handles following messages:
 * <ul>
 * <li>{@link ConnectionEvent} - registers a new user. Doesn't reply.</li>
 * <li>{@link ListUsersRequest} - replies with {@link UserListOutMessage},
 * containing the list of currently connected users.</li>
 * <li>{@link SendMsgRequest} - sends a message to the user</li>
 * <li>{@link MsgSentRequest} - sends a "message sent" acknowledgement to the user</li>
 * <li>{@link DisconnectRequest} - unregisters a user. Doesn't reply</li>
 * <li>{@link ShutdownCommand} - shuts the manager down</li>
 * </ul>
 */
public final class InteractorManager implements EventListener {

    private final static Logger log = LoggerFactory.getLogger(InteractorManager.class.getName());
    private static final int CONNECTION_FAILURE_TIMEOUT = 1000;

    private Map<String, ClientInteractor> clients = new HashMap<>();
    private ExecutorService writeToClientExecutor;
    private EventQueue eventQueue;

    public InteractorManager() {
        writeToClientExecutor = Executors.newCachedThreadPool();
        eventQueue = new EventQueue();
        eventQueue.start(this);
    }

    @Override
    public void post(Object event) {
        eventQueue.post(event);
    }

    /**
     * Sends a "message sent" acknowledgement to the user
     */
    @Subscribe
    private void handleMessageSentRequest(MsgSentRequest message) {
        String messageSender = message.getMessageSender();
        String destination = message.getSender();
        if (clients.containsKey(messageSender)) {
            ClientInteractor senderClient = clients.get(messageSender);
            senderClient.post(new MsgSentOutMessage(destination));
        }

        getMsgDao().setOldestMessageDelivered(destination);
    }

    /**
     * Sends a message to the user
     */
    @Subscribe
    private void handleSendMessageRequest(SendMsgRequest message) {
        SendMsgInMessage sendMessage = message.getMessage();
        if (sendMessage == null) {
            log.warn("Null message from user {}", message.getSender());
            return;
        }

        String destination = sendMessage.getUsername();
        String sender = message.getSender();

        if (destination == null) {
            log.warn("Destination user can't be null in message sent by {}", sender);
            return;
        }

        getMsgDao().addUndeliveredMsg(message).thenRunAsync(() -> {
            if (clients.containsKey(destination)) {
                ClientInteractor destinationClient = clients.get(destination);
                destinationClient.post(new NewMsgOutMessage(sender, sendMessage.getMessage(), true, message.getTimestamp()));
            }
        }, eventQueue.executor());
    }

    /**
     * Unregisters a user
     */
    @Subscribe
    private void handleDisconnectRequest(DisconnectRequest message) {
        clients.remove(message.getSender());
        log.info("User {} has disconnected", message.getSender());
    }

    /**
     * Replies with {@link UserListOutMessage} containing the list of currently connected users
     */
    @Subscribe
    private void handleListUsersRequest(ListUsersRequest message) {
        ClientInteractor clientInteractor = clients.get(message.getSender());
        if (clientInteractor != null) {
            clientInteractor.post(new UserListOutMessage(clients.keySet()));
        }
    }

    @Subscribe
    private void handleHistoryRequest(HistoryRequest message) {
        getMsgDao().getHistoryForUsers(message.getSender(), message.getPartner())
                   .thenAcceptAsync(msgHistoryOutMessage -> {
                       if (clients.containsKey(message.getSender())) {
                           clients.get(message.getSender()).post(msgHistoryOutMessage);
                       }
                   }, eventQueue.executor());
    }

    @Subscribe
    private void handleJoinRoomRequest(JoinRoomRequest request) {
        String user = request.getSender();
        String roomName = request.getRoomName();

        joinUserToRoomOrCreate(roomName, user).thenAcceptAsync(roomExists -> {
            if (roomExists) {
                log.info("User {} joined room {}", user, roomName);
            } else {
                log.info("User {} created room {}", user, roomName);
            }

            if (clients.containsKey(user)) {
                clients.get(user).post(new RoomJoinedOutMessage(roomName, roomExists));
            }
        }, eventQueue.executor());
    }

    /**
     * Registers a new user
     */
    @Subscribe
    private void handleConnectionEvent(ConnectionEvent message) {
        String username = message.getConnectMessage().getUsername();
        String password = message.getConnectMessage().getPassword();
        Socket client = message.getClient();

        if (username == null) {
            log.info("Username can't be null from {}", client.getInetAddress());
            writeToClientExecutor.submit(() -> sendConnectionFailure(client, false));
            return;
        }

        if (clients.containsKey(username)) {
            log.info("User {} is already connected", username);
            writeToClientExecutor.submit(() -> sendConnectionFailure(client, true));
        }

        getUserDao().getUserByName(username).thenAcceptAsync(userDtoOptional -> {
            if (userDtoOptional.isPresent()) {
                if (authenticateUser(userDtoOptional.get(), password)) {
                    connectUser(username, client, true);
                } else {
                    log.info("Invalid password for user {}", username);
                    writeToClientExecutor.submit(() -> sendConnectionFailure(client, true));
                }
            } else {
                log.info("Creating user {}", username);
                createUser(username, password, client);
            }
        }, eventQueue.executor());
    }

    /**
     * Shuts the manager down, disconnecting all users
     */
    @Subscribe
    private void handleShutdownCommand(ShutdownCommand shutdownCommand) {
        for (ClientInteractor clientInteractor : clients.values()) {
            clientInteractor.post(new ShutdownCommand());
        }

        clients.clear();
        eventQueue.stop();
        writeToClientExecutor.shutdown();
    }

    private boolean authenticateUser(UserDto user, String password) {
        String salt = user.getSalt();
        String hash = HashUtils.hash(password, salt);

        return Objects.equals(hash, user.getHash());
    }

    private void createUser(String username, String password, Socket client) {
        String salt = HashUtils.newSalt();
        String hash = HashUtils.hash(password, salt);

        getUserDao().createUser(username, hash, salt)
                    .thenRunAsync(() -> connectUser(username, client, false), eventQueue.executor());
    }

    private void connectUser(String username, Socket client, boolean userExists) {
        try {
            ClientInteractor clientInteractor = new ClientInteractor(username, client, this);
            clients.put(username, clientInteractor);
            clientInteractor.post(new ConnectionResultOutMessage(true, userExists));

            sendUndeliveredMsgs(username);
            log.info("User {} has connected", username);
        } catch (IOException e) {
            log.warn("Could not read from client {}", client.getInetAddress());
            Util.closeQuietly(client);
        }
    }

    /**
     * @return Future with boolean which tells if the room already exists
     */
    private CompletableFuture<Boolean> joinUserToRoomOrCreate(String roomName, String user) {
        return getRoomDao().getRoomAdmin(roomName).thenCompose(adminOptional -> {
            if (adminOptional.isPresent()) {
                return getRoomDao().addUserToRoom(roomName, user).thenApply(o -> true);
            } else {
                return getRoomDao().createRoom(roomName, user).thenApply(o -> false);
            }
        });
    }

    private void sendUndeliveredMsgs(String username) {
        getMsgDao().getUndeliveredMsgsForUser(username)
                   .thenAcceptAsync(undeliveredMsgs -> undeliveredMsgs.forEach(sendMsgRequest -> {
                       SendMsgInMessage sendMessage = sendMsgRequest.getMessage();
                       String sender = sendMsgRequest.getSender();
                       if (clients.containsKey(username)) {
                           clients.get(username)
                                  .post(new NewMsgOutMessage(sender, sendMessage.getMessage(), clients.containsKey(sender),
                                          sendMsgRequest.getTimestamp()));
                       }
                   }), eventQueue.executor());
    }

    /**
     * Sends `connection failed` message and closes socket
     *
     * @param clientSocket client socket
     */
    private void sendConnectionFailure(Socket clientSocket, boolean userExists) {
        String message = JsonUtils.GSON.toJson(new Message(Message.MessageType.CONNECTION_RESULT, new ConnectionResultOutMessage(false, userExists)));
        try (PrintWriter out = new PrintWriter(new OutputStreamWriter(clientSocket.getOutputStream(), StandardCharsets.UTF_8), true)) {
            clientSocket.setSoTimeout(CONNECTION_FAILURE_TIMEOUT);
            out.println(message);
        } catch (IOException e) {
            log.info("Could not send connection failure to the user: {}", e.getMessage());
        }
    }

    private ChatMsgDao getMsgDao() {
        return DbHelper.getInstance().getMsgDao();
    }

    private UserDao getUserDao() {
        return DbHelper.getInstance().getUserDao();
    }

    private RoomDao getRoomDao() {
        return DbHelper.getInstance().getRoomDao();
    }
}
