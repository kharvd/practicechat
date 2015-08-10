package com.dataart.vkharitonov.practicechat.client;

import com.dataart.vkharitonov.practicechat.client.cli.CommandHandler;
import com.dataart.vkharitonov.practicechat.client.cli.CommandReader;
import com.dataart.vkharitonov.practicechat.common.json.ChatMsg;
import com.dataart.vkharitonov.practicechat.common.json.UserListOutMessage;

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
                "    connect <user>@<host>:<port> <password> - connects <user> to <host>:<port>\n" +
                "    list [#<room>] - lists users currently connected to the server, or, \n" +
                "        if room is specified, members of the room\n" +
                "    rooms - lists all available rooms\n" +
                "    send <username> \"<message>\" - sends <message> to <username>\n" +
                "    history <username> - lists message history with user <username>\n" +
                "    join #<room> - joins the room\n" +
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
        public void onConnect(String username, String password, String host, int port) {
            if (connection != null && connection.isConnected()) {
                System.out.println("Already connected");
            } else {
                System.out.format("Connecting to %s:%d as %s%n", host, port, username);
                try {
                    connection = new ChatConnection(username, password, host, port, new MainServerMessageListener());
                } catch (IOException e) {
                    System.out.println("Couldn't connect to host");
                }
            }
        }

        @Override
        public void onList(String roomName) {
            if (checkConnection()) {
                try {
                    connection.listUsers(roomName);
                } catch (IOException e) {
                    System.out.println("Couldn't send message to the server. Disconnecting");
                    connection.disconnect();
                }
            }
        }

        @Override
        public void onRoomsList() {
            if (checkConnection()) {
                try {
                    connection.listRooms();
                } catch (IOException e) {
                    System.out.println("Couldn't send message. Disconnecting");
                    connection.disconnect();
                }
            }
        }

        @Override
        public void onSendMessage(String username, String message) {
            if (checkConnection()) {
                try {
                    connection.sendMessage(username, message);
                } catch (IOException e) {
                    System.out.println("Couldn't send message. Disconnecting");
                    connection.disconnect();
                }
            }
        }

        @Override
        public void onHistory(String username) {
            if (checkConnection()) {
                try {
                    connection.getHistory(username);
                } catch (IOException e) {
                    System.out.println("Couldn't send message to the server. Disconnecting");
                    connection.disconnect();
                }
            }
        }

        @Override
        public void onJoin(String roomName) {
            if (checkConnection()) {
                try {
                    connection.joinRoom(roomName);
                } catch (IOException e) {
                    System.out.println("Couldn't send message to the server. Disconnecting");
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
        public void onConnectionResult(boolean success, boolean userExists) {
            if (success && userExists) {
                System.out.println("Successfully connected to the server");
            } else if (success) {
                System.out.println("Successfully registered a new user");
            } else if (userExists) {
                System.out.println("Invalid password");
            } else {
                System.out.println("Invalid username");
            }

            if (!success && connection != null) {
                connection.disconnect();
            }
        }

        @Override
        public void onMessageSent(String user) {
            System.out.format("Message to %s has been sent%n", user);
        }

        @Override
        public void onUserList(List<UserListOutMessage.User> users) {
            System.out.format("Online users: %s%n", users);
        }

        @Override
        public void onRoomList(List<String> rooms) {
            System.out.format("%s%n", rooms);
        }

        @Override
        public void onNewMessage(String sender, String message, boolean userOnline, long timestamp) {
            System.out.format("[%tT] %s: %s%n", timestamp, sender, message);
        }

        @Override
        public void onMessageHistory(List<ChatMsg> messages) {
            if (messages.isEmpty()) {
                System.out.println("History is empty");
            } else {
                for (ChatMsg message : messages) {
                    onNewMessage(message.getSender(), message.getMessage(), false, message.getTimestamp());
                }
            }
        }

        @Override
        public void onRoomJoined(String roomName, boolean roomExists) {
            if (roomExists) {
                System.out.format("Joined room %s%n", roomName);
            } else {
                System.out.format("Created room %s%n", roomName);
            }
        }

        @Override
        public void onDisconnect() {
            System.out.println("Server has disconnected");
        }
    }
}
