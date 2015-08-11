package com.dataart.vkharitonov.practicechat.common.json.out;

public class ConnectionResultOutMessage {

    private boolean success;
    private boolean userExists;

    public ConnectionResultOutMessage(boolean success, boolean userExists) {
        this.success = success;
        this.userExists = userExists;
    }

    public boolean isSuccess() {
        return success;
    }

    public boolean isUserExists() {
        return userExists;
    }
}
