package com.dataart.vkharitonov.practicechat.server.db;

public class RoomDto {
    private String name;
    private String admin;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getAdmin() {
        return admin;
    }

    public void setAdmin(String admin) {
        this.admin = admin;
    }
}
