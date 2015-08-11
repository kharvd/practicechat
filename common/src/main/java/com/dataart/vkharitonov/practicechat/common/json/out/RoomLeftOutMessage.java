package com.dataart.vkharitonov.practicechat.common.json.out;

public class RoomLeftOutMessage {

    private boolean success;
    private String roomName;

    public RoomLeftOutMessage(String roomName, boolean success) {
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
