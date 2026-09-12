/*
 * Decompiled with CFR 0.152.
 */
package com.zhanganzhi.chathub.core.config;

public enum MessageType {
    CHAT("chat"),
    JOIN("join"),
    LEAVE("leave"),
    SWITCH("switch"),
    MSG_SENDER("msgSender"),
    MSG_TARGET("msgTarget"),
    LIST("list"),
    LIST_EMPTY("listEmpty");

    private final String name;

    private MessageType(String name) {
        this.name = name;
    }

    public String getName() {
        return this.name;
    }
}

