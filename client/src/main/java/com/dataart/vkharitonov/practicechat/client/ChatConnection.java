package com.dataart.vkharitonov.practicechat.client;

import com.dataart.vkharitonov.practicechat.common.json.*;
import com.dataart.vkharitonov.practicechat.common.util.JsonUtils;
import com.dataart.vkharitonov.practicechat.common.util.MessageProducer;
import org.apache.commons.net.io.Util;

import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.logging.Logger;

/**
 * Manages client's connection to the server
 */
public class ChatConnection {

    private final static Logger log = Logger.getLogger(ChatConnection.class.getName());

    private Socket socket;
    private PrintWriter writer;
    private ServerMessageListener listener;

    public ChatConnection(String username, String host, int port, ServerMessageListener listener) throws IOException {
        this.listener = listener;

        socket = new Socket(host, port);
        writer = new PrintWriter(new OutputStreamWriter(socket.getOutputStream(), StandardCharsets.UTF_8), true);
        MessageProducer producer = new MessageProducer();
        producer.start(socket.getInputStream(), new MessageConsumer());
        writeConnectMessage(username);
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
            log.info("Couldn't send disconnect message");
        } finally {
            close();
        }
    }

    /**
     * Sends `list_users` message to the server
     * @throws IOException
     */
    public void listUsers() throws IOException {
        sendMessage(Message.MessageType.LIST_USERS, null);
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

    private void writeConnectMessage(String username) throws IOException {
        sendMessage(Message.MessageType.CONNECT, new ConnectInMessage(username));
    }

    private <T> void sendMessage(Message.MessageType type, T payload) throws IOException {
        Message message = new Message(type, payload);
        writer.println(JsonUtils.GSON.toJson(message));
        if (writer.checkError()) {
            throw new IOException("Couldn't write to socket");
        }
    }

    private void handleUserList(Message message) {
        UserListOutMessage payload = JsonUtils.GSON.fromJson(message.getPayload(), UserListOutMessage.class);
        listener.onUserList(payload.getUsers());
    }

    private void handleNewMessage(Message message) {
        NewMsgOutMessage payload = JsonUtils.GSON.fromJson(message.getPayload(), NewMsgOutMessage.class);
        listener.onNewMessage(payload.getUsername(), payload.getMessage(), payload.isOnline(), payload.getTimestamp());
    }

    private void handleMsgSent(Message message) {
        MsgSentOutMessage payload = JsonUtils.GSON.fromJson(message.getPayload(), MsgSentOutMessage.class);
        listener.onMessageSent(payload.getUsername());
    }

    private void handleConnectionResult(Message message) {
        ConnectionResultOutMessage payload =
                JsonUtils.GSON.fromJson(message.getPayload(), ConnectionResultOutMessage.class);
        listener.onConnectionResult(payload.isSuccess());
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
