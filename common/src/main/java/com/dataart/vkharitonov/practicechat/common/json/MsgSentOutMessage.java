package com.dataart.vkharitonov.practicechat.common.json;

public class MsgSentOutMessage {
    private String username;
    private boolean online;

    public MsgSentOutMessage(String username, boolean online) {
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
