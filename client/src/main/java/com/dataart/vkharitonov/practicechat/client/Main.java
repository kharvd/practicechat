package com.dataart.vkharitonov.practicechat.client;

import com.dataart.vkharitonov.practicechat.client.cli.CommandHandler;
import com.dataart.vkharitonov.practicechat.client.cli.CommandReader;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.List;

public class Main {

    private static ChatConnection connection;

    public static void main(String[] args) {
        showHelp();

        BufferedReader cin = new BufferedReader(new InputStreamReader(System.in, StandardCharsets.UTF_8));
        new CommandReader(cin, new MainCommandHandler());
    }

    private static void showHelp() {
        System.out.println("Available commands: \n" +
                "    connect <user>@<host>:<port> - connects <user> to <host>:<port>\n" +
                "    list - lists users currently connected to the server\n" +
                "    send \"<username>\" \"<message>\" - sends <message> to <username>\n" +
                "    disconnect - disconnects from the server\n" +
                "    help - show this help message\n" +
                "    exit - exit the application");
    }

    private static boolean checkConnection() {
        if (connection == null || !connection.isConnected()) {
            System.out.println("Not connected to the server");
            return false;
        }

        return true;
    }

    /**
     * Listens to messages from the user
     */
    private static class MainCommandHandler implements CommandHandler {

        @Override
        public void onConnect(String username, String host, int port) {
            if (connection != null && connection.isConnected()) {
                System.out.println("Already connected");
            } else {
                System.out.format("Connecting to %s:%d as %s%n", host, port, username);
                try {
                    connection = new ChatConnection(username, host, port, new MainServerMessageListener());
                } catch (IOException e) {
                    System.out.println("Couldn't connect to host");
                }
            }
        }

        @Override
        public void onList() {
            if (checkConnection()) {
                try {
                    connection.listUsers();
                } catch (IOException e) {
                    System.out.println("Couldn't send message to the server. Disconnecting");
                    connection.disconnect();
                }
            }
        }

        @Override
        public void onSendMessage(String username, String message) {
            if (checkConnection()) {
                System.out.format("Sending message \"%s\" to %s%n", message, username);
                try {
                    connection.sendMessage(username, message);
                } catch (IOException e) {
                    System.out.println("Couldn't send message. Disconnecting");
                    connection.disconnect();
                }
            }
        }

        @Override
        public void onDisconnect() {
            if (connection != null) {
                connection.disconnect();
                System.out.println("disconnected");
            }
        }

        @Override
        public void onExit() {
            onDisconnect();
        }

        @Override
        public void onHelp() {
            showHelp();
        }

        @Override
        public void onError(Throwable e) {
            System.out.println(e.getLocalizedMessage());
        }
    }

    /**
     * Listens to messages from the server
     */
    private static class MainServerMessageListener implements ServerMessageListener {
        @Override
        public void onConnectionResult(boolean success) {
            if (success) {
                System.out.println("Successfully connected to the server");
            } else {
                System.out.println("Username is already taken");
                if (connection != null) {
                    connection.disconnect();
                }
            }
        }

        @Override
        public void onMessageSent(String user, boolean online) {
            System.out.format("Message to %s has been sent%n", user);
        }

        @Override
        public void onUserList(List<String> users) {
            System.out.format("Online users: %s%n", users);
        }

        @Override
        public void onNewMessage(String sender, String message, boolean userOnline, long timestamp) {
            System.out.format("[%tT] %s: %s%n", timestamp, sender, message);
        }

        @Override
        public void onDisconnect() {
            System.out.println("Server has disconnected");
        }
    }


}
