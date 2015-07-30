package com.dataart.vkharitonov.practicechat.json;

import com.google.gson.JsonElement;
import com.google.gson.annotations.SerializedName;

public class Message {

    private MessageType messageType;
    private JsonElement payload;

    public Message(MessageType messageType, JsonElement payload) {
        this.messageType = messageType;
        this.payload = payload;
    }

    public MessageType getMessageType() {
        return messageType;
    }

    public JsonElement getPayload() {
        return payload;
    }

    public enum MessageType {
        @SerializedName("connect")
        CONNECT,

        @SerializedName("connection_result")
        CONNECTION_RESULT
    }
}
