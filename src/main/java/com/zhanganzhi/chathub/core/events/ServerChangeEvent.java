package com.zhanganzhi.chathub.core.events;

public class ServerChangeEvent {

    public final String playerName;
    public final String serverPrev;
    public final String server;
    public final SwitchType type;

    public ServerChangeEvent(String playerName, String serverPrev, String server) {
        this.playerName = playerName;
        this.serverPrev = serverPrev;
        this.server = server;
        this.type = serverPrev != null ? SwitchType.SWITCH
                : (server != null ? SwitchType.JOIN : SwitchType.LEAVE);
    }

    public enum SwitchType {
        JOIN,
        LEAVE,
        SWITCH
    }
}