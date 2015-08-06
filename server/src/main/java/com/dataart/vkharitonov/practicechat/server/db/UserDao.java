package com.dataart.vkharitonov.practicechat.server.db;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.sql.DataSource;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

public class UserDao extends Dao<UserDto> {

    private final static Logger log = LoggerFactory.getLogger(UserDao.class.getName());

    protected UserDao(DataSource dataSource) {
        super(dataSource, UserDto.class);
    }

    public CompletableFuture<Void> createUser(String name, String hash, String salt) {
        return supplyAsync(connection -> {
            String insert = "INSERT INTO users(name, hash, salt) VALUES (?, ?, ?);";
            getQueryRunner().insert(connection, insert, getDefaultResultSetHandler(),
                    name, hash, salt);

            return null;
        });
    }

    public CompletableFuture<Optional<UserDto>> getUserByName(String name) {
        return supplyAsync(connection -> {
            String query = "SELECT * FROM users WHERE name = ?;";

            List<UserDto> user = getQueryRunner().query(connection, query, getDefaultResultSetHandler(), name);

            return user.stream().findFirst();
        });
    }
}
