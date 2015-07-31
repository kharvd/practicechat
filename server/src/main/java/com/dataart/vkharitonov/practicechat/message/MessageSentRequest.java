package com.dataart.vkharitonov.practicechat.message;

public class MessageSentRequest extends Request {

    private String messageSender;

    public MessageSentRequest(String sender, String messageSender) {
        super(sender);
        this.messageSender = messageSender;
    }

    public String getMessageSender() {
        return messageSender;
    }
}
