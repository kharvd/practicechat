package com.dataart.vkharitonov.practicechat.common.json;

public class NewMsgOutMessage {

    private String room;
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

    public NewMsgOutMessage(String room, String username, String message, long timestamp) {
        this.room = room;
        this.username = username;
        this.message = message;
        this.timestamp = timestamp;
    }

    public String getUsername() {
        return username;
    }

    public String getMessage() {
        return message;
    }

    public boolean isOnline() {
        return online;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public String getRoom() {
        return room;
    }
}
