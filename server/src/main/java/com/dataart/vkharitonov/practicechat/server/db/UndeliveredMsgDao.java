package com.dataart.vkharitonov.practicechat.server.db;

import com.dataart.vkharitonov.practicechat.server.request.SendMsgRequest;
import org.apache.commons.dbutils.QueryRunner;
import org.apache.commons.dbutils.ResultSetHandler;
import org.apache.commons.dbutils.handlers.BeanListHandler;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

public class UndeliveredMsgDao {

    private final static Logger log = Logger.getLogger(UndeliveredMsgDao.class.getName());

    private DataSource dataSource;
    private QueryRunner run;
    private ExecutorService dbExecutor;
    private ResultSetHandler<List<UndeliveredMsg>> undeliveredMessageHandler;

    protected UndeliveredMsgDao(DataSource dataSource) {
        this.dataSource = dataSource;
        dbExecutor = Executors.newSingleThreadExecutor();
        undeliveredMessageHandler = new BeanListHandler<>(UndeliveredMsg.class);
        run = new QueryRunner();
    }

    protected void close() {
        dbExecutor.shutdown();
    }

    public CompletableFuture<List<SendMsgRequest>> getUndeliveredMsgsForUser(String username) {
        return CompletableFuture.supplyAsync(() -> getUndeliveredMsgsForUserSync(username), dbExecutor).exceptionally(e -> {
            log.log(Level.WARNING, "Error fetching messages from db", e);
            return null;
        });
    }

    public CompletableFuture<Void> addUndeliveredMsg(SendMsgRequest request) {
        return CompletableFuture.runAsync(() -> addUndeliveredMsgSync(request), dbExecutor).exceptionally(e -> {
            log.log(Level.WARNING, "Error inserting message to db", e);
            return null;
        });
    }

    public CompletableFuture<Void> removeOldestMessage(String username) {
        return CompletableFuture.runAsync(() -> removeOldestMessageSync(username), dbExecutor).exceptionally(e -> {
            log.log(Level.WARNING, "Error removing message from db", e);
            return null;
        });
    }

    private List<SendMsgRequest> getUndeliveredMsgsForUserSync(String username) {
        try (Connection conn = dataSource.getConnection()) {
            String query = "SELECT sender, destination, message, sending_time AS sendingTime \n" +
                    "FROM undelivered_messages \n" +
                    "WHERE destination = ? \n" +
                    "ORDER BY sendingTime;";

            return run.query(conn, query, undeliveredMessageHandler, username).stream()
                    .map(UndeliveredMsg::toSendMsgRequest)
                    .collect(Collectors.toList());
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    private void addUndeliveredMsgSync(SendMsgRequest request) {
        try (Connection conn = dataSource.getConnection()) {
            String insert = "INSERT INTO undelivered_messages(sender, destination, message, sending_time) \n" +
                    "VALUES (?, ?, ?, to_timestamp(?));";
            run.insert(conn, insert, undeliveredMessageHandler,
                    request.getSender(),
                    request.getMessage().getUsername(),
                    request.getMessage().getMessage(),
                    request.getTimestamp() / 1000.0);
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    private void removeOldestMessageSync(String username) {
        try (Connection conn = dataSource.getConnection()) {
            String sql = "DELETE FROM undelivered_messages \n" +
                    "WHERE id = (SELECT id \n" +
                    "            FROM undelivered_messages \n" +
                    "            WHERE destination = ?\n" +
                    "            ORDER BY sending_time \n" +
                    "            LIMIT 1\n" +
                    "            );";
            run.update(conn, sql, username);
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }
}
