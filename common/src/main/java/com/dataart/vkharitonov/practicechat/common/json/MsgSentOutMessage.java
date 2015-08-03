package com.dataart.vkharitonov.practicechat.common.json;

public class MsgSentOutMessage {
    private String username;

    public MsgSentOutMessage(String username) {
        this.username = username;
    }

    public String getUsername() {
        return username;
    }
}
