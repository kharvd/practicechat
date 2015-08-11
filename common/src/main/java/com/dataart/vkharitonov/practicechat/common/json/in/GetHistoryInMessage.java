package com.dataart.vkharitonov.practicechat.common.json.in;

public class GetHistoryInMessage {

    private String username;
    private int limit;
    private long timestampTo;

    public GetHistoryInMessage(String username, int limit, long timestampTo) {
        this.username = username;
        this.limit = limit;
        this.timestampTo = timestampTo;
    }

    public String getUsername() {
        return username;
    }

    public long getTimestampTo() {
        return timestampTo;
    }

    public int getLimit() {
        return limit;
    }
}
