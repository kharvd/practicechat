package com.dataart.vkharitonov.practicechat.request;

import com.dataart.vkharitonov.practicechat.json.SendMsgInMessage;

public class SendMsgRequest extends Request {
    private SendMsgInMessage message;

    public SendMsgRequest(String sender, SendMsgInMessage message) {
        super(sender);
        this.message = message;
    }

    public SendMsgInMessage getMessage() {
        return message;
    }

    @Override
    public String toString() {
        return "SendMsgRequest{" +
                "message=" + message +
                '}';
    }
}
