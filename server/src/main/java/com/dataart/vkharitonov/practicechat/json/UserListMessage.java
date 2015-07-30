package com.dataart.vkharitonov.practicechat.json;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class UserListMessage {
    private List<String> users;

    public UserListMessage(Collection<String> users) {
        this.users = new ArrayList<>(users);
    }
}
