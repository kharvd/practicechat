package com.dataart.vkharitonov.practicechat.common.json.in;

public class JoinRoomInMessage {

    private String name;

    public JoinRoomInMessage(String name) {
        this.name = name;
    }

    public String getRoomName() {
        return name;
    }
}
