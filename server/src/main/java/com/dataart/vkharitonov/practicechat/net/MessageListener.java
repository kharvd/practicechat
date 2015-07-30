package com.dataart.vkharitonov.practicechat.net;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

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

    public void sendMessage(Object message) {
        messageQueue.add(message);
    }

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
