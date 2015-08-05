package com.dataart.vkharitonov.practicechat.server.event;

public class HistoryRequest extends Request {

    private String partner;

    public HistoryRequest(String sender, String partner) {
        super(sender);
        this.partner = partner;
    }

    public String getPartner() {
        return partner;
    }
}
