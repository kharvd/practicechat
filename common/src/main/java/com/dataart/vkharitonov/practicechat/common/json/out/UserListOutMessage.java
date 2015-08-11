package com.dataart.vkharitonov.practicechat.common.json.out;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class UserListOutMessage {

    private String room;
    private List<User> users;

    public UserListOutMessage(Collection<User> users) {
        this.users = new ArrayList<>(users);
    }

    public UserListOutMessage(String room,
                              List<User> users) {
        this.room = room;
        this.users = users;
    }

    public String getRoom() {
        return room;
    }

    public List<User> getUsers() {
        return users;
    }

    public static class User {

        private String username;
        private boolean online;

        public User(String username, boolean online) {
            this.username = username;
            this.online = online;
        }

        public String getUsername() {
            return username;
        }

        public boolean isOnline() {
            return online;
        }

        @Override
        public String toString() {
            return "User{" +
                    "username='" + username + '\'' +
                    ", online=" + online +
                    '}';
        }
    }
}
