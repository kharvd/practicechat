package com.dataart.vkharitonov.practicechat.message;

import com.dataart.vkharitonov.practicechat.json.ConnectMessage;

import java.net.Socket;

public class ConnectionRequest {

    private ConnectMessage connectMessage;
    private Socket client;

    public ConnectionRequest(ConnectMessage connectMessage, Socket client) {
        this.connectMessage = connectMessage;
        this.client = client;
    }

    public ConnectMessage getConnectMessage() {
        return connectMessage;
    }

    public Socket getClient() {
        return client;
    }
}
