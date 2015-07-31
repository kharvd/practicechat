package com.dataart.vkharitonov.practicechat.json;

public class SendMessage {

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
        return "SendMessage{" +
                "username='" + username + '\'' +
                ", message='" + message + '\'' +
                '}';
    }
}
