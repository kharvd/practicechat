package com.dataart.vkharitonov.practicechat.server.event;

public class JoinRoomRequest extends Request {

    private String roomName;

    public JoinRoomRequest(String sender, String roomName) {
        super(sender);
        this.roomName = roomName;
    }

    public String getRoomName() {
        return roomName;
    }
}
