package com.dataart.vkharitonov.practicechat.common.json;

public class SendMsgInMessage {

    private String username;
    private String message;

    public SendMsgInMessage(String username, String message) {
        this.username = username;
        this.message = message;
    }

    public String getUsername() {
        return username;
    }

    public String getMessage() {
        return message;
    }

    @Override
    public String toString() {
        return "SendMsgInMessage{" +
                "username='" + username + '\'' +
                ", message='" + message + '\'' +
                '}';
    }
}
