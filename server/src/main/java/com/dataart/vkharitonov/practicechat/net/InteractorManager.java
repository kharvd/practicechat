package com.dataart.vkharitonov.practicechat.net;

import com.dataart.vkharitonov.practicechat.json.ConnectionResult;
import com.dataart.vkharitonov.practicechat.json.Message;
import com.dataart.vkharitonov.practicechat.message.ConnectionRequest;
import com.dataart.vkharitonov.practicechat.util.JsonUtils;
import com.google.gson.JsonElement;

import java.io.IOException;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.logging.Level;
import java.util.logging.Logger;

public class InteractorManager implements MessageListener {

    private final static Logger log = Logger.getLogger(InteractorManager.class.getName());
    private static final int CONNECTION_FAILURE_TIMEOUT = 1000;
    private Map<String, ClientInteractor> clients = new HashMap<>();
    private BlockingQueue<Object> messageQueue = new LinkedBlockingQueue<>();
    private ExecutorService executor = Executors.newCachedThreadPool();
    private volatile boolean isRunning;

    public InteractorManager() {
        isRunning = true;
        new WorkerThread().start();
    }

    public void stop() {
        isRunning = false;
    }

    /**
     * Sends `connection failed` message and closes socket
     *
     * @param clientSocket client socket
     */
    private void sendConnectionFailure(Socket clientSocket) {
        JsonElement payload = JsonUtils.GSON.toJsonTree(new ConnectionResult(false));
        String message = JsonUtils.GSON.toJson(new Message(Message.MessageType.CONNECTION_RESULT, payload));
        try (PrintWriter out = new PrintWriter(clientSocket.getOutputStream(), true)) {
            clientSocket.setSoTimeout(CONNECTION_FAILURE_TIMEOUT);
            out.println(message);
        } catch (IOException e) {
            log.log(Level.WARNING, e.getMessage());
        }
    }

    @Override
    public void onMessage(Object message) {
        messageQueue.add(message);
    }

    private void handleMessage(Object message) {
        if (message instanceof ConnectionRequest) {
            ConnectionRequest request = (ConnectionRequest) message;
            String username = request.getConnectMessage().getUsername();
            Socket client = request.getClient();

            if (username == null) {
                log.info("Username can't be null");
                executor.submit(() -> sendConnectionFailure(client));
            } else if (clients.containsKey(username)) {
                log.info("User " + username + " is already connected");
                executor.submit(() -> sendConnectionFailure(client));
            } else {
                log.info("User " + username + " has been connected");
                clients.put(username, new ClientInteractor(client));
            }
        }
    }

    private class WorkerThread extends Thread {

        @Override
        public void run() {
            while (isRunning) {
                try {
                    Object message = messageQueue.take();
                    handleMessage(message);
                } catch (InterruptedException e) {
                    yield();
                }
            }
        }
    }
}
