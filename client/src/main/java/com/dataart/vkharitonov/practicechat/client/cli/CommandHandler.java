package com.dataart.vkharitonov.practicechat.client.cli;

public interface CommandHandler {
    void onConnect(String username, String host, int port);

    void onList();

    void onSend(String username, String message);

    void onDisconnect();

    void onError(Throwable e);
}
