package com.dataart.vkharitonov.practicechat.common.json;

public class ListUsersInMessage {

    private String roomName;

    public ListUsersInMessage(String roomName) {
        this.roomName = roomName;
    }

    public String getRoomName() {
        return roomName;
    }
}
