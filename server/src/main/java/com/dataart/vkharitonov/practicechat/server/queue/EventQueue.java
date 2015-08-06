package com.dataart.vkharitonov.practicechat.server.queue;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.Executor;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Maintains an event queue which is processed on a separate worker thread. All events supplied to {@link #post(Object)} method
 * are added to the queue and delivered to the handler in order.
 * <p>
 * The handler must annotate its event handler methods with {@link Subscribe} annotation.
 * The handler method must only have one parameter whose type is the type of the event that must be handled by the method.
 * If there are several annotated methods with the same argument type in the same class,
 * the order of method invocation is undefined
 * <p>
 * If the {@link #post(Object)} method is supplied with {@link Runnable} instance, it would not send this event
 * to the handler. Instead, this runnable would be run on the queue thread as soon as it's dequeued
 */
public class EventQueue implements EventListener {

    private final static Logger log = LoggerFactory.getLogger(EventQueue.class.getName());
    private static final AtomicInteger threadCounter = new AtomicInteger(0);
    private BlockingQueue<Object> eventQueue = new LinkedBlockingQueue<>();
    private WorkerThread workerThread;
    private Object eventHandler;
    private Executor executor; // Event queue executor

    /**
     * Starts processing messages
     *
     * @param handler Event handler with {@link Subscribe}-annotated methods.
     */
    public void start(Object handler) {
        eventHandler = handler;
        executor = this::post;

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
     * Adds an event to the event queue. The event will be processed asynchronously on the event queue thread.
     * <p>
     * <b>Warning:</b> If the event is an instance of {@link Runnable}, it is not sent to the handler. Instead, it is run directly on the queue thread.
     *
     * @param event an event
     */
    @Override
    public void post(Object event) {
        checkNotNull(event, "Event must not be null");

        if (isRunning()) {
            eventQueue.add(event);
        } else {
            log.warn("Posting to stopped queue");
        }
    }

    /**
     * Returns an {@link Executor} that enqueues {@link Runnable}s to be run on the event queue thread
     *
     * @return an executor
     */
    public Executor executor() {
        return executor;
    }

    private void handleEvent(Object message) {
        // Runnables are run directly
        if (message instanceof Runnable) {
            ((Runnable) message).run();
            return;
        }

        Class<?> messageType = message.getClass();
        if (eventHandler == null) {
            log.warn("Event of type {} was not handled: handler is null", messageType);
            return;
        }

        Class<?> handlerClass = eventHandler.getClass();
        Method[] methods = handlerClass.getDeclaredMethods();

        List<Method> methodsToCall = Stream.of(methods)
                                           .filter(m -> m.isAnnotationPresent(Subscribe.class) &&
                                                   m.getParameterCount() == 1 &&
                                                   m.getParameterTypes()[0].equals(messageType))
                                           .collect(Collectors.toList());

        if (methodsToCall.isEmpty()) {
            log.warn("Event of type {} was not handled by object of class {}", messageType, handlerClass);
        } else {
            if (methodsToCall.size() > 1) {
                log.warn("Multiple handlers for event type {} in class {}", messageType, handlerClass);
            }

            methodsToCall.forEach(method -> sendMessageToHandler(method, message));
        }
    }

    private void sendMessageToHandler(Method method, Object message) {
        try {
            method.setAccessible(true);
            method.invoke(eventHandler, message);
        } catch (IllegalAccessException e) {
            log.error("Couldn't invoke event handler", e);
        } catch (InvocationTargetException e) {
            log.error("Exception in event handler", e.getCause());
        }
    }

    private class WorkerThread extends Thread {

        public WorkerThread() {
            setName("EventQueue-" + threadCounter.incrementAndGet());
        }

        @Override
        public void run() {
            while (!isInterrupted()) {
                try {
                    Object event = eventQueue.take();
                    handleEvent(event);
                } catch (InterruptedException e) {
                    interrupt();
                }
            }
        }
    }
}
