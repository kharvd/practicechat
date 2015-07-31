package com.dataart.vkharitonov.practicechat.request;

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
