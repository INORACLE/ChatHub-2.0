/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.velocitypowered.api.proxy.ProxyServer
 *  com.velocitypowered.api.proxy.server.RegisteredServer
 */
package com.zhanganzhi.chathub.platforms.qq;

import com.velocitypowered.api.proxy.ProxyServer;
import com.velocitypowered.api.proxy.server.RegisteredServer;
import com.zhanganzhi.chathub.core.formatter.AbstractFormatter;
import com.zhanganzhi.chathub.core.formatter.FormattingContent;
import com.zhanganzhi.chathub.platforms.Platform;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;

public class QQBotFormatter
extends AbstractFormatter {
    protected QQBotFormatter() {
        super(Platform.QQ);
    }

    @Override
    protected String replaceAll(String message, FormattingContent content) {
        return this.getPlainString(super.replaceAll(message, content));
    }

    @Override
    public String formatListAll(ProxyServer proxyServer) {
        String displayName;
        if (proxyServer == null) {
            return "\u65e0\u6cd5\u83b7\u53d6\u670d\u52a1\u5668\u4fe1\u606f";
        }
        HashMap<String, Iterator<String>> serverPlayerMap = new HashMap<String, Iterator<String>>();
        int totalPlayers = 0;
        for (RegisteredServer registeredServer : proxyServer.getAllServers()) {
            Collection connectedPlayers;
            if (registeredServer == null || (connectedPlayers = registeredServer.getPlayersConnected()) == null) continue;
            Iterator<String> playerNames = new ArrayList();
            for (Object player : connectedPlayers) {
                if (player == null) continue;
                playerNames.add(player.getUsername());
            }
            serverPlayerMap.put(registeredServer.getServerInfo().getName(), playerNames);
            totalPlayers += connectedPlayers.size();
        }
        StringBuilder result = new StringBuilder();
        result.append("\u5728\u7ebf\u73a9\u5bb6\u603b\u4eba\u6570\uff1a").append(totalPlayers).append("\n");
        List<String> serverOrder = this.config.getServerOrder();
        boolean isFirst = true;
        block2: for (String expectedServer : serverOrder) {
            for (RegisteredServer registeredServer : proxyServer.getAllServers()) {
                int playerCount;
                if (registeredServer == null || !registeredServer.getServerInfo().getName().toLowerCase().contains(expectedServer.toLowerCase())) continue;
                String serverName = registeredServer.getServerInfo().getName();
                displayName = this.config.getServername(serverName);
                List playerNames = (List)serverPlayerMap.get(serverName);
                int n = playerCount = playerNames != null ? playerNames.size() : 0;
                if (!isFirst) {
                    result.append("\n");
                } else {
                    isFirst = false;
                }
                result.append("\u3010").append(this.getPlainString(displayName)).append("\u3011\uff1a").append(playerCount);
                continue block2;
            }
        }
        for (RegisteredServer registeredServer : proxyServer.getAllServers()) {
            if (registeredServer == null) continue;
            String serverName = registeredServer.getServerInfo().getName();
            boolean alreadyAdded = false;
            for (String expectedServer : serverOrder) {
                if (!serverName.toLowerCase().contains(expectedServer.toLowerCase())) continue;
                alreadyAdded = true;
                break;
            }
            if (alreadyAdded) continue;
            String displayName2 = this.config.getServername(serverName);
            List playerNames = (List)serverPlayerMap.get(serverName);
            int playerCount = playerNames != null ? playerNames.size() : 0;
            result.append("\n").append("\u3010").append(this.getPlainString(displayName2)).append("\u3011\uff1a").append(playerCount);
        }
        result.append("\n");
        boolean isDetailsFirst = true;
        block6: for (String expectedServer : serverOrder) {
            for (RegisteredServer registeredServer : proxyServer.getAllServers()) {
                int playerCount;
                if (registeredServer == null || !registeredServer.getServerInfo().getName().toLowerCase().contains(expectedServer.toLowerCase())) continue;
                String serverName = registeredServer.getServerInfo().getName();
                String displayName3 = this.config.getServername(serverName);
                List playerNames = (List)serverPlayerMap.get(serverName);
                int n = playerCount = playerNames != null ? playerNames.size() : 0;
                if (!isDetailsFirst) {
                    result.append("\n");
                } else {
                    isDetailsFirst = false;
                }
                result.append("\u3010").append(this.getPlainString(displayName3)).append("\u3011\u5728\u7ebf\u4eba\u6570\uff1a").append(playerCount);
                if (playerNames == null || playerNames.isEmpty()) continue block6;
                result.append("\n").append(String.join((CharSequence)" ", playerNames));
                continue block6;
            }
        }
        for (RegisteredServer registeredServer : proxyServer.getAllServers()) {
            if (registeredServer == null) continue;
            String serverName = registeredServer.getServerInfo().getName();
            boolean alreadyAdded = false;
            for (String expectedServer : serverOrder) {
                if (!serverName.toLowerCase().contains(expectedServer.toLowerCase())) continue;
                alreadyAdded = true;
                break;
            }
            if (alreadyAdded) continue;
            displayName = this.config.getServername(serverName);
            List playerNames = (List)serverPlayerMap.get(serverName);
            int playerCount = playerNames != null ? playerNames.size() : 0;
            result.append("\n");
            result.append("\u3010").append(this.getPlainString(displayName)).append("\u3011\u5728\u7ebf\u4eba\u6570\uff1a").append(playerCount);
            if (playerNames == null || playerNames.isEmpty()) continue;
            result.append("\n").append(String.join((CharSequence)" ", playerNames));
        }
        return result.toString();
    }
}

