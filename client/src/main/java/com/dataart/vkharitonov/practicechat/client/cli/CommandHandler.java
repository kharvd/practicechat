package com.dataart.vkharitonov.practicechat.client.cli;

public interface CommandHandler {
    void onConnect(String username, String host, int port);

    void onList();

    void onSendMessage(String username, String message);

    void onHistory(String username);

    void onDisconnect();

    void onExit();

    void onHelp();

    /**
     * Called when failed to parse user's input
     *
     * @param e Thrown exception with a human-readable message
     */
    void onError(Throwable e);
}
