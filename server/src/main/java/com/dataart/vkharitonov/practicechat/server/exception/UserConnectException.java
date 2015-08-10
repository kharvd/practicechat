package com.dataart.vkharitonov.practicechat.server.exception;

public class UserConnectException extends RuntimeException {
    private boolean userExists;

    public UserConnectException(boolean userExists, String message) {
        super(message);
        this.userExists = userExists;
    }

    public boolean isUserExists() {
        return userExists;
    }
}
