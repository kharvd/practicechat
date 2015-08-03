package com.dataart.vkharitonov.practicechat.server.db;

import com.dataart.vkharitonov.practicechat.common.json.SendMsgInMessage;
import com.dataart.vkharitonov.practicechat.server.request.SendMsgRequest;

import java.sql.Timestamp;

public class UndeliveredMsg {
    private String sender;
    private String destination;
    private String message;
    private Timestamp sendingTime;

    public UndeliveredMsg() {
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

    public SendMsgRequest toSendMsgRequest() {
        return new SendMsgRequest(sender, sendingTime.getTime(), new SendMsgInMessage(destination, message));
    }

    @Override
    public String toString() {
        return "UndeliveredMsg{" +
                "sender='" + sender + '\'' +
                ", destination='" + destination + '\'' +
                ", message='" + message + '\'' +
                ", sendingTime=" + sendingTime +
                '}';
    }
}
