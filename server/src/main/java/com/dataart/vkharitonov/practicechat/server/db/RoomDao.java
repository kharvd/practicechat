package com.dataart.vkharitonov.practicechat.server.db;

import com.dataart.vkharitonov.practicechat.server.db.dto.RoomDto;
import org.apache.commons.dbutils.handlers.ArrayListHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.sql.DataSource;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

public class RoomDao extends Dao<RoomDto> {

    private final static Logger log = LoggerFactory.getLogger(RoomDao.class.getName());

    public RoomDao(DataSource dataSource) {
        super(dataSource, RoomDto.class);
    }

    public CompletableFuture<Optional<String>> getRoomAdmin(String room) {
        return supplyAsync(connection -> {
            String queryExists = "SELECT * FROM rooms WHERE name = ?;";
            List<RoomDto> rooms = getQueryRunner().query(connection, queryExists, getDefaultResultSetHandler(), room);

            return rooms.stream().findFirst().map(RoomDto::getAdmin);
        });
    }

    public CompletableFuture<Void> createRoom(String roomName, String admin) {
        return supplyAsync(connection -> {
            String createRoom = "INSERT INTO rooms(name, admin) VALUES (?, ?);";
            getQueryRunner().insert(connection, createRoom, getDefaultResultSetHandler(), roomName, admin);

            return null;
        }).thenCompose(o -> addUserToRoom(roomName, admin));
    }

    public CompletableFuture<Void> addUserToRoom(String roomName, String username) {
        return supplyAsync(connection -> {
            String joinRoom = "INSERT INTO room_members(room, username) VALUES (?, ?);";
            getQueryRunner().insert(connection, joinRoom, rs -> null, roomName, username);

            log.debug("user joined");
            return null;
        });
    }

    public CompletableFuture<Boolean> removeUserFromRoom(String roomName, String username) {
        return supplyAsync(connection -> {
            String leaveRoom = "DELETE FROM room_members WHERE room = ? AND username = ?;";
            int rowsUpdated = getQueryRunner().update(connection, leaveRoom, roomName, username);
            return rowsUpdated > 0;
        });
    }

    public CompletableFuture<Boolean> dropRoom(String roomName, String username) {
        return supplyAsync(connection -> {
            String dropRoom = "DELETE FROM rooms WHERE name = ? AND admin = ?;";
            int rowsUpdated = getQueryRunner().update(connection, dropRoom, roomName, username);
            return rowsUpdated > 0;
        });
    }

    public CompletableFuture<List<String>> getUsersForRoom(String roomName) {
        return supplyAsync(connection -> {
            String query = "SELECT username FROM room_members WHERE room = ?;";
            return getQueryRunner().query(connection, query, new ArrayListHandler(), roomName)
                                   .stream()
                                   .map(arr -> ((String) arr[0]))
                                   .collect(Collectors.toList());
        });
    }

    public CompletableFuture<List<String>> getRooms() {
        return supplyAsync(connection -> {
            String query = "SELECT name FROM rooms;";
            return getQueryRunner().query(connection, query, new ArrayListHandler())
                                   .stream()
                                   .map(arr -> ((String) arr[0]))
                                   .collect(Collectors.toList());
        });
    }
}
