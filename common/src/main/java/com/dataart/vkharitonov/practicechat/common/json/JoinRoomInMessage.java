package com.dataart.vkharitonov.practicechat.common.json;

public class JoinRoomInMessage {

    private String name;

    public JoinRoomInMessage(String name) {
        this.name = name;
    }

    public String getRoomName() {
        return name;
    }
}
