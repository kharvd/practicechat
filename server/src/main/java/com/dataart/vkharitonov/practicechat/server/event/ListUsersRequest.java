package com.dataart.vkharitonov.practicechat.server.event;

public class ListUsersRequest extends Request {

    public ListUsersRequest(String username) {
        super(username);
    }
}
