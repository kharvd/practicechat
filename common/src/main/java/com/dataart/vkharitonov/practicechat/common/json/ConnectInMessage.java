package com.dataart.vkharitonov.practicechat.common.json;

public class ConnectInMessage {

    private String username;

    public ConnectInMessage(String username) {
        this.username = username;
    }

    public String getUsername() {
        return username;
    }

    @Override
    public String toString() {
        return "ConnectInMessage{" +
                "username='" + username + '\'' +
                '}';
    }
}
