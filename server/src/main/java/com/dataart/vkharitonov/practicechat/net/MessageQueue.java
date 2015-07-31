package com.dataart.vkharitonov.practicechat.net;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

/**
 * Maintains a message queue which is processed on a separate thread
 */
public abstract class MessageQueue implements MessageListener {

    private BlockingQueue<Object> messageQueue = new LinkedBlockingQueue<>();
    private volatile boolean isRunning;
    private WorkerThread workerThread;

    /**
     * Starts processing messages
     */
    public void start() {
        isRunning = true;
        workerThread = new WorkerThread();
        workerThread.start();
    }

    /**
     * Stops processing messages
     */
    public void stop() {
        isRunning = false;
        workerThread.interrupt();
    }

    /**
     * @return true, if the queue is accepting new messages
     */
    public boolean isRunning() {
        return isRunning;
    }

    /**
     * Adds a message to a message queue. The message will be processed asynchronously.
     *
     * @param message a message
     */
    @Override
    public void post(Object message) {
        if (!isRunning()) {
            throw new IllegalStateException("Queue is not running!");
        }

        messageQueue.add(message);
    }

    /**
     * Handle next message from a queue
     * @param message a message
     */
    protected abstract void handleMessage(Object message);

    /**
     * Called when an exception was thrown during message handling.
     *
     * @param e thrown exception
     */
    protected abstract void handleError(Throwable e);

    private class WorkerThread extends Thread {

        @Override
        public void run() {
            while (isRunning) {
                try {
                    Object message = messageQueue.take();
                    handleMessage(message);
                } catch (InterruptedException e) {
                    isRunning = false;
                } catch (Exception e) {
                    handleError(e);
                }
            }
        }
    }
}
