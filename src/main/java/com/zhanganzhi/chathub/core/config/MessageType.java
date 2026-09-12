package com.zhanganzhi.chathub.core.config;

public enum MessageType {

    CHAT("chat"),
    JOIN("join"),
    LEAVE("leave"),
    LIST("list"),
    LIST_EMPTY("listEmpty"),
    MSG_SENDER("msgSender"),
    MSG_TARGET("msgTarget");

    private final String name;

    MessageType(String name) {
        this.name = name;
    }

    public String getName() {
        return this.name;
    }
}