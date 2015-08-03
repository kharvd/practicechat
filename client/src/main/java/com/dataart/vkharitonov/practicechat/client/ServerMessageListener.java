package com.dataart.vkharitonov.practicechat.client;

import java.util.List;

public interface ServerMessageListener {
    void onConnectionResult(boolean success);

    void onMessageSent(String user, boolean online);

    void onUserList(List<String> users);

    void onNewMessage(String sender, String message, boolean userOnline, long timestamp);

    void onDisconnect();
}
