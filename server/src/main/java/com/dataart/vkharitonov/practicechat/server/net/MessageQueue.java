package com.dataart.vkharitonov.practicechat.server.net;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.logging.Logger;

/**
 * Maintains a message queue which is processed on a separate thread
 */
public abstract class MessageQueue implements MessageListener {

    private final static Logger log = Logger.getLogger(MessageQueue.class.getName());

    private BlockingQueue<Object> messageQueue = new LinkedBlockingQueue<>();
    private WorkerThread workerThread;

    /**
     * Starts processing messages
     */
    public void start() {
        workerThread = new WorkerThread();
        workerThread.start();
    }

    /**
     * Stops processing messages
     */
    public void stop() {
        workerThread.interrupt();
    }

    /**
     * @return true, if the queue is accepting new messages
     */
    public boolean isRunning() {
        return workerThread.isAlive() && !workerThread.isInterrupted();
    }

    /**
     * Adds a message to a message queue. The message will be processed asynchronously.
     *
     * @param message a message
     */
    @Override
    public void post(Object message) {
        if (isRunning()) {
            messageQueue.add(message);
        } else {
            log.warning("Posting to stopped queue");
        }
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
            while (!isInterrupted()) {
                try {
                    Object message = messageQueue.take();
                    handleMessage(message);
                } catch (InterruptedException e) {
                    interrupt();
                } catch (Exception e) {
                    handleError(e);
                }
            }
        }
    }
}
