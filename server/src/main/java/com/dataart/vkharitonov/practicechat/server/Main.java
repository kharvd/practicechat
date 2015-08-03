package com.dataart.vkharitonov.practicechat.server;

import com.dataart.vkharitonov.practicechat.server.net.ConnectionManager;
import com.dataart.vkharitonov.practicechat.server.net.InteractorManager;
import com.dataart.vkharitonov.practicechat.server.request.ShutdownRequest;

import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;

public class Main {

    private final static Logger log = Logger.getLogger(Main.class.getName());

    public static void main(String[] args) {
        if (args.length < 1) {
            showUsageAndExit();
        }

        int port;

        try {
            port = Integer.parseInt(args[0]);
        } catch (NumberFormatException e) {
            showUsageAndExit();
            return;
        }

        ConnectionManager connectionManager = new ConnectionManager();
        InteractorManager interactorManager = new InteractorManager();

        try {
            connectionManager.start(port, interactorManager);
            log.info("Started server on port " + port);
        } catch (IOException e) {
            log.log(Level.SEVERE, e, () -> "Couldn't start the server");
            interactorManager.post(new ShutdownRequest());
        }
    }

    private static void showUsageAndExit() {
        System.out.println("usage: server.jar <port>");
        System.exit(1);
    }

}
