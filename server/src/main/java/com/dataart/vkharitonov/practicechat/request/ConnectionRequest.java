package com.dataart.vkharitonov.practicechat.request;

import com.dataart.vkharitonov.practicechat.json.ConnectInMessage;

import java.net.Socket;

/**
 * A request to register a new user
 */
public class ConnectionRequest {

    private ConnectInMessage connectMessage;
    private Socket client;

    public ConnectionRequest(ConnectInMessage connectMessage, Socket client) {
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
