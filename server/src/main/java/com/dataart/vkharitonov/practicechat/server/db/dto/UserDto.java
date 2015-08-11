package com.dataart.vkharitonov.practicechat.server.db.dto;

public class UserDto {

    private String name;
    private String hash;
    private String salt;

    public UserDto() {
    }

    public UserDto(String name, String hash, String salt) {
        this.name = name;
        this.hash = hash;
        this.salt = salt;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getHash() {
        return hash;
    }

    public void setHash(String hash) {
        this.hash = hash;
    }

    public String getSalt() {
        return salt;
    }

    public void setSalt(String salt) {
        this.salt = salt;
    }
}
