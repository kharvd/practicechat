package com.dataart.vkharitonov.practicechat.common.json.in;

public class LeaveRoomInMessage {

    private String name;

    public LeaveRoomInMessage(String name) {
        this.name = name;
    }

    public String getRoomName() {
        return name;
    }
}
