package com.dataart.vkharitonov.practicechat.server.queue;

public interface EventListener {
    void post(Object event);
}
