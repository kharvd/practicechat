package com.dataart.vkharitonov.practicechat.json;

public class SendMsgInMessage {

    private String username;
    private String message;

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
