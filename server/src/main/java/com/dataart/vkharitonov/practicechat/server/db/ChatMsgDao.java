package com.dataart.vkharitonov.practicechat.server.db;

import com.dataart.vkharitonov.practicechat.common.json.ChatMsg;
import com.dataart.vkharitonov.practicechat.common.json.MsgHistoryOutMessage;
import com.dataart.vkharitonov.practicechat.server.event.SendMsgRequest;
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
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;

public class ChatMsgDao {

    private final static Logger log = LoggerFactory.getLogger(ChatMsgDao.class.getName());
    private static final int MAX_THREADS = 10;
    private DataSource dataSource;
    private QueryRunner run;
    private ExecutorService dbExecutor;
    private ResultSetHandler<List<ChatMsgDto>> messageHandler;

    protected ChatMsgDao(DataSource dataSource) {
        this.dataSource = dataSource;
        dbExecutor = Executors.newFixedThreadPool(MAX_THREADS);

        messageHandler = new BeanListHandler<>(ChatMsgDto.class);
        run = new QueryRunner();
    }

    protected void close() {
        dbExecutor.shutdown();
    }

    public CompletableFuture<List<SendMsgRequest>> getUndeliveredMsgsForUser(String username) {
        return CompletableFuture.supplyAsync(() -> getUndeliveredMsgsForUserSync(username), dbExecutor)
                                .exceptionally(e -> {
                                    log.error("Error fetching messages from db", e);
                                    return null;
                                });
    }

    public CompletableFuture<Void> addUndeliveredMsg(SendMsgRequest request) {
        return CompletableFuture.runAsync(() -> addUndeliveredMsgSync(request), dbExecutor)
                                .exceptionally(e -> {
                                    log.error("Error inserting message to db", e);
                                    return null;
                                });
    }

    public CompletableFuture<Void> setOldestMessageDelivered(String username) {
        return CompletableFuture.runAsync(() -> setOldestMessageDeliveredSync(username), dbExecutor)
                                .exceptionally(e -> {
                                    log.error("Error removing message from db", e);
                                    return null;
                                });
    }

    public CompletableFuture<MsgHistoryOutMessage> getHistoryForUsers(String username1, String username2) {
        return CompletableFuture.supplyAsync(() -> getHistoryForUsersSync(username1, username2), dbExecutor)
                                .exceptionally(e -> {
                                    log.error("Error fetching messages from db", e);
                                    return null;
                                });
    }

    private List<SendMsgRequest> getUndeliveredMsgsForUserSync(String username) {
        try (Connection conn = dataSource.getConnection()) {
            String query = "SELECT sender, destination, message, delivered, sending_time AS sendingTime\n" +
                    "FROM messages\n" +
                    "WHERE destination = ? AND NOT delivered\n" +
                    "ORDER BY sendingTime;";

            return run.query(conn, query, messageHandler, username)
                      .stream()
                      .map(ChatMsgDto::toSendMsgRequest)
                      .collect(Collectors.toList());
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    private void addUndeliveredMsgSync(SendMsgRequest request) {
        try (Connection conn = dataSource.getConnection()) {
            String insert = "INSERT INTO messages(sender, destination, message, sending_time, delivered) \n" +
                    "VALUES (?, ?, ?, to_timestamp(?), FALSE);";
            run.insert(conn, insert, messageHandler,
                    request.getSender(),
                    request.getMessage().getUsername(),
                    request.getMessage().getMessage(),
                    request.getTimestamp() / 1000.0);
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    private void setOldestMessageDeliveredSync(String username) {
        try (Connection conn = dataSource.getConnection()) {
            String sql = "UPDATE messages SET delivered = TRUE \n" +
                    "WHERE id = (SELECT id FROM messages \n" +
                    "            WHERE destination = ? AND NOT delivered\n" +
                    "            ORDER BY sending_time \n" +
                    "            LIMIT 1);";
            run.update(conn, sql, username);
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    private MsgHistoryOutMessage getHistoryForUsersSync(String username1, String username2) {
        try (Connection conn = dataSource.getConnection()) {
            String sql = "SELECT sender, destination, message, delivered, sending_time AS sendingTime FROM messages \n" +
                    "WHERE (sender = ? AND destination = ?) OR \n" +
                    "    (sender = ? AND destination = ?)\n" +
                    "ORDER BY sendingTime;";
            List<ChatMsg> messages = run.query(conn, sql, messageHandler, username1, username2, username2, username1)
                                        .stream()
                                        .map(ChatMsgDto::toChatMsg)
                                        .collect(Collectors.toList());

            return new MsgHistoryOutMessage(messages);
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }
}
