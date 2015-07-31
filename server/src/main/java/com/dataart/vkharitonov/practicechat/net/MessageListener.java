package com.dataart.vkharitonov.practicechat.net;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Maintains a message queue which is processed on a separate thread
 */
public abstract class MessageListener {

    private final static Logger log = Logger.getLogger(MessageListener.class.getName());

    private BlockingQueue<Object> messageQueue = new LinkedBlockingQueue<>();
    private volatile boolean isRunning;
    private WorkerThread workerThread;

    public void startMessageQueue() {
        isRunning = true;
        workerThread = new WorkerThread();
        workerThread.start();
    }

    public void stopMessageQueue() {
        isRunning = false;
        workerThread.interrupt();
    }

    public boolean isMessageQueueRunning() {
        return isRunning;
    }

    /**
     * Adds a message to a message queue. The message will be processed asynchronously.
     *
     * @param message a message
     */
    public void sendMessage(Object message) {
        messageQueue.add(message);
    }

    /**
     * Handle next message from a queue
     * @param message a message
     */
    protected abstract void handleMessage(Object message);

    private class WorkerThread extends Thread {

        @Override
        public void run() {
            while (isRunning) {
                try {
                    Object message = messageQueue.take();
                    handleMessage(message);
                } catch (InterruptedException e) {
                    log.warning("WorkerThread in " + MessageListener.this + " was interrupted. Shutting down the queue");
                    isRunning = false;
                } catch (Exception e) {
                    log.log(Level.SEVERE, e, () -> "Exception during message handling in " + MessageListener.this + ". Shutting down the server");
                    System.exit(1);
                }
            }
        }
    }
}
