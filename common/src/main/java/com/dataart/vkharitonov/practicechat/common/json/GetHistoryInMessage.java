package com.dataart.vkharitonov.practicechat.common.json;

public class GetHistoryInMessage {
    private String username;

    public GetHistoryInMessage(String username) {
        this.username = username;
    }

    public String getUsername() {
        return username;
    }
}
