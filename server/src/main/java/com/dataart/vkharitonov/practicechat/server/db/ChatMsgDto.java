package com.dataart.vkharitonov.practicechat.server.db;

import com.dataart.vkharitonov.practicechat.common.json.ChatMsg;
import com.dataart.vkharitonov.practicechat.common.json.SendMsgInMessage;
import com.dataart.vkharitonov.practicechat.server.event.SendMsgRequest;

import java.sql.Timestamp;

public class ChatMsgDto {
    private String sender;
    private String destination;
    private String message;
    private Timestamp sendingTime;
    private boolean delivered;

    public ChatMsgDto() {
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

    public SendMsgRequest toSendMsgRequest() {
        return new SendMsgRequest(sender, sendingTime.getTime(), new SendMsgInMessage(destination, message));
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
