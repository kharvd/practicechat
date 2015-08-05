package com.dataart.vkharitonov.practicechat.server.event;

import com.dataart.vkharitonov.practicechat.common.json.SendMsgInMessage;

public class SendMsgRequest extends Request {
    private SendMsgInMessage message;
    private long timestamp;

    public SendMsgRequest(String sender, SendMsgInMessage message) {
        super(sender);
        this.message = message;
        timestamp = System.currentTimeMillis();
    }

    public SendMsgRequest(String sender, long timestamp, SendMsgInMessage message) {
        super(sender);
        this.message = message;
        this.timestamp = timestamp;
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
                ", timestamp=" + timestamp +
                '}';
    }
}
