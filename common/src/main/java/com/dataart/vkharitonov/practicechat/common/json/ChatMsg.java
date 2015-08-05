package com.dataart.vkharitonov.practicechat.common.json;

public class ChatMsg {
    private String sender;
    private String destination;
    private String message;
    private long timestamp;

    public ChatMsg(String sender, String destination, String message, long timestamp) {
        this.sender = sender;
        this.destination = destination;
        this.message = message;
        this.timestamp = timestamp;
    }

    public String getSender() {
        return sender;
    }

    public String getDestination() {
        return destination;
    }

    public String getMessage() {
        return message;
    }

    public long getTimestamp() {
        return timestamp;
    }
}
