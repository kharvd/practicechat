package com.dataart.vkharitonov.practicechat.server.net;

import com.dataart.vkharitonov.practicechat.common.json.*;
import com.dataart.vkharitonov.practicechat.common.util.JsonUtils;
import com.dataart.vkharitonov.practicechat.server.db.DbUtils;
import com.dataart.vkharitonov.practicechat.server.request.*;

import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Maintains a list of connected clients. Passes messages between users.
 * <p>
 * Handles following messages:
 * <ul>
 * <li>{@link ConnectionRequest} - registers a new user. Doesn't reply.</li>
 * <li>{@link ListUsersRequest} - replies to with {@link UserListOutMessage},
 * containing the list of currently connected users.</li>
 * <li>{@link SendMsgRequest} - sends a message to the user</li>
 * <li>{@link MsgSentRequest} - sends a "message sent" acknowledgement to the user</li>
 * <li>{@link DisconnectRequest} - unregisters a user. Doesn't reply</li>
 * <li>{@link ShutdownRequest} - shuts the manager down</li>
 * </ul>
 */
public final class InteractorManager implements MessageListener {

    private final static Logger log = Logger.getLogger(InteractorManager.class.getName());
    private static final int CONNECTION_FAILURE_TIMEOUT = 1000;

    private Map<String, ClientInteractor> clients = new HashMap<>();
    private ExecutorService writeToClientExecutor;
    private MessageQueue messageQueue;

    public InteractorManager() {
        writeToClientExecutor = Executors.newCachedThreadPool();
        messageQueue = new ManagerMessageQueue();
        messageQueue.start();
    }

    @Override
    public void post(Object message) {
        messageQueue.post(message);
    }

    private void shutdown() {
        messageQueue.stop();
        writeToClientExecutor.shutdown();
    }

    private void handleMessageSentRequest(MsgSentRequest message) {
        String messageSender = message.getMessageSender();
        String destination = message.getSender();
        if (clients.containsKey(messageSender)) {
            ClientInteractor senderClient = clients.get(messageSender);
            senderClient.post(new MsgSentOutMessage(destination));
        }

        DbUtils.removeOldestMessage(destination);
    }

    private void handleSendMessageRequest(SendMsgRequest message) {
        SendMsgInMessage sendMessage = message.getMessage();
        if (sendMessage == null) {
            log.warning("Null message from user " + message.getSender());
            return;
        }

        String destination = sendMessage.getUsername();
        String sender = message.getSender();

        if (destination == null) {
            log.warning("Destination user can't be null in message sent by " + sender);
            return;
        }

        DbUtils.addUndeliveredMsg(message);

        if (clients.containsKey(destination)) {
            ClientInteractor destinationClient = clients.get(destination);
            destinationClient.post(new NewMsgOutMessage(sender, sendMessage.getMessage(), true, message.getTimestamp()));
        }
    }

    private void handleDisconnectRequest(DisconnectRequest message) {
        clients.remove(message.getSender());
        log.info("User " + message.getSender() + " has disconnected");
    }

    private void handleListUsersRequest(ListUsersRequest message) {
        ClientInteractor clientInteractor = clients.get(message.getSender());
        if (clientInteractor != null) {
            clientInteractor.post(new UserListOutMessage(clients.keySet()));
        }
    }

    private void handleConnectionRequest(ConnectionRequest message) {
        String username = message.getConnectMessage().getUsername();
        Socket client = message.getClient();

        if (username == null) {
            log.info("Username can't be null");
            writeToClientExecutor.submit(() -> sendConnectionFailure(client));
        } else if (clients.containsKey(username)) {
            log.info("User " + username + " is already connected");
            writeToClientExecutor.submit(() -> sendConnectionFailure(client));
        } else {
            connectUser(username, client);
        }
    }

    private void connectUser(String username, Socket client) {
        try {
            ClientInteractor clientInteractor = new ClientInteractor(username, client, this);
            clientInteractor.post(new ConnectionResultOutMessage(true));
            clients.put(username, clientInteractor);

            sendNonDeliveredMsgs(username);
            log.info("User " + username + " has connected");
        } catch (IOException e) {
            log.warning("Could not read from client " + client.getInetAddress());
            if (!client.isClosed()) {
                try {
                    client.close();
                } catch (IOException e1) {
                    log.warning("Could not close socket " + client.getInetAddress());
                }
            }
        }
    }

    private void sendNonDeliveredMsgs(String username) {
        DbUtils.getUndeliveredMsgsForUser(username).thenApply(undeliveredMsgs -> {
            undeliveredMsgs.forEach(sendMsgRequest -> {
                SendMsgInMessage sendMessage = sendMsgRequest.getMessage();
                String sender = sendMsgRequest.getSender();
                clients.get(username).post(new NewMsgOutMessage(sender, sendMessage.getMessage(), clients.containsKey(sender),
                        sendMsgRequest.getTimestamp()));
            });

            return null;
        });
    }

    /**
     * Sends `connection failed` message and closes socket
     *
     * @param clientSocket client socket
     */
    private void sendConnectionFailure(Socket clientSocket) {
        String message = JsonUtils.GSON.toJson(new Message(Message.MessageType.CONNECTION_RESULT, new ConnectionResultOutMessage(false)));
        try (PrintWriter out = new PrintWriter(new OutputStreamWriter(clientSocket.getOutputStream(), StandardCharsets.UTF_8), true)) {
            clientSocket.setSoTimeout(CONNECTION_FAILURE_TIMEOUT);
            out.println(message);
        } catch (IOException e) {
            log.log(Level.WARNING, e.getMessage());
        }
    }

    private class ManagerMessageQueue extends MessageQueue {
        @Override
        protected void handleMessage(Object message) {
            if (message instanceof ConnectionRequest) {
                handleConnectionRequest((ConnectionRequest) message);
            } else if (message instanceof ListUsersRequest) {
                handleListUsersRequest((ListUsersRequest) message);
            } else if (message instanceof SendMsgRequest) {
                handleSendMessageRequest((SendMsgRequest) message);
            } else if (message instanceof MsgSentRequest) {
                handleMessageSentRequest((MsgSentRequest) message);
            } else if (message instanceof DisconnectRequest) {
                handleDisconnectRequest((DisconnectRequest) message);
            } else if (message instanceof ShutdownRequest) {
                shutdown();
            }
        }

        @Override
        protected void handleError(Throwable e) {
            log.log(Level.SEVERE, e, () -> "Exception during message handling in " +
                    InteractorManager.this + ". Shutting down the server");
        }
    }
}
