package com.dataart.vkharitonov.practicechat.net;

import com.dataart.vkharitonov.practicechat.json.*;
import com.dataart.vkharitonov.practicechat.request.*;
import com.dataart.vkharitonov.practicechat.util.JsonUtils;
import com.google.gson.JsonElement;

import java.io.IOException;
import java.io.PrintWriter;
import java.net.Socket;
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
    private ExecutorService executor;

    private MessageQueue messageQueue;

    public InteractorManager() {
        messageQueue = new ManagerMessageQueue();
        messageQueue.start();
        executor = Executors.newSingleThreadExecutor();
    }

    @Override
    public void post(Object message) {
        messageQueue.post(message);
    }

    private void shutdown() {
        messageQueue.stop();
        executor.shutdown();
    }

    private void handleMessageSentRequest(MsgSentRequest message) {
        String messageSender = message.getMessageSender();
        if (clients.containsKey(messageSender)) {
            ClientInteractor senderClient = clients.get(messageSender);
            senderClient.post(new MsgSentOutMessage(message.getSender(), true));
        }
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

        if (clients.containsKey(destination)) {
            ClientInteractor destinationClient = clients.get(destination);
            destinationClient.post(new NewMsgOutMessage(sender, sendMessage.getMessage(), true));
        } else {
            ClientInteractor senderClient = clients.get(sender);
            senderClient.post(new MsgSentOutMessage(destination, false));
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
            executor.submit(() -> sendConnectionFailure(client));
        } else if (clients.containsKey(username)) {
            log.info("User " + username + " is already connected");
            executor.submit(() -> sendConnectionFailure(client));
        } else {
            connectUser(username, client);
        }
    }

    private void connectUser(String username, Socket client) {
        try {
            ClientInteractor clientInteractor = new ClientInteractor(username, client, this);
            clientInteractor.post(new ConnectionResultOutMessage(true));
            clients.put(username, clientInteractor);
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

    /**
     * Sends `connection failed` message and closes socket
     *
     * @param clientSocket client socket
     */
    private void sendConnectionFailure(Socket clientSocket) {
        JsonElement payload = JsonUtils.GSON.toJsonTree(new ConnectionResultOutMessage(false));
        String message = JsonUtils.GSON.toJson(new Message(Message.MessageType.CONNECTION_RESULT, payload));
        try (PrintWriter out = new PrintWriter(clientSocket.getOutputStream(), true)) {
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
