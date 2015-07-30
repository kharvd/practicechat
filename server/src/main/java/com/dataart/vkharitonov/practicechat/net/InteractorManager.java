package com.dataart.vkharitonov.practicechat.net;

import com.dataart.vkharitonov.practicechat.json.ConnectionResultMessage;
import com.dataart.vkharitonov.practicechat.json.Message;
import com.dataart.vkharitonov.practicechat.json.UserListMessage;
import com.dataart.vkharitonov.practicechat.message.ConnectionRequest;
import com.dataart.vkharitonov.practicechat.message.DisconnectRequest;
import com.dataart.vkharitonov.practicechat.message.ListUsersRequest;
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

public final class InteractorManager extends MessageListener {

    private final static Logger log = Logger.getLogger(InteractorManager.class.getName());
    private static final int CONNECTION_FAILURE_TIMEOUT = 1000;

    private Map<String, ClientInteractor> clients = new HashMap<>();
    private ExecutorService executor = Executors.newCachedThreadPool();

    @Override
    protected void handleMessage(Object message) {
        if (message instanceof ConnectionRequest) {
            handleConnectionRequest((ConnectionRequest) message);
        } else if (message instanceof ListUsersRequest) {
            handleListUsersRequest((ListUsersRequest) message);
        } else if (message instanceof DisconnectRequest) {
            handleDisconnectRequest((DisconnectRequest) message);
        }
    }

    private void handleDisconnectRequest(DisconnectRequest message) {
        clients.remove(message.getUsername());
    }

    private void handleListUsersRequest(ListUsersRequest message) {
        ClientInteractor clientInteractor = clients.get(message.getUsername());
        if (clientInteractor != null) {
            clientInteractor.sendMessage(new UserListMessage(clients.keySet()));
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
            clientInteractor.startMessageQueue();
            clientInteractor.sendMessage(new ConnectionResultMessage(true));
            clients.put(username, clientInteractor);
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
        JsonElement payload = JsonUtils.GSON.toJsonTree(new ConnectionResultMessage(false));
        String message = JsonUtils.GSON.toJson(new Message(Message.MessageType.CONNECTION_RESULT, payload));
        try (PrintWriter out = new PrintWriter(clientSocket.getOutputStream(), true)) {
            clientSocket.setSoTimeout(CONNECTION_FAILURE_TIMEOUT);
            out.println(message);
        } catch (IOException e) {
            log.log(Level.WARNING, e.getMessage());
        }
    }
}
