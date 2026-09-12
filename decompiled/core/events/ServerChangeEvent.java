/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.velocitypowered.api.event.connection.DisconnectEvent
 *  com.velocitypowered.api.event.player.ServerConnectedEvent
 *  com.velocitypowered.api.proxy.Player
 *  com.velocitypowered.api.proxy.server.RegisteredServer
 */
package com.zhanganzhi.chathub.core.events;

import com.velocitypowered.api.event.connection.DisconnectEvent;
import com.velocitypowered.api.event.player.ServerConnectedEvent;
import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.server.RegisteredServer;

public class ServerChangeEvent {
    public final Player player;
    public final String serverPrev;
    public final String server;
    public final SwitchType type;

    public ServerChangeEvent(Player player, String serverPrev, String server) {
        this.player = player;
        this.serverPrev = serverPrev;
        this.server = server;
        this.type = serverPrev != null ? SwitchType.SWITCH : (server != null ? SwitchType.JOIN : SwitchType.LEAVE);
    }

    public ServerChangeEvent(ServerConnectedEvent event) {
        this(event.getPlayer(), event.getPreviousServer().isPresent() ? ((RegisteredServer)event.getPreviousServer().get()).getServerInfo().getName() : null, event.getServer().getServerInfo().getName());
    }

    public ServerChangeEvent(DisconnectEvent event) {
        this(event.getPlayer(), null, null);
    }

    public static enum SwitchType {
        JOIN,
        LEAVE,
        SWITCH;

    }
}

