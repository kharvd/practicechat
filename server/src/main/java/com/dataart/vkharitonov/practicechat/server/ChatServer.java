package com.dataart.vkharitonov.practicechat.server;

import com.dataart.vkharitonov.practicechat.server.db.DbHelper;
import com.dataart.vkharitonov.practicechat.server.net.ConnectionManager;
import com.dataart.vkharitonov.practicechat.server.net.InteractorManager;
import com.dataart.vkharitonov.practicechat.server.request.ShutdownCommand;

import java.io.IOException;
import java.util.logging.Logger;

import static com.google.common.base.Preconditions.checkNotNull;

public class ChatServer {

    private final static Logger log = Logger.getLogger(ChatServer.class.getName());

    private final int port;
    private final String dbServerName;
    private final String dbName;
    private final String dbUsername;
    private final String dbPassword;

    private ConnectionManager connectionManager;
    private InteractorManager interactorManager;

    private ChatServer(int port, String dbServerName, String dbName, String dbUsername, String dbPassword) {
        this.port = port;
        this.dbServerName = dbServerName;
        this.dbName = dbName;
        this.dbUsername = dbUsername;
        this.dbPassword = dbPassword;
    }

    public void start() throws IOException {
        DbHelper.init(dbName, dbServerName, dbUsername, dbPassword);

        connectionManager = new ConnectionManager();
        interactorManager = new InteractorManager();

        connectionManager.start(port, interactorManager);
        log.info("Started server on port " + port);
    }

    public void stop() {
        connectionManager.stop();
        interactorManager.post(new ShutdownCommand());
        DbHelper.close();
    }

    public static class Builder {

        private int port;
        private String dbServerName;
        private String dbName;
        private String dbUsername;
        private String dbPassword;

        public Builder port(int port) {
            this.port = port;
            return this;
        }

        public Builder dbServerName(String dbServerName) {
            this.dbServerName = dbServerName;
            checkNotNull(dbServerName, "DB serverName must not be null");
            return this;
        }

        public Builder dbName(String dbName) {
            this.dbName = dbName;
            checkNotNull(dbName, "DB name must not be null");
            return this;
        }

        public Builder dbUsername(String dbUsername) {
            this.dbUsername = dbUsername;
            checkNotNull(dbUsername, "DB username must not be null");
            return this;
        }

        public Builder dbPassword(String dbPassword) {
            this.dbPassword = dbPassword;
            checkNotNull(dbPassword, "DB password must not be null");
            return this;
        }

        public ChatServer create() {
            return new ChatServer(port, dbServerName, dbName, dbUsername, dbPassword);
        }
    }
}
