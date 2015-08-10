package com.dataart.vkharitonov.practicechat.client;

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

/**
 * Manages client's connection to the server
 */
public class ChatConnection {

    private final static Logger log = LoggerFactory.getLogger(ChatConnection.class.getName());

    private Socket socket;
    private PrintWriter writer;
    private ServerMessageListener listener;

    public ChatConnection(String username, String password, String host, int port, ServerMessageListener listener) throws IOException {
        this.listener = listener;

        socket = new Socket(host, port);
        writer = new PrintWriter(new OutputStreamWriter(socket.getOutputStream(), StandardCharsets.UTF_8), true);
        MessageProducer producer = new MessageProducer();
        producer.start(socket.getInputStream(), new MessageConsumer());
        writeConnectMessage(username, password);
    }

    /**
     * Checks if this client is accepting and sending messages
     *
     * @return true, if connection is established
     */
    public boolean isConnected() {
        return socket != null && !socket.isClosed();
    }

    /**
     * Closes connection. No more messages are accepted or sent
     */
    private void close() {
        Util.closeQuietly(socket);
        socket = null;
        writer = null;
    }

    /**
     *  Sends `disconnect` message to the server. If it wasn't successful, just closes the connection
     */
    public void disconnect() {
        try {
            if (socket != null) {
                sendMessage(Message.MessageType.DISCONNECT, null);
            }
        } catch (IOException e) {
            log.warn("Couldn't send disconnect message");
        } finally {
            close();
        }
    }

    /**
     * Sends `list_users` message to the server
     * @throws IOException
     */
    public void listUsers(String roomName) throws IOException {
        if (roomName == null) {
            sendMessage(Message.MessageType.LIST_USERS, null);
        } else {
            sendMessage(Message.MessageType.LIST_USERS, new ListUsersInMessage(roomName));
        }
    }

    public void listRooms() throws IOException {
        sendMessage(Message.MessageType.LIST_ROOMS, null);
    }

    public void getHistory(String username) throws IOException {
        sendMessage(Message.MessageType.GET_HISTORY, new GetHistoryInMessage(username));
    }

    public void joinRoom(String name) throws IOException {
        sendMessage(Message.MessageType.JOIN_ROOM, new JoinRoomInMessage(name));
    }
    /**
     * Sends `send_message` message to the server
     * @param destination user to send this message to
     * @param message message body
     * @throws IOException
     */
    public void sendMessage(String destination, String message) throws IOException {
        sendMessage(Message.MessageType.SEND_MESSAGE, new SendMsgInMessage(destination, message));
    }

    private void writeConnectMessage(String username, String password) throws IOException {
        sendMessage(Message.MessageType.CONNECT, new ConnectInMessage(username, password));
    }

    private <T> void sendMessage(Message.MessageType type, T payload) throws IOException {
        if (writer != null) {
            Message message = new Message(type, payload);
            writer.println(JsonUtils.GSON.toJson(message));
            if (writer.checkError()) {
                throw new IOException("Couldn't write to socket");
            }
        }
    }

    private void handleUserList(Message message) {
        UserListOutMessage payload = message.getPayload(UserListOutMessage.class);
        listener.onUserList(payload.getUsers());
    }

    private void handleNewMessage(Message message) {
        NewMsgOutMessage payload = message.getPayload(NewMsgOutMessage.class);
        listener.onNewMessage(payload.getUsername(), Optional.ofNullable(payload.getRoom()), payload.getMessage(), payload
                .isOnline(), payload.getTimestamp());
    }

    private void handleMsgSent(Message message) {
        MsgSentOutMessage payload = message.getPayload(MsgSentOutMessage.class);
        listener.onMessageSent(payload.getUsername());
    }

    private void handleConnectionResult(Message message) {
        ConnectionResultOutMessage payload = message.getPayload(ConnectionResultOutMessage.class);
        listener.onConnectionResult(payload.isSuccess(), payload.isUserExists());
    }

    private void handleMessageHistory(Message message) {
        MsgHistoryOutMessage msg = message.getPayload(MsgHistoryOutMessage.class);
        listener.onMessageHistory(msg.getMessages());
    }

    private void handleRoomJoined(Message message) {
        RoomJoinedOutMessage msg = message.getPayload(RoomJoinedOutMessage.class);
        listener.onRoomJoined(msg.getRoomName(), msg.isRoomExists());
    }

    private void handleRoomList(Message message) {
        RoomListOutMessage msg = message.getPayload(RoomListOutMessage.class);
        listener.onRoomList(msg.getRooms());
    }

    private class MessageConsumer implements MessageProducer.Consumer {
        @Override
        public void onNext(Message message) {
            switch (message.getMessageType()) {
                case CONNECTION_RESULT:
                    handleConnectionResult(message);
                    break;
                case MESSAGE_SENT:
                    handleMsgSent(message);
                    break;
                case NEW_MESSAGE:
                    handleNewMessage(message);
                    break;
                case USER_LIST:
                    handleUserList(message);
                    break;
                case MESSAGE_HISTORY:
                    handleMessageHistory(message);
                    break;
                case ROOM_JOINED:
                    handleRoomJoined(message);
                    break;
                case ROOM_LIST:
                    handleRoomList(message);
                    break;
                default:
                    break;
            }
        }

        @Override
        public void onError(Throwable e) {
            listener.onDisconnect();
            close();
        }

        @Override
        public void onCompleted() {
            listener.onDisconnect();
            close();
        }
    }
}
