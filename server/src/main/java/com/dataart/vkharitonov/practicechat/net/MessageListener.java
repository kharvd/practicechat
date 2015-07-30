package com.dataart.vkharitonov.practicechat.net;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

/**
 * Maintains a message queue which is processed on a separate thread
 */
public abstract class MessageListener {

    private BlockingQueue<Object> messageQueue = new LinkedBlockingQueue<>();
    private volatile boolean isRunning;

    public void startMessageQueue() {
        isRunning = true;
        new WorkerThread().start();
    }

    public void stopMessageQueue() {
        isRunning = false;
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
                    yield();
                }
            }
        }
    }
}
