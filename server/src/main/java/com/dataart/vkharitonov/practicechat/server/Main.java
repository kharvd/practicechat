package com.dataart.vkharitonov.practicechat.server;

import com.dataart.vkharitonov.practicechat.server.db.DbHelper;
import com.dataart.vkharitonov.practicechat.server.net.ConnectionManager;
import com.dataart.vkharitonov.practicechat.server.net.InteractorManager;
import com.dataart.vkharitonov.practicechat.server.request.ShutdownRequest;

import java.io.FileInputStream;
import java.io.IOException;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;

import static com.google.common.base.Preconditions.checkNotNull;

public class Main {

    private final static Logger log = Logger.getLogger(Main.class.getName());

    public static void main(String[] args) {
        if (args.length < 1) {
            showUsageAndExit();
        }

        int port;

        try (FileInputStream propertiesFile = new FileInputStream(args[0])) {
            Properties props = new Properties();
            props.load(propertiesFile);
            port = Integer.parseInt(props.getProperty("server.port"));
            initDbHelper(props);
        } catch (IOException | NumberFormatException e) {
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

        DbHelper.close();
    }

    private static void initDbHelper(Properties config) {
        String dbName = config.getProperty("db.name");
        String serverName = config.getProperty("db.serverName");
        String username = config.getProperty("db.username");
        String password = config.getProperty("db.password");

        checkNotNull(dbName, "db.name must not be null");
        checkNotNull(serverName, "db.serverName must not be null");
        checkNotNull(username, "db.username must not be null");
        checkNotNull(password, "db.password must not be null");

        DbHelper.init(dbName, serverName, username, password);
    }

    private static void showUsageAndExit() {
        System.out.println("usage: server.jar <properties_file>");
        System.exit(1);
    }

}
