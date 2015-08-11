package com.dataart.vkharitonov.practicechat.common.json.in;

public class ConnectInMessage {

    private String username;
    private String password;

    public ConnectInMessage(String username, String password) {
        this.username = username;
        this.password = password;
    }

    public String getUsername() {
        return username;
    }

    public String getPassword() {
        return password;
    }

    @Override
    public String toString() {
        return "ConnectInMessage{" +
                "username='" + username + '\'' +
                ", password='" + password + '\'' +
                '}';
    }
}
