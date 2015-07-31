package com.dataart.vkharitonov.practicechat.json;

public class NewMsgOutMessage {

    private String username;
    private String message;
    private boolean online;
    private long timestamp;

    public NewMsgOutMessage(String username, String message, boolean online, long timestamp) {
        this.username = username;
        this.message = message;
        this.online = online;
        this.timestamp = timestamp;
    }

    public String getUsername() {
        return username;
    }
}
