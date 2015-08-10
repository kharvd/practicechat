package com.dataart.vkharitonov.practicechat.server.db;

import org.apache.commons.dbutils.QueryRunner;
import org.apache.commons.dbutils.ResultSetHandler;
import org.apache.commons.dbutils.handlers.BeanListHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;
import java.util.concurrent.CompletableFuture;

class Dao<T> {

    private final static Logger log = LoggerFactory.getLogger(Dao.class.getName());

    private DataSource dataSource;
    private QueryRunner queryRunner;

    private ResultSetHandler<List<T>> resultSetHandler;

    Dao(DataSource dataSource, Class<T> cls) {
        queryRunner = new QueryRunner();
        resultSetHandler = new BeanListHandler<>(cls);
        this.dataSource = dataSource;
    }

    void close() {
        DbHelper.getDbExecutor().shutdown();
    }

    private DataSource getDataSource() {
        return dataSource;
    }

    QueryRunner getQueryRunner() {
        return queryRunner;
    }

    ResultSetHandler<List<T>> getDefaultResultSetHandler() {
        return resultSetHandler;
    }

    <U> CompletableFuture<U> supplyAsync(Supplier<U> supplier) {
        return CompletableFuture.supplyAsync(() -> {
            try (Connection conn = getDataSource().getConnection()) {
                return supplier.get(conn);
            } catch (SQLException e) {
                throw new RuntimeException(e);
            }
        }, DbHelper.getDbExecutor()).exceptionally(e -> {
            log.error("Error during DB query: {}", e.getLocalizedMessage());
            throw new RuntimeException(e);
        });
    }

    interface Supplier<U> {
        U get(Connection connection) throws SQLException;
    }
}
