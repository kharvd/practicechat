package com.dataart.vkharitonov.practicechat.message;

/**
 * Request made by a specific user
 */
public class Request {
    private String username;

    public Request(String username) {
        this.username = username;
    }

    public String getUsername() {
        return username;
    }
}
