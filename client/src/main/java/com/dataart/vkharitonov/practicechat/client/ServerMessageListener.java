package com.dataart.vkharitonov.practicechat.client;

import com.dataart.vkharitonov.practicechat.common.json.ChatMsg;
import com.dataart.vkharitonov.practicechat.common.json.UserListOutMessage;

import java.util.List;

/**
 * Listens to server's messages to the client
 */
public interface ServerMessageListener {
    void onConnectionResult(boolean success, boolean userExists);

    void onMessageSent(String user);

    void onUserList(List<UserListOutMessage.User> users);

    void onRoomList(List<String> rooms);

    void onNewMessage(String sender, String message, boolean userOnline, long timestamp);

    void onMessageHistory(List<ChatMsg> messages);

    void onRoomJoined(String roomName, boolean roomExists);

    /**
     * Server disconnected. No more messages are expected after this point
     */
    void onDisconnect();
}
