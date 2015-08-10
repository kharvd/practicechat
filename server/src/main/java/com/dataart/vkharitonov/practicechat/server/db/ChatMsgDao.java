package com.dataart.vkharitonov.practicechat.server.db;

import com.dataart.vkharitonov.practicechat.common.json.ChatMsg;
import com.dataart.vkharitonov.practicechat.common.json.MsgHistoryOutMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.sql.DataSource;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

public class ChatMsgDao extends Dao<ChatMsgDto> {

    private final static Logger log = LoggerFactory.getLogger(ChatMsgDao.class.getName());

    ChatMsgDao(DataSource dataSource) {
        super(dataSource, ChatMsgDto.class);
    }

    public CompletableFuture<List<ChatMsgDto>> getUndeliveredMsgsForUser(String username) {
        return supplyAsync(connection -> {
            String query = "SELECT sender, destination, message, delivered, sending_time AS sendingTime\n" +
                    "FROM messages\n" +
                    "WHERE destination = ? AND NOT delivered\n" +
                    "ORDER BY sendingTime;";

            return getQueryRunner().query(connection, query, getDefaultResultSetHandler(), username)
                                   .stream()
                                   .collect(Collectors.toList());
        });
    }

    public CompletableFuture<Void> addMsg(ChatMsgDto chatMsg) {
        return supplyAsync(connection -> {
            String insert = "INSERT INTO messages(sender, destination, message, sending_time, delivered) \n" +
                    "VALUES (?, ?, ?, to_timestamp(?), FALSE);";
            getQueryRunner().insert(connection, insert, getDefaultResultSetHandler(),
                    chatMsg.getSender(),
                    chatMsg.getDestination(),
                    chatMsg.getMessage(),
                    chatMsg.getSendingTime().getTime() / 1000.0);

            return null;
        });
    }

    public CompletableFuture<Void> setOldestMessageDelivered(String username) {
        return supplyAsync(connection -> {
            log.info("setting delivered flag");

            String sql = "UPDATE messages SET delivered = TRUE \n" +
                    "WHERE id = (SELECT id FROM messages \n" +
                    "            WHERE destination = ? AND NOT delivered\n" +
                    "            ORDER BY sending_time \n" +
                    "            LIMIT 1);";
            int rowsAffected = getQueryRunner().update(connection, sql, username);

            log.info("delivered flag set {}", rowsAffected);

            return null;
        });
    }

    public CompletableFuture<MsgHistoryOutMessage> getHistoryForUsers(String username1, String username2) {
        return supplyAsync(connection -> {
            String sql = "SELECT sender, destination, message, delivered, sending_time AS sendingTime FROM messages \n" +
                    "WHERE (sender = ? AND destination = ?) OR \n" +
                    "    (sender = ? AND destination = ?)\n" +
                    "ORDER BY sendingTime;";
            List<ChatMsg> messages = getQueryRunner()
                    .query(connection, sql, getDefaultResultSetHandler(), username1, username2, username2, username1)
                    .stream()
                    .map(ChatMsgDto::toChatMsg)
                    .collect(Collectors.toList());

            return new MsgHistoryOutMessage(messages);
        });
    }
}
