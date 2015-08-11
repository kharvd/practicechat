package com.dataart.vkharitonov.practicechat.server.db.dto;

import com.dataart.vkharitonov.practicechat.common.json.ChatMsg;

import java.sql.Timestamp;

public class ChatMsgDto {

    private String sender;
    private String destination;
    private String message;
    private Timestamp sendingTime;
    private boolean delivered;

    public ChatMsgDto() {
    }

    public ChatMsgDto(String sender, String destination, String message, long timestamp, boolean delivered) {
        this.sender = sender;
        this.destination = destination;
        this.message = message;
        this.sendingTime = new Timestamp(timestamp);
        this.delivered = delivered;
    }

    public String getSender() {
        return sender;
    }

    public void setSender(String sender) {
        this.sender = sender;
    }

    public String getDestination() {
        return destination;
    }

    public void setDestination(String destination) {
        this.destination = destination;
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

    public boolean isDelivered() {
        return delivered;
    }

    public void setDelivered(boolean delivered) {
        this.delivered = delivered;
    }

    public ChatMsg toChatMsg() {
        return new ChatMsg(sender, destination, message, sendingTime.getTime());
    }

    @Override
    public String toString() {
        return "ChatMsgDto{" +
                "sender='" + sender + '\'' +
                ", destination='" + destination + '\'' +
                ", message='" + message + '\'' +
                ", sendingTime=" + sendingTime +
                ", delivered=" + delivered +
                '}';
    }
}
