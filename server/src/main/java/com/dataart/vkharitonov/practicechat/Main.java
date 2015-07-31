package com.dataart.vkharitonov.practicechat;

import com.dataart.vkharitonov.practicechat.net.ConnectionManager;
import com.dataart.vkharitonov.practicechat.net.InteractorManager;
import com.dataart.vkharitonov.practicechat.request.ShutdownRequest;

import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;

public class Main {

    public static final int PORT = 1234;
    private final static Logger log = Logger.getLogger(Main.class.getName());

    public static void main(String[] args) {
        ConnectionManager connectionManager = new ConnectionManager();
        InteractorManager interactorManager = new InteractorManager();

        try {
            connectionManager.start(PORT, interactorManager);
            log.info("Started server on port " + PORT);
        } catch (IOException e) {
            log.log(Level.SEVERE, e, () -> "Couldn't start the server");
            interactorManager.post(new ShutdownRequest());
        }
    }

}
