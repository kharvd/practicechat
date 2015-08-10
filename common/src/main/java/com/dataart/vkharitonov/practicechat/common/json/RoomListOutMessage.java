package com.dataart.vkharitonov.practicechat.common.json;

import java.util.List;

public class RoomListOutMessage {
    private List<String> rooms;

    public RoomListOutMessage(List<String> rooms) {
        this.rooms = rooms;
    }

    public List<String> getRooms() {
        return rooms;
    }
}
