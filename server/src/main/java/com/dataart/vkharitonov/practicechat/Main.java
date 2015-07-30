package com.dataart.vkharitonov.practicechat;

import com.dataart.vkharitonov.practicechat.net.ConnectionManager;
import com.dataart.vkharitonov.practicechat.net.InteractorManager;

import java.io.IOException;
import java.util.logging.Logger;

public class Main {
    private final static Logger log = Logger.getLogger(ConnectionManager.class.getName());

    public static void main(String[] args) throws IOException {
        ConnectionManager connectionManager = new ConnectionManager();
        InteractorManager interactorManager = new InteractorManager();

        interactorManager.startMessageQueue();
        connectionManager.start(1234, interactorManager);
    }

}
