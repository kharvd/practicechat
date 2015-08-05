package com.dataart.vkharitonov.practicechat.server.queue;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * <p>Maintains an event queue which is processed on a separate thread.</p>
 *
 * <p>The event handler must annotate its handler methods with {@link Subscribe} annotation.
 * The handler method must have only one parameter whose type is the type of the event that must be handled by the method.
 * If there are several annotated methods with the same argument type in the same class,
 * the order of method invocation is undefined</p>
 */
public class EventQueue implements EventListener {

    private final static Logger log = LoggerFactory.getLogger(EventQueue.class.getName());

    private BlockingQueue<Object> eventQueue = new LinkedBlockingQueue<>();
    private WorkerThread workerThread;
    private Object eventHandler;

    /**
     * Starts processing messages
     * @param handler Event handler with {@link Subscribe}-annotated methods.
     */
    public void start(Object handler) {
        eventHandler = handler;

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
     * Adds an event to the event queue. The event will be processed asynchronously.
     *
     * @param event an event
     */
    @Override
    public void post(Object event) {
        if (isRunning()) {
            eventQueue.add(event);
        } else {
            log.warn("Posting to stopped queue");
        }
    }

    private void handleEvent(Object message) {
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
            return;
        }

        if (methodsToCall.size() > 1) {
            log.warn("Multiple handlers for event type {} in class {}", messageType, handlerClass);
        }

        methodsToCall.forEach(method -> {
            try {
                method.setAccessible(true);
                method.invoke(eventHandler, message);
            } catch (IllegalAccessException e) {
                log.error("Couldn't invoke event handler", e);
            } catch (InvocationTargetException e) {
                log.error("Exception in event handler", e.getCause());
            }
        });
    }

    private class WorkerThread extends Thread {

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
