package com.dataart.vkharitonov.practicechat.server.event;

import java.util.Optional;

public class ListUsersRequest extends Request {

    private Optional<String> roomName;

    public ListUsersRequest(String sender, String roomName) {
        super(sender);
        this.roomName = Optional.ofNullable(roomName);
    }

    public ListUsersRequest(String sender) {
        super(sender);
        this.roomName = Optional.empty();
    }

    public Optional<String> getRoomName() {
        return roomName;
    }
}
