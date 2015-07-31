package com.dataart.vkharitonov.practicechat.client;

import com.dataart.vkharitonov.practicechat.client.cli.CommandHandler;
import com.dataart.vkharitonov.practicechat.client.cli.CommandReader;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.logging.Logger;

public class Main {
    private final static Logger log = Logger.getLogger(Main.class.getName());

    public static void main(String[] args) {
        BufferedReader cin = new BufferedReader(new InputStreamReader(System.in));

        new CommandReader(cin, new MainCommandHandler());
    }

    private static class MainCommandHandler implements CommandHandler {
        @Override
        public void onConnect(String username, String host, int port) {
            System.out.format("Connecting to %s:%d as %s\n", host, port, username);
        }

        @Override
        public void onList() {
            System.out.println("list users");
        }

        @Override
        public void onSend(String username, String message) {
            System.out.format("Sending message \"%s\" to %s\n", message, username);
        }

        @Override
        public void onDisconnect() {
            System.out.println("disconnected");
        }

        @Override
        public void onError(Throwable e) {
            System.out.println(e.getLocalizedMessage());
        }
    }


}
