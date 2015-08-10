package com.dataart.vkharitonov.practicechat.server.db;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.sql.DataSource;
import java.util.concurrent.CompletableFuture;

public class RoomMsgDao extends Dao<RoomMsgDto> {

    private final static Logger log = LoggerFactory.getLogger(RoomMsgDao.class.getName());

    public RoomMsgDao(DataSource dataSource) {
        super(dataSource, RoomMsgDto.class);
    }

    public CompletableFuture<Void> addMsg(RoomMsgDto roomMsg) {
        return supplyAsync(connection -> {
            String insert = "INSERT INTO room_messages(sender, room, message, sending_time) \n" +
                    "VALUES (?, ?, ?, to_timestamp(?));";
            getQueryRunner().insert(connection, insert, getDefaultResultSetHandler(),
                    roomMsg.getSender(),
                    roomMsg.getRoom(),
                    roomMsg.getMessage(),
                    roomMsg.getSendingTime().getTime() / 1000.0);

            return null;
        });
    }
}
