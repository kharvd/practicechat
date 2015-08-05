package com.dataart.vkharitonov.practicechat.server.event;

public class DisconnectRequest extends Request {

    public DisconnectRequest(String username) {
        super(username);
    }
}
