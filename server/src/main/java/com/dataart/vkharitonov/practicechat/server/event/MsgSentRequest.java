package com.dataart.vkharitonov.practicechat.server.event;

public class MsgSentRequest extends Request {

    private String messageSender;

    public MsgSentRequest(String sender, String messageSender) {
        super(sender);
        this.messageSender = messageSender;
    }

    public String getMessageSender() {
        return messageSender;
    }
}
