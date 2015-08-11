package com.dataart.vkharitonov.practicechat.server.net;

import com.dataart.vkharitonov.practicechat.common.json.*;
import com.dataart.vkharitonov.practicechat.common.util.JsonUtils;
import com.dataart.vkharitonov.practicechat.common.util.MessageProducer;
import org.apache.commons.net.io.Util;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Interacts with the clients.
 */
public final class ClientInteractor {

    private final static Logger log = LoggerFactory.getLogger(ClientInteractor.class.getName());

    private final PrintWriter writer;
    private final String username;
    private final Socket clientSocket;
    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private final InteractorManager interactorManager;

    private final MessageProducer messageProducer;

    private volatile boolean isShutdown;

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

        isShutdown = false;

        writer = new PrintWriter(new OutputStreamWriter(clientSocket.getOutputStream(), StandardCharsets.UTF_8), true);

        messageProducer = new MessageProducer();
        messageProducer.start(clientSocket.getInputStream(), new MessageConsumer());
    }

    /**
     * Send new message to the client
     *
     * @return {@link CompletableFuture} that completes as soon as the message is sent
     */
    public CompletableFuture<Void> sendNewMessage(NewMsgOutMessage message) {
        return sendMessageToClient(Message.MessageType.NEW_MESSAGE, message);
    }

    /**
     * Sends "message sent" acknowledgement to the current user
     *
     * @return {@link CompletableFuture} that completes as soon as the message is sent
     */
    public CompletableFuture<Void> sendMsgSentMessage(MsgSentOutMessage message) {
        return sendMessageToClient(Message.MessageType.MESSAGE_SENT, message);
    }

    /**
     * Sends an acknowledgement to the newly connected user
     *
     * @return {@link CompletableFuture} that completes as soon as the message is sent
     */
    public CompletableFuture<Void> sendConnectMessage(ConnectionResultOutMessage message) {
        return sendMessageToClient(Message.MessageType.CONNECTION_RESULT, message);
    }

    /**
     * Shuts down the interactor and disconnects the user
     */
    public void shutdown() {
        isShutdown = true;
        executor.shutdown();
        messageProducer.stop();
        closeConnection();
    }

    private <T> CompletableFuture<Void> sendMessageToClient(Message.MessageType type, T payload) {
        Message message = new Message(type, payload);
        String json = JsonUtils.GSON.toJson(message);
        return CompletableFuture.runAsync(() -> writeToClient(json), executor);
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

    private void handleJoinRoomRequest(Message message) {
        JoinRoomInMessage joinRoomMessage = message.getPayload(JoinRoomInMessage.class);
        interactorManager.joinRoom(username, joinRoomMessage.getRoomName())
                         .thenAccept(msg -> sendMessageToClient(Message.MessageType.ROOM_JOINED, msg));
    }

    private void handleLeaveRoomRequest(Message message) {
        LeaveRoomInMessage msg = message.getPayload(LeaveRoomInMessage.class);
        interactorManager.leaveRoom(username, msg.getRoomName())
                         .thenAcceptAsync(m -> sendMessageToClient(Message.MessageType.ROOM_LEFT, m));
    }

    private void handleGetHistoryRequest(Message message) {
        GetHistoryInMessage getHistoryMessage = message.getPayload(GetHistoryInMessage.class);
        interactorManager.getHistory(username, getHistoryMessage.getUsername())
                         .thenAccept(msg -> sendMessageToClient(Message.MessageType.MESSAGE_HISTORY, msg));
    }

    private void handleListUsersRequest(Message message) {
        Optional<String> roomName;
        if (message.getRawPayload() == null || message.getRawPayload().isJsonNull()) {
            roomName = Optional.empty();
        } else {
            ListUsersInMessage listUsersInMessage = message.getPayload(ListUsersInMessage.class);
            roomName = Optional.of(listUsersInMessage.getRoomName());
        }

        interactorManager.listUsers(roomName)
                         .thenAcceptAsync(userListOutMessage ->
                                 sendMessageToClient(Message.MessageType.USER_LIST, userListOutMessage));
    }

    private void handleSendMessageRequest(Message message) {
        SendMsgInMessage msg = message.getPayload(SendMsgInMessage.class);
        interactorManager.sendMessage(username, msg.getUsername(), msg.getMessage(), System.currentTimeMillis());
    }

    private void handleListRoomsRequest() {
        interactorManager.listRooms().thenAcceptAsync(msg -> sendMessageToClient(Message.MessageType.ROOM_LIST, msg));
    }

    /**
     * Disconnects current user, notifies the interaction manager and shuts down
     */
    private void disconnect() {
        if (!isShutdown) {
            shutdown();
            interactorManager.disconnect(username);
        }
    }

    private class MessageConsumer implements MessageProducer.Consumer {
        @Override
        public void onNext(Message message) {
            log.info("Received message from {}: {}", username, message);

            if (message.getMessageType() != null) {
                switch (message.getMessageType()) {
                    case LIST_USERS:
                        handleListUsersRequest(message);
                        break;
                    case DISCONNECT:
                        messageProducer.stop();
                        break;
                    case SEND_MESSAGE:
                        handleSendMessageRequest(message);
                        break;
                    case GET_HISTORY:
                        handleGetHistoryRequest(message);
                        break;
                    case JOIN_ROOM:
                        handleJoinRoomRequest(message);
                        break;
                    case LIST_ROOMS:
                        handleListRoomsRequest();
                        break;
                    case LEAVE_ROOM:
                        handleLeaveRoomRequest(message);
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
            disconnect();
        }

        @Override
        public void onCompleted() {
            disconnect();
        }
    }
}
