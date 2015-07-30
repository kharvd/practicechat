package com.dataart.vkharitonov.practicechat.net;

import com.dataart.vkharitonov.practicechat.json.ConnectMessage;
import com.dataart.vkharitonov.practicechat.json.Message;
import com.dataart.vkharitonov.practicechat.message.ConnectionRequest;
import com.dataart.vkharitonov.practicechat.util.JsonUtils;
import com.google.gson.JsonSyntaxException;
import com.google.gson.stream.JsonReader;
import com.sun.istack.internal.NotNull;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Accepts connection requests from clients and redirects them to the listener.
 */
public class ConnectionManager {

    private final static Logger log = Logger.getLogger(ConnectionManager.class.getName());
    private final static int MAX_CONNECTION_POOL = 10;
    private final static int CONNECT_MESSAGE_TIMEOUT = 1000;
    private ServerSocket server;
    private volatile boolean isRunning;
    private MessageListener connectionListener;
    private ExecutorService executor;

    /**
     * Starts listening to incoming client connections.
     *
     * @param port               the port number
     * @param connectionListener handles each connection request
     * @throws IOException thrown if couldn't create server socket
     */
    public void start(int port, @NotNull MessageListener connectionListener) throws IOException {
        executor = Executors.newFixedThreadPool(MAX_CONNECTION_POOL);
        server = new ServerSocket(port);

        this.connectionListener = connectionListener;

        isRunning = true;
        WorkerThread workerThread = new WorkerThread();
        workerThread.start();
    }

    /**
     * Stops listening to connections.
     */
    public void stop() {
        if (!isRunning) {
            throw new IllegalStateException("ConnectionManager is not running!");
        }

        isRunning = false;
        try {
            server.close();
        } catch (IOException e) {
            log.log(Level.WARNING, e, () -> "Couldn't close server socket");
        }
        executor.shutdown();
    }

    private class WorkerThread extends Thread {
        @Override
        public void run() {
            while (isRunning) {
                try {
                    Socket client = server.accept();
                    executor.submit(() -> handleConnection(client));
                } catch (SocketException e) {
                    log.info("Server socket was stopped");
                } catch (IOException e) {
                    log.log(Level.WARNING, e, () -> "IOException in WorkerThread");
                }
            }
        }

        private void handleConnection(Socket client) {
            try {
                client.setSoTimeout(CONNECT_MESSAGE_TIMEOUT);
                Message message = parseMessage(client);
                client.setSoTimeout(0);

                if (message.getMessageType() == Message.MessageType.CONNECT) {
                    ConnectMessage connectMessage = JsonUtils.GSON.fromJson(message.getPayload(), ConnectMessage.class);
                    connectionListener.onMessage(new ConnectionRequest(connectMessage, client));
                } else {
                    throw new JsonSyntaxException("First message should be `connect`");
                }
            } catch (IOException | JsonSyntaxException e) {
                log.log(Level.INFO, client.getInetAddress().toString() + " " + e.getMessage());
                try {
                    client.close();
                } catch (IOException e1) {
                    log.log(Level.INFO, client.getInetAddress().toString() + " " + e1.getMessage());
                }
            }
        }

        private Message parseMessage(Socket client) throws IOException {
            BufferedReader in = new BufferedReader(new InputStreamReader(client.getInputStream()));
            JsonReader reader = new JsonReader(in);
            return JsonUtils.GSON.fromJson(reader, Message.class);
        }
    }

}
