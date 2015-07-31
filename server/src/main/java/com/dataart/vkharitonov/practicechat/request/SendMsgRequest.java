package com.dataart.vkharitonov.practicechat.request;

import com.dataart.vkharitonov.practicechat.json.SendMsgInMessage;

public class SendMsgRequest extends Request {
    private SendMsgInMessage message;
    private long timestamp;

    public SendMsgRequest(String sender, SendMsgInMessage message) {
        super(sender);
        this.message = message;
        timestamp = System.currentTimeMillis();
    }

    public SendMsgInMessage getMessage() {
        return message;
    }

    public long getTimestamp() {
        return timestamp;
    }

    @Override
    public String toString() {
        return "SendMsgRequest{" +
                "message=" + message +
                '}';
    }
}
