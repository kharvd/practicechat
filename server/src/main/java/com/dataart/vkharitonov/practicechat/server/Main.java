package com.dataart.vkharitonov.practicechat.server;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Objects;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;

public class Main {

    private final static Logger log = Logger.getLogger(Main.class.getName());

    public static void main(String[] args) {
        if (args.length < 1) {
            showUsageAndExit();
        }

        ChatServer server = initServer(args[0]);
        if (server == null) {
            showUsageAndExit();
            return;
        }

        try {
            server.start();
        } catch (IOException e) {
            log.log(Level.SEVERE, e, () -> "Couldn't start the server");
            server.stop();
            return;
        }

        try (BufferedReader reader = new BufferedReader(new InputStreamReader(System.in))) {
            String line;
            do {
                line = reader.readLine();
            } while (line != null && !Objects.equals(line, "exit"));

            server.stop();
        } catch (IOException e) {
            log.warning("Couldn't read from System.in");
        }
    }

    private static void showUsageAndExit() {
        System.out.println("usage: server.jar <properties_file>\n" +
                "\n" +
                "Required properties:\n" +
                "    server.port = \n" +
                "    db.name = \n" +
                "    db.serverName = \n" +
                "    db.username = \n" +
                "    db.password = ");
        System.exit(1);
    }

    private static ChatServer initServer(String propertiesFileName) {
        try (FileInputStream propertiesFile = new FileInputStream(propertiesFileName)) {
            Properties props = new Properties();
            props.load(propertiesFile);

            return new ChatServer.Builder().port(Integer.parseInt(props.getProperty("server.port")))
                                           .dbServerName(props.getProperty("db.serverName"))
                                           .dbName(props.getProperty("db.name"))
                                           .dbUsername(props.getProperty("db.username"))
                                           .dbPassword(props.getProperty("db.password"))
                                           .create();
        } catch (IOException | NumberFormatException e) {
            return null;
        }
    }

}
