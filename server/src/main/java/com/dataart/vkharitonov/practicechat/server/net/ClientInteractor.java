package com.dataart.vkharitonov.practicechat.server.net;

import com.dataart.vkharitonov.practicechat.common.json.*;
import com.dataart.vkharitonov.practicechat.common.util.JsonUtils;
import com.dataart.vkharitonov.practicechat.common.util.MessageProducer;
import com.dataart.vkharitonov.practicechat.server.request.*;
import org.apache.commons.net.io.Util;

import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Interacts with the clients.
 * <p>
 * Handles following messages:
 * <ul>
 * <li>{@link ConnectionResultOutMessage} - sends an acknowledgement to the newly connected user</li>
 * <li>{@link UserListOutMessage} - sends the received user list to the user</li>
 * <li>{@link NewMsgOutMessage} - sends a message to the current user</li>
 * <li>{@link MsgSentOutMessage} - sends "message sent" acknowledgement to the current user</li>
 * <p>
 * <li>{@link ListUsersRequest} - asks the interaction manager to return a user list</li>
 * <li>{@link SendMsgRequest} - asks the interaction manager to send a message to some user.
 * Must reply with {@link MsgSentRequest} as soon as the message is sent</li>
 * <li>{@link MsgSentRequest} - asks the interaction manager to send a "message sent" ack to some user</li>
 * <li>{@link DisconnectRequest} - disconnects current user, notifies the interaction manager and shuts down</li>
 * </ul>
 */
public final class ClientInteractor implements MessageListener {

    private final static Logger log = Logger.getLogger(ClientInteractor.class.getName());

    private PrintWriter writer;
    private String username;
    private Socket clientSocket;
    private ExecutorService executor = Executors.newSingleThreadExecutor();
    private InteractorManager interactorManager;

    private MessageQueue messageQueue;
    private MessageProducer messageProducer;

    /**
     * @param username          username associated with the client
     * @param clientSocket      client's socket
     * @param interactorManager manager
     * @throws IOException thrown if couldn't get output stream from a socket
     */
    public ClientInteractor(String username, Socket clientSocket, InteractorManager interactorManager) throws IOException {
        super();
        this.username = username;
        this.clientSocket = clientSocket;
        this.interactorManager = interactorManager;

        writer = new PrintWriter(new OutputStreamWriter(clientSocket.getOutputStream(), StandardCharsets.UTF_8), true);

        messageQueue = new ClientMessageQueue();
        messageQueue.start();

        messageProducer = new MessageProducer();
        messageProducer.start(clientSocket.getInputStream(), new MessageConsumer());
    }

    private <T> CompletableFuture<Void> sendMessageToClient(Message.MessageType type, T payload) {
        Message message = new Message(type, payload);
        String json = JsonUtils.GSON.toJson(message);
        return CompletableFuture.runAsync(() -> writeToClient(json), executor);
    }

    private void sendNewMessage(NewMsgOutMessage message) {
        CompletableFuture<Void> future = sendMessageToClient(Message.MessageType.NEW_MESSAGE, message);
        future.thenRun(() -> messageQueue.post(new MsgSentRequest(username, message.getUsername())))
                .exceptionally(e -> {
                    log.info("Failed to send message to " + username + ": " + e.getLocalizedMessage());
                    return null;
                });
    }

    private void handleDisconnectRequest(DisconnectRequest message) {
        shutdown();
        interactorManager.post(message);
    }

    private void shutdown() {
        executor.shutdown();
        messageProducer.stop();
        closeConnection();
        messageQueue.stop();
    }

    private void sendToManager(Object message) {
        interactorManager.post(message);
    }

    private void closeConnection() {
        if (!clientSocket.isClosed()) {
            Util.closeQuietly(clientSocket);
        }
    }

    private void writeToClient(String message) {
        writer.println(message);
        if (writer.checkError()) {
            throw new RuntimeException(new IOException("Couldn't write to socket for user " + username));
        }
    }

    @Override
    public void post(Object message) {
        messageQueue.post(message);
    }

    private class ClientMessageQueue extends MessageQueue {
        @Override
        protected void handleMessage(Object message) {
            if (message instanceof ConnectionResultOutMessage) {
                sendMessageToClient(Message.MessageType.CONNECTION_RESULT, message);
            } else if (message instanceof UserListOutMessage) {
                sendMessageToClient(Message.MessageType.USER_LIST, message);
            } else if (message instanceof NewMsgOutMessage) {
                sendNewMessage((NewMsgOutMessage) message);
            } else if (message instanceof MsgSentOutMessage) {
                sendMessageToClient(Message.MessageType.MESSAGE_SENT, message);
            } else if (message instanceof ListUsersRequest ||
                    message instanceof SendMsgRequest ||
                    message instanceof MsgSentRequest) {
                sendToManager(message);
            } else if (message instanceof DisconnectRequest) {
                handleDisconnectRequest((DisconnectRequest) message);
            } else if (message instanceof ShutdownRequest) {
                shutdown();
            }
        }

        @Override
        protected void handleError(Throwable e) {
            log.log(Level.SEVERE, e, () -> "Exception during message handling in " +
                    ClientInteractor.this + ". Shutting down the server");
        }
    }

    private class MessageConsumer implements MessageProducer.Consumer {
        @Override
        public void onNext(Message message) {
            log.info("Received message from " + username + ": " + message);

            if (message.getMessageType() != null) {
                switch (message.getMessageType()) {
                    case LIST_USERS:
                        messageQueue.post(new ListUsersRequest(username));
                        break;
                    case DISCONNECT:
                        messageProducer.stop();
                        break;
                    case SEND_MESSAGE:
                        SendMsgInMessage msg = JsonUtils.GSON.fromJson(message.getPayload(), SendMsgInMessage.class);
                        messageQueue.post(new SendMsgRequest(username, msg));
                        break;
                    default:
                        log.info("Unexpected message from " + username);
                        break;
                }
            }
        }

        @Override
        public void onError(Throwable e) {
            log.info("Error reading from user " + username + ": " + e.getLocalizedMessage());
            messageQueue.post(new DisconnectRequest(username));
        }

        @Override
        public void onCompleted() {
            messageQueue.post(new DisconnectRequest(username));
        }
    }
}
