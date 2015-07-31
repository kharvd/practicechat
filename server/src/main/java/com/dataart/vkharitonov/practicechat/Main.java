package com.dataart.vkharitonov.practicechat;

import com.dataart.vkharitonov.practicechat.net.ConnectionManager;
import com.dataart.vkharitonov.practicechat.net.InteractorManager;

import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;

public class Main {

    private final static Logger log = Logger.getLogger(Main.class.getName());

    public static void main(String[] args) {
        ConnectionManager connectionManager = new ConnectionManager();
        InteractorManager interactorManager = new InteractorManager();

        try {
            connectionManager.start(1234, interactorManager);
        } catch (IOException e) {
            log.log(Level.SEVERE, e, () -> "Couldn't start the server");
            interactorManager.stopMessageQueue();
        }
    }

}
