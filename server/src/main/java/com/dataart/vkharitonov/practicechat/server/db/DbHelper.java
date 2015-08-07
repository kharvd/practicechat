package com.dataart.vkharitonov.practicechat.server.db;

import org.flywaydb.core.Flyway;
import org.postgresql.ds.PGPoolingDataSource;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static com.google.common.base.Preconditions.checkNotNull;

public class DbHelper {

    private static final int MAX_THREADS = 10;
    private static ExecutorService dbExecutor;
    private static DbHelper instance;
    private PGPoolingDataSource dataSource;

    private ChatMsgDao chatMsgDao;
    private UserDao userDao;
    private RoomDao roomDao;

    private DbHelper(String dbName, String serverName, String username, String password) {
        dataSource = new PGPoolingDataSource();
        dataSource.setDatabaseName(dbName);
        dataSource.setServerName(serverName);
        dataSource.setUser(username);
        dataSource.setPassword(password);

        Flyway flyway = new Flyway();
        flyway.setDataSource(dataSource);
        flyway.migrate();
    }

    public static synchronized void init(String dbName, String serverName, String username, String password) {
        if (instance == null) {
            instance = new DbHelper(dbName, serverName, username, password);
            dbExecutor = Executors.newFixedThreadPool(MAX_THREADS);
        }
    }

    public static void close() {
        if (instance != null) {
            instance.dataSource.close();

            if (instance.chatMsgDao != null) {
                instance.chatMsgDao.close();
                instance.chatMsgDao = null;
            }

            if (instance.userDao != null) {
                instance.userDao.close();
                instance.userDao = null;
            }

            if (instance.roomDao != null) {
                instance.roomDao.close();
                instance.roomDao = null;
            }

            if (dbExecutor != null) {
                dbExecutor.shutdown();
            }
        }
    }

    public static DbHelper getInstance() {
        synchronized (DbHelper.class) {
            checkNotNull(instance, "You must call init() first");
            return instance;
        }
    }

    static ExecutorService getDbExecutor() {
        return dbExecutor;
    }

    public synchronized ChatMsgDao getMsgDao() {
        if (chatMsgDao == null) {
            chatMsgDao = new ChatMsgDao(dataSource);
        }

        return chatMsgDao;
    }

    public synchronized UserDao getUserDao() {
        if (userDao == null) {
            userDao = new UserDao(dataSource);
        }

        return userDao;
    }

    public synchronized RoomDao getRoomDao() {
        if (roomDao == null) {
            roomDao = new RoomDao(dataSource);
        }

        return roomDao;
    }
}
