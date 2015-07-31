package com.dataart.vkharitonov.practicechat.message;

import com.dataart.vkharitonov.practicechat.json.SendMessage;

public class SendMessageRequest extends Request {
    private SendMessage message;

    public SendMessageRequest(String sender, SendMessage message) {
        super(sender);
        this.message = message;
    }

    public SendMessage getMessage() {
        return message;
    }

    @Override
    public String toString() {
        return "SendMessageRequest{" +
                "message=" + message +
                '}';
    }
}
