package com.dataart.vkharitonov.practicechat.common.json;

import com.dataart.vkharitonov.practicechat.common.util.JsonUtils;
import com.google.gson.JsonElement;
import com.google.gson.annotations.SerializedName;

public class Message {

    private MessageType messageType;
    private JsonElement payload;

    public <T> Message(MessageType messageType, T payload) {
        this.messageType = messageType;
        this.payload = JsonUtils.GSON.toJsonTree(payload);
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

        @SerializedName("send_message")
        SEND_MESSAGE,

        // Outgoing messages

        @SerializedName("connection_result")
        CONNECTION_RESULT,

        @SerializedName("user_list")
        USER_LIST,

        @SerializedName("new_message")
        NEW_MESSAGE,

        @SerializedName("message_sent")
        MESSAGE_SENT
    }
}
