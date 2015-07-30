package com.dataart.vkharitonov.practicechat.net;

import java.net.Socket;

public class ClientInteractor {
    private Socket clientSocket;

    public ClientInteractor(Socket clientSocket) {
        this.clientSocket = clientSocket;
    }
}
