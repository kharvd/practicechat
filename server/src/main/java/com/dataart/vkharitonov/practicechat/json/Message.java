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

    @Override
    public String toString() {
        return "Message{" +
                "messageType=" + messageType +
                ", payload=" + payload +
                '}';
    }

    public enum MessageType {
        // Incoming messages

        @SerializedName("connect")
        CONNECT,

        @SerializedName("disconnect")
        DISCONNECT,

        @SerializedName("list_users")
        LIST_USERS,


        // Outgoing messages

        @SerializedName("connection_result")
        CONNECTION_RESULT,

        @SerializedName("user_list")
        USER_LIST
    }
}
