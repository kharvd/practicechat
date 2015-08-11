package com.dataart.vkharitonov.practicechat.common.json.in;

public class DropRoomInMessage {

    private String name;

    public DropRoomInMessage(String name) {
        this.name = name;
    }

    public String getRoomName() {
        return name;
    }
}
