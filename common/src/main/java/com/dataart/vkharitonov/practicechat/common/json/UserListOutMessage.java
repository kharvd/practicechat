package com.dataart.vkharitonov.practicechat.common.json;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class UserListOutMessage {
    private List<String> users;

    public UserListOutMessage(Collection<String> users) {
        this.users = new ArrayList<>(users);
    }
}
