package com.dataart.vkharitonov.practicechat.server.net;

import com.dataart.vkharitonov.practicechat.common.json.*;
import com.dataart.vkharitonov.practicechat.common.util.JsonUtils;
import com.dataart.vkharitonov.practicechat.server.db.*;
import com.dataart.vkharitonov.practicechat.server.exception.UserConnectException;
import com.dataart.vkharitonov.practicechat.server.utils.FutureUtils;
import com.dataart.vkharitonov.practicechat.server.utils.HashUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

/**
 * Maintains a list of connected clients. Passes messages between users.
 */
public final class InteractorManager {

    private final static Logger log = LoggerFactory.getLogger(InteractorManager.class.getName());
    private static final int CONNECTION_FAILURE_TIMEOUT = 1000;

    private final UserList clients = new UserList();

    public InteractorManager() {
    }

    public void disconnect(String username) {
        clients.removeInteractor(username).thenRunAsync(() -> log.info("User {} has disconnected", username));
    }

    public CompletableFuture<Boolean> sendMessage(String sender, long timestamp, SendMsgInMessage sendMessage) {
        getMsgDao().addUndeliveredMsg(new ChatMsgDto(sender, sendMessage.getUsername(), sendMessage.getMessage(), timestamp, false));
        return sendMessageToClient(sender, sendMessage.getUsername(), sendMessage.getMessage(), timestamp);
    }

    public CompletableFuture<UserListOutMessage> listUsers(Optional<String> roomName) {
        CompletableFuture<List<String>> usernamesFuture;
        usernamesFuture = roomName.isPresent()
                ? getRoomDao().getUsersForRoom(roomName.get())
                : clients.usersList();

        return usernamesFuture.thenApplyAsync(this::wrapListUsersResult);
    }

    public CompletableFuture<MsgHistoryOutMessage> getHistory(String sender, String partner) {
        return getMsgDao().getHistoryForUsers(sender, partner);
    }

    public CompletableFuture<RoomJoinedOutMessage> joinRoom(String user, String roomName) {
        return joinUserToRoomOrCreate(roomName, user).thenApplyAsync(roomExists -> {
            if (roomExists) {
                log.info("User {} joined room {}", user, roomName);
            } else {
                log.info("User {} created room {}", user, roomName);
            }

            return new RoomJoinedOutMessage(roomName, roomExists);
        });
    }

    public void connectUser(String username, String password, Socket client) {
        getUserDao().getUserByName(username).thenComposeAsync(userDtoOptional -> {
            if (userDtoOptional.isPresent()) {
                if (authenticateUser(userDtoOptional.get(), password)) {
                    return addInteractor(username, client, true);
                } else {
                    throw new UserConnectException(true, "Invalid password");
                }
            } else {
                return createUser(username, password).thenComposeAsync(aVoid ->
                        addInteractor(username, client, false));
            }
        }).handleAsync((connectionResult, e) -> {
            if (e == null) {
                ClientInteractor clientInteractor = connectionResult.getClientInteractor();
                clientInteractor.sendConnectMessage(new ConnectionResultOutMessage(true, connectionResult.isUserExists()));
                log.info("User {} has connected", username);
                sendUndeliveredMsgs(username);
            } else if (e.getCause() instanceof UserConnectException) {
                log.info("Error connecting user {}: {}", username, e.getMessage());
                sendConnectionFailure(client, ((UserConnectException) e.getCause()).isUserExists());
            } else {
                log.info("Couldn't read from client: {}", e.getMessage());
                sendConnectionFailure(client, false);
            }

            return null;
        });
    }

    private CompletableFuture<ConnectionResult> addInteractor(String username, Socket client, boolean userExists) {
        try {
            ClientInteractor clientInteractor = new ClientInteractor(username, client, this);
            return clients.addInteractor(username, clientInteractor).thenApplyAsync(prevInteractor -> {
                if (prevInteractor != null) {
                    prevInteractor.shutdown();
                }

                return new ConnectionResult(clientInteractor, userExists);
            });
        } catch (IOException e) {
            return FutureUtils.failure(e);
        }
    }

    private boolean authenticateUser(UserDto user, String password) {
        String salt = user.getSalt();
        String hash = HashUtils.hash(password, salt);

        return Objects.equals(hash, user.getHash());
    }

    public void shutdown() {
        clients.removeAllAndShutdown().thenAcceptAsync(clientInteractors ->
                clientInteractors.forEach(ClientInteractor::shutdown));
    }

    private CompletableFuture<Void> createUser(String username, String password) {
        String salt = HashUtils.newSalt();
        String hash = HashUtils.hash(password, salt);

        return getUserDao().createUser(username, hash, salt);
    }

    private CompletableFuture<Boolean> sendMessageToClient(String sender, String destination, String message, long timestamp) {
        ClientInteractor interactor = clients.getInteractor(destination);
        if (interactor != null) {
            return interactor.sendNewMessage(new NewMsgOutMessage(sender, message, true, timestamp))
                             .thenComposeAsync(o1 -> {
                                 ClientInteractor senderInteractor = clients.getInteractor(sender);

                                 if (senderInteractor != null) {
                                     return senderInteractor.sendMsgSentMessage(new MsgSentOutMessage(destination));
                                 } else {
                                     return CompletableFuture.completedFuture(null);
                                 }
                             })
                             .thenComposeAsync(o1 -> getMsgDao().setOldestMessageDelivered(destination))
                             .thenApply(o1 -> true);
        } else {
            return CompletableFuture.completedFuture(false);
        }
    }

    /**
     * @return Future with boolean which tells if the room already exists
     */
    private CompletableFuture<Boolean> joinUserToRoomOrCreate(String roomName, String user) {
        return getRoomDao().getRoomAdmin(roomName).thenComposeAsync(adminOptional -> {
            if (adminOptional.isPresent()) {
                return getRoomDao().addUserToRoom(roomName, user).thenApply(o -> true);
            } else {
                return getRoomDao().createRoom(roomName, user).thenApply(o -> false);
            }
        });
    }

    private void sendUndeliveredMsgs(String username) {
        getMsgDao().getUndeliveredMsgsForUser(username)
                   .thenAcceptAsync(undeliveredMsgs -> undeliveredMsgs.forEach(msg ->
                           sendMessageToClient(msg.getSender(), username, msg.getMessage(), msg.getSendingTime()
                                                                                               .getTime())));
    }

    /**
     * Sends `connection failed` message and closes socket
     *
     * @param clientSocket client socket
     */
    private CompletableFuture<Void> sendConnectionFailure(Socket clientSocket, boolean userExists) {
        return CompletableFuture.runAsync(() -> {
            String message = JsonUtils.GSON.toJson(new Message(Message.MessageType.CONNECTION_RESULT, new ConnectionResultOutMessage(false, userExists)));
            try (PrintWriter out = new PrintWriter(new OutputStreamWriter(clientSocket.getOutputStream(), StandardCharsets.UTF_8), true)) {
                clientSocket.setSoTimeout(CONNECTION_FAILURE_TIMEOUT);
                out.println(message);
            } catch (IOException e) {
                log.info("Could not send connection failure to the user: {}", e.getMessage());
            }
        });
    }

    private UserListOutMessage wrapListUsersResult(Collection<String> usernames) {
        return new UserListOutMessage(
                usernames.stream()
                         .map(username -> new UserListOutMessage.User(username, clients.isOnline(username)))
                         .collect(Collectors.toList()));
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

    private static class ConnectionResult {
        private ClientInteractor clientInteractor;
        private boolean userExists;

        public ConnectionResult(ClientInteractor clientInteractor, boolean userExists) {
            this.clientInteractor = clientInteractor;
            this.userExists = userExists;
        }

        public ClientInteractor getClientInteractor() {
            return clientInteractor;
        }

        public boolean isUserExists() {
            return userExists;
        }
    }
}
