/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.velocitypowered.api.proxy.Player
 *  com.velocitypowered.api.proxy.ProxyServer
 *  com.velocitypowered.api.proxy.server.RegisteredServer
 */
package com.zhanganzhi.chathub.platforms.velocity;

import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.ProxyServer;
import com.velocitypowered.api.proxy.server.RegisteredServer;
import com.zhanganzhi.chathub.core.formatter.AbstractFormatter;
import com.zhanganzhi.chathub.platforms.Platform;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;

public class VelocityFormatter
extends AbstractFormatter {
    public VelocityFormatter() {
        super(Platform.VELOCITY);
    }

    @Override
    public String formatListAll(ProxyServer proxyServer) {
        if (proxyServer == null) {
            return "\u65e0\u6cd5\u83b7\u53d6\u670d\u52a1\u5668\u4fe1\u606f";
        }
        HashMap serverPlayerMap = new HashMap();
        int totalPlayers = 0;
        for (RegisteredServer registeredServer : proxyServer.getAllServers()) {
            Collection players;
            if (registeredServer == null || (players = registeredServer.getPlayersConnected()) == null) continue;
            ArrayList<String> playerNames = new ArrayList<String>();
            for (Player player : players) {
                if (player == null) continue;
                playerNames.add(player.getUsername());
            }
            serverPlayerMap.put(registeredServer.getServerInfo().getName(), playerNames);
            totalPlayers += players.size();
        }
        StringBuilder output = new StringBuilder();
        output.append("\u603b\u8ba1\u5728\u7ebf: ").append(totalPlayers).append("\u4eba\n\n");
        for (RegisteredServer server : proxyServer.getAllServers()) {
            if (server == null) continue;
            String serverName = server.getServerInfo().getName();
            String displayName = this.getPlainString(this.config.getServername(serverName));
            List players = (List)serverPlayerMap.get(serverName);
            int count = players != null ? players.size() : 0;
            output.append("\u670d[").append(displayName).append("]: ").append(count).append("\u4eba");
            if (players != null && !players.isEmpty()) {
                output.append(" (").append(String.join((CharSequence)", ", players)).append(")");
            }
            output.append("\n");
        }
        return output.toString();
    }
}

