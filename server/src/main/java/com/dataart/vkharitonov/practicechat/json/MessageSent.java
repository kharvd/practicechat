package com.dataart.vkharitonov.practicechat.json;

public class MessageSent {
    private String username;
    private boolean online;

    public MessageSent(String username, boolean online) {
        this.username = username;
        this.online = online;
    }

    public String getUsername() {
        return username;
    }

    public boolean isOnline() {
        return online;
    }
}
