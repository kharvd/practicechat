package com.dataart.vkharitonov.practicechat.server.net;

import com.dataart.vkharitonov.practicechat.common.json.ConnectInMessage;
import com.dataart.vkharitonov.practicechat.common.json.Message;
import com.dataart.vkharitonov.practicechat.common.util.JsonUtils;
import com.dataart.vkharitonov.practicechat.server.queue.EventListener;
import com.dataart.vkharitonov.practicechat.server.request.ConnectionEvent;
import com.google.gson.JsonSyntaxException;
import com.google.gson.stream.JsonReader;
import org.apache.commons.net.io.Util;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Accepts connection requests from clients and redirects them to the message listener.
 */
public final class ConnectionManager {

    private final static Logger log = Logger.getLogger(ConnectionManager.class.getName());
    private final static int MAX_CONNECTION_POOL = 10;
    private final static int CONNECT_MESSAGE_TIMEOUT = 1000;
    private ServerSocket server;
    private EventListener connectionListener;
    private ExecutorService executor;
    private WorkerThread workerThread;

    /**
     * Starts listening to incoming client connections.
     *
     * @param port               the port number
     * @param connectionListener must handle {@link ConnectionEvent} messages
     * @throws IOException thrown if couldn't create server socket
     */
    public void start(int port, EventListener connectionListener) throws IOException {
        server = new ServerSocket(port);

        this.connectionListener = connectionListener;

        executor = Executors.newFixedThreadPool(MAX_CONNECTION_POOL);
        workerThread = new WorkerThread();
        workerThread.start();
    }

    /**
     * Stops listening to connections.
     */
    public void stop() {
        workerThread.interrupt();
        Util.closeQuietly(server);
        executor.shutdown();
    }

    private class WorkerThread extends Thread {
        @Override
        public void run() {
            while (!isInterrupted()) {
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
                    ConnectInMessage connectMessage = JsonUtils.GSON.fromJson(message.getPayload(), ConnectInMessage.class);
                    connectionListener.post(new ConnectionEvent(connectMessage, client));
                } else {
                    throw new JsonSyntaxException("First message should be `connect`");
                }
            } catch (IOException | JsonSyntaxException e) {
                log.log(Level.INFO, client.getInetAddress().toString() + " " + e.getMessage());
                Util.closeQuietly(client);
            }
        }

        private Message parseMessage(Socket client) throws IOException {
            BufferedReader in = new BufferedReader(new InputStreamReader(client.getInputStream(), StandardCharsets.UTF_8));
            JsonReader reader = new JsonReader(in);
            return JsonUtils.GSON.fromJson(reader, Message.class);
        }
    }

}
