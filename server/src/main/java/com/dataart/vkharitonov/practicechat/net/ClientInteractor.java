package com.dataart.vkharitonov.practicechat.net;

import com.dataart.vkharitonov.practicechat.json.ConnectionResultMessage;
import com.dataart.vkharitonov.practicechat.json.Message;
import com.dataart.vkharitonov.practicechat.json.UserListMessage;
import com.dataart.vkharitonov.practicechat.message.DisconnectRequest;
import com.dataart.vkharitonov.practicechat.message.ListUsersRequest;
import com.dataart.vkharitonov.practicechat.util.JsonUtils;
import com.google.gson.JsonSyntaxException;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonToken;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.logging.Logger;

public final class ClientInteractor extends MessageListener {

    private final static Logger log = Logger.getLogger(ClientInteractor.class.getName());
    private final PrintWriter writer;
    private String username;
    private Socket clientSocket;
    private ExecutorService executor = Executors.newSingleThreadExecutor();
    private InteractorManager interactorManager;
    private volatile boolean isReadRunning;

    public ClientInteractor(String username, Socket clientSocket, InteractorManager interactorManager) throws IOException {
        super();
        this.username = username;
        this.clientSocket = clientSocket;
        this.interactorManager = interactorManager;

        writer = new PrintWriter(clientSocket.getOutputStream(), true);

        isReadRunning = true;
        new SocketReadThread().start();
    }

    @Override
    protected void handleMessage(Object message) {
        if (message instanceof ConnectionResultMessage) {
            replyConnectionResultMessage((ConnectionResultMessage) message);
        } else if (message instanceof ListUsersRequest) {
            handleListUsersRequest(message);
        } else if (message instanceof UserListMessage) {
            replyUserListMessage((UserListMessage) message);
        } else if (message instanceof DisconnectRequest) {
            handleDisconnectRequest(message);
        }
    }

    private void handleDisconnectRequest(Object message) {
        isReadRunning = false;
        closeConnection();
        stopMessageQueue();
        interactorManager.handleMessage(message);
    }

    private void replyUserListMessage(UserListMessage message) {
        Message resultMessage = new Message(Message.MessageType.USER_LIST, JsonUtils.GSON.toJsonTree(message));
        String json = JsonUtils.GSON.toJson(resultMessage);
        executor.submit(() -> writeToClient(json));
    }

    private void handleListUsersRequest(Object message) {
        interactorManager.sendMessage(message);
    }

    private void replyConnectionResultMessage(ConnectionResultMessage message) {
        Message resultMessage = new Message(Message.MessageType.CONNECTION_RESULT, JsonUtils.GSON.toJsonTree(message));
        String json = JsonUtils.GSON.toJson(resultMessage);
        executor.submit(() -> writeToClient(json));
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
                    default:
                        break;
                }
            }
        }

        private void disconnectRequest() {
            sendMessage(new DisconnectRequest(username));
        }
    }
}
