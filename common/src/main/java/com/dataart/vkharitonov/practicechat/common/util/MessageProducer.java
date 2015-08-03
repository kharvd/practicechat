package com.dataart.vkharitonov.practicechat.common.util;

import com.dataart.vkharitonov.practicechat.common.json.Message;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonToken;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;

/**
 * Reads messages from an input stream asynchronously and notifies the consumer about them
 */
public final class MessageProducer {

    private volatile boolean isRunning;
    private ReadThread readThread;

    /**
     * Starts reading messages from {@code inputStream} and notifying the {@code consumer}.
     * The consumer takes ownership of the InputStream and closes it after the reading is done.
     * <em>All methods of the consumer are called on the same thread that reads the stream,
     * so you shouldn't do any blocking operations there.</em>
     *
     * @param inputStream an input stream to read from
     * @param consumer    a consumer
     */
    public void start(InputStream inputStream, Consumer consumer) {
        if (consumer == null) {
            throw new NullPointerException();
        }

        isRunning = true;
        readThread = new ReadThread(inputStream, consumer);
        readThread.start();
    }

    /**
     * Stops reading from the stream. Consumer will be notified by onCompleted method.
     * The InputStream which was supplied to {@link #start(InputStream, Consumer)} method will be closed
     */
    public void stop() {
        isRunning = false;
        readThread.interrupt();
    }

    public interface Consumer {
        /**
         * Called when a new message was received
         *
         * @param message received message
         */
        void onNext(Message message);

        /**
         * Called when an exception was thrown during the reading. After this point, no more calls
         * to {@link #onNext} or {@link #onCompleted} will be made
         *
         * @param e thrown exception
         */
        void onError(Throwable e);

        /**
         * Called when the end of the stream was reached. After this point, no more calls
         * to {@link #onNext} or {@link #onError} will be made
         */
        void onCompleted();
    }

    private class ReadThread extends Thread {
        private InputStream inputStream;
        private Consumer consumer;

        private ReadThread(InputStream inputStream, Consumer consumer) {
            this.inputStream = inputStream;
            this.consumer = consumer;
        }

        @Override
        public void run() {
            try (BufferedReader in = new BufferedReader(new InputStreamReader(inputStream, StandardCharsets.UTF_8));
                 JsonReader reader = new JsonReader(in)) {
                reader.setLenient(true);
                while (isRunning && reader.peek() != JsonToken.END_DOCUMENT) {
                    Message message = JsonUtils.GSON.fromJson(reader, Message.class);

                    if (message != null) {
                        consumer.onNext(message);
                    } else {
                        isRunning = false;
                    }
                }

                consumer.onCompleted();
            } catch (Exception e) {
                consumer.onError(e);
                isRunning = false;
            }
        }
    }
}
