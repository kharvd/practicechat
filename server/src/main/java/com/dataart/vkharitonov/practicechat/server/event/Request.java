package com.dataart.vkharitonov.practicechat.server.event;

/**
 * Request made by a specific user
 */
public class Request {
    private String sender;

    public Request(String sender) {
        this.sender = sender;
    }

    public String getSender() {
        return sender;
    }
}