package com.dataart.vkharitonov.practicechat.server.db;

import com.dataart.vkharitonov.practicechat.common.json.ChatMsg;
import com.dataart.vkharitonov.practicechat.common.json.out.MsgHistoryOutMessage;
import com.dataart.vkharitonov.practicechat.server.db.dto.RoomMsgDto;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.sql.DataSource;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

public class RoomMsgDao extends Dao<RoomMsgDto> {

    private final static Logger log = LoggerFactory.getLogger(RoomMsgDao.class.getName());

    public RoomMsgDao(DataSource dataSource) {
        super(dataSource, RoomMsgDto.class);
    }

    public CompletableFuture<Void> addMsg(RoomMsgDto roomMsg) {
        return supplyAsync(connection -> {
            String insert = "INSERT INTO room_messages(sender, room, message, sending_time) \n" +
                    "VALUES (?, ?, ?, to_timestamp(?));";
            getQueryRunner().insert(connection, insert, getDefaultResultSetHandler(), roomMsg.getSender(),
                                    roomMsg.getRoom(), roomMsg.getMessage(),
                                    roomMsg.getSendingTime().getTime() / 1000.0);

            return null;
        });
    }

    public CompletableFuture<MsgHistoryOutMessage> getHistoryForRoom(String room, long timestampTo, int limit) {
        return supplyAsync(connection -> {
            String query = "SELECT sender, room, message, sending_time AS sendingTime\n" +
                    "FROM room_messages \n" +
                    "WHERE room = ? AND sending_time <= to_timestamp(?)\n" +
                    "LIMIT ?;";
            List<ChatMsg> list =
                    getQueryRunner().query(connection, query, getDefaultResultSetHandler(), room, timestampTo, limit)
                                    .stream()
                                    .map(RoomMsgDto::toChatMsg)
                                    .collect(Collectors.toList());
            return new MsgHistoryOutMessage(list);
        });
    }
}
