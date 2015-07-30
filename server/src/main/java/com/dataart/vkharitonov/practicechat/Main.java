package com.dataart.vkharitonov.practicechat;

import com.dataart.vkharitonov.practicechat.net.ConnectionManager;
import com.dataart.vkharitonov.practicechat.net.InteractorManager;

import java.io.IOException;

public class Main {

    public static void main(String[] args) throws IOException {
        ConnectionManager connectionManager = new ConnectionManager();
        InteractorManager interactorManager = new InteractorManager();

        connectionManager.start(1234, interactorManager);
    }

}
