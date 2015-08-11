package com.dataart.vkharitonov.practicechat.common.json.out;

public class RoomJoinedOutMessage {

    private String roomName;
    private boolean roomExists;

    public RoomJoinedOutMessage(String roomName, boolean roomExists) {
        this.roomName = roomName;
        this.roomExists = roomExists;
    }

    public boolean isRoomExists() {
        return roomExists;
    }

    public String getRoomName() {
        return roomName;
    }
}
