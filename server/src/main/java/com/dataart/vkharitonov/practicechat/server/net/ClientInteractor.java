package com.dataart.vkharitonov.practicechat.server.net;

import com.dataart.vkharitonov.practicechat.common.json.*;
import com.dataart.vkharitonov.practicechat.common.util.JsonUtils;
import com.dataart.vkharitonov.practicechat.common.util.MessageProducer;
import com.dataart.vkharitonov.practicechat.server.event.*;
import com.dataart.vkharitonov.practicechat.server.queue.EventListener;
import com.dataart.vkharitonov.practicechat.server.queue.EventQueue;
import com.dataart.vkharitonov.practicechat.server.queue.Subscribe;
import org.apache.commons.net.io.Util;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

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
 * Replies with {@link MsgSentRequest} as soon as the message is sent</li>
 * <li>{@link MsgSentRequest} - asks the interaction manager to send a "message sent" ack to some user</li>
 * <li>{@link DisconnectRequest} - disconnects current user, notifies the interaction manager and shuts down</li>
 * <li>{@link ShutdownCommand} - shuts down without notifying the interactor manager</li>
 * </ul>
 */
public final class ClientInteractor implements EventListener {

    private final static Logger log = LoggerFactory.getLogger(ClientInteractor.class.getName());


    private PrintWriter writer;
    private String username;
    private Socket clientSocket;
    private ExecutorService executor = Executors.newSingleThreadExecutor();
    private InteractorManager interactorManager;

    private EventQueue eventQueue;
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

        eventQueue = new EventQueue();
        eventQueue.start(this);

        messageProducer = new MessageProducer();
        messageProducer.start(clientSocket.getInputStream(), new MessageConsumer());
    }

    @Override
    public void post(Object event) {
        eventQueue.post(event);
    }

    /**
     * Shuts down without notifying the interactor manager
     */
    @Subscribe
    private void handleShutdownCommand(ShutdownCommand message) {
        shutdown();
    }

    /**
     * Disconnects current user, notifies the interaction manager and shuts down
     */
    @Subscribe
    private void handleDisconnectRequest(DisconnectRequest message) {
        shutdown();
        interactorManager.post(message);
    }

    /**
     * Asks the interaction manager to return a user list
     */
    @Subscribe
    private void handleListUsersRequest(ListUsersRequest message) {
        sendToManager(message);
    }

    /**
     * Asks the interaction manager to send a message to some user
     */
    @Subscribe
    private void handleSendMsgRequest(SendMsgRequest message) {
        sendToManager(message);
    }

    /**
     * Asks the interaction manager to send a "message sent" ack to some user
     */
    @Subscribe
    private void handleMsgSentRequest(MsgSentRequest message) {
        sendToManager(message);
    }

    /**
     * Sends "message sent" acknowledgement to the current user
     */
    @Subscribe
    private void sendMsgSentMessage(MsgSentOutMessage message) {
        sendMessageToClient(Message.MessageType.MESSAGE_SENT, message);
    }

    /**
     * Sends the received user list to the user
     */
    @Subscribe
    private void sendUserListMessage(UserListOutMessage message) {
        sendMessageToClient(Message.MessageType.USER_LIST, message);
    }

    /**
     * Sends an acknowledgement to the newly connected user
     */
    @Subscribe
    private void sendConnectMessage(ConnectionResultOutMessage message) {
        sendMessageToClient(Message.MessageType.CONNECTION_RESULT, message);
    }

    /**
     * Sends a message to the current user
     */
    @Subscribe
    private void sendNewMessage(NewMsgOutMessage message) {
        CompletableFuture<Void> future = sendMessageToClient(Message.MessageType.NEW_MESSAGE, message);
        future.thenRun(() -> eventQueue.post(new MsgSentRequest(username, message.getUsername())))
              .exceptionally(e -> {
                  log.warn("Failed to send message to {}: {}", username, e.getLocalizedMessage());
                  return null;
              });
    }

    private <T> CompletableFuture<Void> sendMessageToClient(Message.MessageType type, T payload) {
        Message message = new Message(type, payload);
        String json = JsonUtils.GSON.toJson(message);
        return CompletableFuture.runAsync(() -> writeToClient(json), executor);
    }

    private void shutdown() {
        executor.shutdown();
        messageProducer.stop();
        closeConnection();
        eventQueue.stop();
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

    private class MessageConsumer implements MessageProducer.Consumer {
        @Override
        public void onNext(Message message) {
            log.info("Received message from {}: {}", username, message);

            if (message.getMessageType() != null) {
                switch (message.getMessageType()) {
                    case LIST_USERS:
                        eventQueue.post(new ListUsersRequest(username));
                        break;
                    case DISCONNECT:
                        messageProducer.stop();
                        break;
                    case SEND_MESSAGE:
                        SendMsgInMessage msg = JsonUtils.GSON.fromJson(message.getPayload(), SendMsgInMessage.class);
                        eventQueue.post(new SendMsgRequest(username, msg));
                        break;
                    default:
                        log.warn("Unexpected message from {}", username);
                        break;
                }
            }
        }

        @Override
        public void onError(Throwable e) {
            log.info("Error reading from user {}: {}", username, e.getLocalizedMessage());
            if (eventQueue.isRunning()) {
                eventQueue.post(new DisconnectRequest(username));
            }
        }

        @Override
        public void onCompleted() {
            eventQueue.post(new DisconnectRequest(username));
        }
    }
}
