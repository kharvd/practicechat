package com.dataart.vkharitonov.practicechat.common.json.out;

public class RoomDroppedOutMessage {
    private boolean success;
    private String roomName;

    public RoomDroppedOutMessage(String roomName, boolean success) {
        this.roomName = roomName;
        this.success = success;
    }

    public boolean isSuccess() {
        return success;
    }

    public String getRoomName() {
        return roomName;
    }
}
