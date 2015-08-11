package com.dataart.vkharitonov.practicechat.server.db.dto;

import com.dataart.vkharitonov.practicechat.common.json.ChatMsg;

import java.sql.Timestamp;

public class RoomMsgDto {
    private String sender;
    private String room;
    private String message;
    private Timestamp sendingTime;

    public RoomMsgDto() {
    }

    public RoomMsgDto(String sender, String room, String message, long timestamp) {
        this.sender = sender;
        this.room = room;
        this.message = message;
        this.sendingTime = new Timestamp(timestamp);
    }

    public String getSender() {
        return sender;
    }

    public void setSender(String sender) {
        this.sender = sender;
    }

    public String getRoom() {
        return room;
    }

    public void setRoom(String room) {
        this.room = room;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public Timestamp getSendingTime() {
        return new Timestamp(sendingTime.getTime());
    }

    public void setSendingTime(Timestamp sendingTime) {
        this.sendingTime = new Timestamp(sendingTime.getTime());
    }

    public ChatMsg toChatMsg() {
        return new ChatMsg(sender, room, message, sendingTime.getTime());
    }

    @Override
    public String toString() {
        return "RoomMsgDto{" +
                "sender='" + sender + '\'' +
                ", room='" + room + '\'' +
                ", message='" + message + '\'' +
                ", sendingTime=" + sendingTime +
                '}';
    }
}
