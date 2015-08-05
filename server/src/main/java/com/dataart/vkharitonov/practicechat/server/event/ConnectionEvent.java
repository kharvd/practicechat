package com.dataart.vkharitonov.practicechat.server.event;

import com.dataart.vkharitonov.practicechat.common.json.ConnectInMessage;

import java.net.Socket;

/**
 * A request to register a new user
 */
public class ConnectionEvent {

    private ConnectInMessage connectMessage;
    private Socket client;

    public ConnectionEvent(ConnectInMessage connectMessage, Socket client) {
        this.connectMessage = connectMessage;
        this.client = client;
    }

    public ConnectInMessage getConnectMessage() {
        return connectMessage;
    }

    public Socket getClient() {
        return client;
    }
}
