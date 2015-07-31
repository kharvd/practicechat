package com.dataart.vkharitonov.practicechat.net;

import com.dataart.vkharitonov.practicechat.json.*;
import com.dataart.vkharitonov.practicechat.message.DisconnectRequest;
import com.dataart.vkharitonov.practicechat.message.ListUsersRequest;
import com.dataart.vkharitonov.practicechat.message.MessageSentRequest;
import com.dataart.vkharitonov.practicechat.message.SendMessageRequest;
import com.dataart.vkharitonov.practicechat.util.JsonUtils;
import com.google.gson.JsonSyntaxException;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonToken;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.logging.Logger;

/**
 * Interacts with the clients.
 * <p>
 * Handles following messages:
 * <ul>
 * <li>{@link ConnectionResultMessage} - sends an acknowledgement to the newly connected user</li>
 * <li>{@link UserListMessage} - sends the received user list to the user</li>
 * <li>{@link NewMessage} - sends a message to the current user</li>
 * <li>{@link MessageSent} - sends "message sent" acknowledgement to the current user</li>
 *
 * <li>{@link ListUsersRequest} - asks the interaction manager to return a user list</li>
 * <li>{@link SendMessageRequest} - asks the interaction manager to send a message to some user.
 * Must reply with {@link MessageSentRequest} as soon as the message is sent</li>
 * <li>{@link MessageSentRequest} - asks the interaction manager to send a "message sent" ack to some user</li>
 * <li>{@link DisconnectRequest} - disconnects current user, notifies the interaction manager and shuts down</li>
 * </ul>
 */
public final class ClientInteractor extends MessageListener {

    private final static Logger log = Logger.getLogger(ClientInteractor.class.getName());

    private PrintWriter writer;
    private String username;
    private Socket clientSocket;
    private ExecutorService executor = Executors.newSingleThreadExecutor();
    private InteractorManager interactorManager;
    private volatile boolean isReadRunning;

    /**
     * @param username username associated with the client
     * @param clientSocket client's socket
     * @param interactorManager manager
     * @throws IOException thrown if couldn't get output stream from a socket
     */
    public ClientInteractor(String username, Socket clientSocket, InteractorManager interactorManager) throws IOException {
        super();
        this.username = username;
        this.clientSocket = clientSocket;
        this.interactorManager = interactorManager;

        writer = new PrintWriter(clientSocket.getOutputStream(), true);

        startMessageQueue();
        isReadRunning = true;
        new SocketReadThread().start();
    }

    @Override
    protected void handleMessage(Object message) {
        if (message instanceof ConnectionResultMessage) {
            sendMessageToClient(Message.MessageType.CONNECTION_RESULT, message);
        } else if (message instanceof UserListMessage) {
            sendMessageToClient(Message.MessageType.USER_LIST, message);
        } else if (message instanceof NewMessage) {
            sendNewMessage((NewMessage) message);
        } else if (message instanceof MessageSent) {
            sendMessageToClient(Message.MessageType.MESSAGE_SENT, message);
        } else if (message instanceof ListUsersRequest ||
                message instanceof SendMessageRequest ||
                message instanceof MessageSentRequest) {
            sendToManager(message);
        } else if (message instanceof DisconnectRequest) {
            handleDisconnectRequest(message);
        }
    }

    private <T> CompletableFuture<Void> sendMessageToClient(Message.MessageType type, T payload) {
        Message message = new Message(type, JsonUtils.GSON.toJsonTree(payload));
        String json = JsonUtils.GSON.toJson(message);
        return CompletableFuture.runAsync(() -> writeToClient(json), executor);
    }

    private void sendNewMessage(NewMessage message) {
        CompletableFuture<Void> future = sendMessageToClient(Message.MessageType.NEW_MESSAGE, message);
        future.thenRun(() -> sendMessage(new MessageSentRequest(username, message.getUsername())));
    }

    private void handleDisconnectRequest(Object message) {
        isReadRunning = false;
        closeConnection();
        stopMessageQueue();
        executor.shutdown();
        interactorManager.handleMessage(message);
    }

    private void sendToManager(Object message) {
        interactorManager.sendMessage(message);
    }

    private void closeConnection() {
        if (!clientSocket.isClosed()) {
            try {
                clientSocket.close();
            } catch (IOException e) {
                log.warning("Couldn't close socket for user " + username);
            }
        }
    }

    private void writeToClient(String message) {
        writer.println(message);
    }

    private class SocketReadThread extends Thread {
        @Override
        public void run() {
            try (BufferedReader in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
                 JsonReader reader = new JsonReader(in)) {
                reader.setLenient(true);
                while (isReadRunning && reader.peek() != JsonToken.END_DOCUMENT) {
                    Message message = JsonUtils.GSON.fromJson(reader, Message.class);

                    log.info("Received message from " + username + ": " + message);
                    if (message != null) {
                        deliverMessage(message);
                    } else {
                        disconnectRequest();
                    }
                }
            } catch (JsonSyntaxException | IOException e) {
                log.info("Error reading from user " + username + ": " + e.getLocalizedMessage());
                disconnectRequest();
            }
        }

        private void deliverMessage(Message message) {
            if (message.getMessageType() != null) {
                switch (message.getMessageType()) {
                    case LIST_USERS:
                        sendMessage(new ListUsersRequest(username));
                        break;
                    case DISCONNECT:
                        disconnectRequest();
                        break;
                    case SEND_MESSAGE:
                        SendMessage msg = JsonUtils.GSON.fromJson(message.getPayload(), SendMessage.class);
                        sendMessage(new SendMessageRequest(username, msg));
                    default:
                        break;
                }
            }
        }

        private void disconnectRequest() {
            isReadRunning = false;
            sendMessage(new DisconnectRequest(username));
        }
    }
}
