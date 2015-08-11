package com.dataart.vkharitonov.practicechat.common.json.out;

import com.dataart.vkharitonov.practicechat.common.json.ChatMsg;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class MsgHistoryOutMessage {

    private List<ChatMsg> messages;

    public MsgHistoryOutMessage(List<ChatMsg> messages) {
        this.messages = new ArrayList<>(messages);
    }

    public List<ChatMsg> getMessages() {
        return Collections.unmodifiableList(messages);
    }
}
