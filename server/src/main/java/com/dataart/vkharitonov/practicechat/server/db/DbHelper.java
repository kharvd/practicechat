package com.dataart.vkharitonov.practicechat.server.db;

import org.flywaydb.core.Flyway;
import org.postgresql.ds.PGPoolingDataSource;

import static com.google.common.base.Preconditions.checkNotNull;

public class DbHelper {

    private static DbHelper instance;
    private PGPoolingDataSource dataSource;
    private UndeliveredMsgDao undeliveredMsgDao;

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

    public static void init(String dbName, String serverName, String username, String password) {
        if (instance == null) {
            synchronized (DbHelper.class) {
                if (instance == null) {
                    instance = new DbHelper(dbName, serverName, username, password);
                }
            }
        }
    }

    public static void close() {
        if (instance != null) {
            instance.dataSource.close();
        }
    }

    public static DbHelper getInstance() {
        synchronized (DbHelper.class) {
            checkNotNull(instance, "You must call init() first");
            return instance;
        }
    }

    public UndeliveredMsgDao getMsgDao() {
        if (undeliveredMsgDao == null) {
            synchronized (DbHelper.class) {
                if (undeliveredMsgDao == null) {
                    undeliveredMsgDao = new UndeliveredMsgDao(dataSource);
                }
            }
        }

        return undeliveredMsgDao;
    }
}
