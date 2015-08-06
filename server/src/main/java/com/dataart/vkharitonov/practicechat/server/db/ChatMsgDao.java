package com.dataart.vkharitonov.practicechat.server.db;

import com.dataart.vkharitonov.practicechat.common.json.ChatMsg;
import com.dataart.vkharitonov.practicechat.common.json.MsgHistoryOutMessage;
import com.dataart.vkharitonov.practicechat.server.event.SendMsgRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.sql.DataSource;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

public class ChatMsgDao extends Dao<ChatMsgDto> {

    private final static Logger log = LoggerFactory.getLogger(ChatMsgDao.class.getName());

    protected ChatMsgDao(DataSource dataSource) {
        super(dataSource, ChatMsgDto.class);
    }

    public CompletableFuture<List<SendMsgRequest>> getUndeliveredMsgsForUser(String username) {
        return supplyAsync(connection -> {
            String query = "SELECT sender, destination, message, delivered, sending_time AS sendingTime\n" +
                    "FROM messages\n" +
                    "WHERE destination = ? AND NOT delivered\n" +
                    "ORDER BY sendingTime;";

            return getQueryRunner().query(connection, query, getDefaultResultSetHandler(), username)
                                   .stream()
                                   .map(ChatMsgDto::toSendMsgRequest)
                                   .collect(Collectors.toList());
        });
    }

    public CompletableFuture<Void> addUndeliveredMsg(SendMsgRequest request) {
        return supplyAsync(connection -> {
            String insert = "INSERT INTO messages(sender, destination, message, sending_time, delivered) \n" +
                    "VALUES (?, ?, ?, to_timestamp(?), FALSE);";
            getQueryRunner().insert(connection, insert, getDefaultResultSetHandler(),
                    request.getSender(),
                    request.getMessage().getUsername(),
                    request.getMessage().getMessage(),
                    request.getTimestamp() / 1000.0);

            return null;
        });
    }

    public CompletableFuture<Void> setOldestMessageDelivered(String username) {
        return supplyAsync(connection -> {
            String sql = "UPDATE messages SET delivered = TRUE \n" +
                    "WHERE id = (SELECT id FROM messages \n" +
                    "            WHERE destination = ? AND NOT delivered\n" +
                    "            ORDER BY sending_time \n" +
                    "            LIMIT 1);";
            getQueryRunner().update(connection, sql, username);

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
