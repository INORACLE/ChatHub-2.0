/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.velocitypowered.api.event.Subscribe
 *  com.velocitypowered.api.event.connection.DisconnectEvent
 *  com.velocitypowered.api.event.player.PlayerChatEvent
 *  com.velocitypowered.api.event.player.PlayerChatEvent$ChatResult
 *  com.velocitypowered.api.event.player.ServerConnectedEvent
 *  com.velocitypowered.api.proxy.Player
 *  com.velocitypowered.api.proxy.ProxyServer
 *  com.velocitypowered.api.proxy.server.RegisteredServer
 *  net.kyori.adventure.text.Component
 *  net.kyori.adventure.text.TextComponent
 *  net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer
 */
package com.zhanganzhi.chathub.platforms.velocity;

import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.connection.DisconnectEvent;
import com.velocitypowered.api.event.player.PlayerChatEvent;
import com.velocitypowered.api.event.player.ServerConnectedEvent;
import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.ProxyServer;
import com.velocitypowered.api.proxy.server.RegisteredServer;
import com.zhanganzhi.chathub.ChatHub;
import com.zhanganzhi.chathub.core.EventHub;
import com.zhanganzhi.chathub.core.adaptor.AbstractAdaptor;
import com.zhanganzhi.chathub.core.binding.BindingManagerV2;
import com.zhanganzhi.chathub.core.events.MessageEvent;
import com.zhanganzhi.chathub.core.events.ServerChangeEvent;
import com.zhanganzhi.chathub.platforms.Platform;
import com.zhanganzhi.chathub.platforms.qq.QQAdaptor;
import com.zhanganzhi.chathub.platforms.velocity.VelocityFormatter;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Pattern;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;

public class VelocityAdaptor
extends AbstractAdaptor<VelocityFormatter> {
    public VelocityAdaptor(ChatHub chatHub) {
        super(chatHub, Platform.VELOCITY, new VelocityFormatter());
    }

    private EventHub getEventHub() {
        return this.chatHub.getEventHub();
    }

    private ProxyServer getProxyServer() {
        return this.chatHub.getProxyServer();
    }

    private void sendMessage(Component component, String ... ignoredServers) {
        if (component == null) {
            return;
        }
        List<String> ignoredServerList = Arrays.asList(ignoredServers);
        for (RegisteredServer registeredServer : this.getProxyServer().getAllServers()) {
            String serverName;
            if (registeredServer == null || ignoredServerList.contains(serverName = registeredServer.getServerInfo().getName())) continue;
            for (Player player : registeredServer.getPlayersConnected()) {
                if (player == null || !player.isActive()) continue;
                try {
                    player.sendMessage(component);
                }
                catch (Exception e) {
                    this.chatHub.getLogger().warn("Failed to send message to player: {}", (Object)player.getUsername(), (Object)e);
                }
            }
        }
    }

    private boolean shouldIgnoreMessage(String message) {
        if (message == null) {
            return false;
        }
        for (Pattern pattern : this.config.getMinecraftIgnoreChatMessagePatterns()) {
            if (pattern == null || !pattern.matcher(message).find()) continue;
            return true;
        }
        return false;
    }

    @Override
    public void start() {
        this.getProxyServer().getEventManager().register((Object)this.chatHub, (Object)this);
    }

    @Override
    public void sendPublicMessage(String message) {
        if (message == null || message.isEmpty()) {
            return;
        }
        this.sendMessage((Component)Component.text((String)message), new String[0]);
    }

    @Override
    public void onUserChat(MessageEvent event) {
        if (event == null || event.content() == null) {
            return;
        }
        for (String line : event.content().split("\n")) {
            if (this.shouldIgnoreMessage(line)) continue;
            TextComponent component = Component.text((String)((VelocityFormatter)this.formatter).formatUserChat(event.getServerName(), event.user(), line));
            if (event.platform() == Platform.VELOCITY && !this.config.isCompleteTakeoverMode()) {
                this.sendMessage((Component)component, event.server());
                continue;
            }
            this.sendMessage((Component)component, new String[0]);
        }
    }

    @Override
    public void onJoinServer(ServerChangeEvent event) {
        if (event == null || event.player == null) {
            return;
        }
        String formattedMessage = ((VelocityFormatter)this.formatter).formatJoinServer(event.server, event.player.getUsername());
        if (formattedMessage != null && !formattedMessage.isEmpty()) {
            this.sendPublicMessage(formattedMessage);
        }
    }

    @Override
    public void onLeaveServer(ServerChangeEvent event) {
        if (event == null || event.player == null) {
            return;
        }
        String formattedMessage = ((VelocityFormatter)this.formatter).formatLeaveServer(event.player.getUsername());
        if (formattedMessage != null && !formattedMessage.isEmpty()) {
            this.sendPublicMessage(formattedMessage);
        }
    }

    @Override
    public void onSwitchServer(ServerChangeEvent event) {
        if (event == null || event.player == null) {
            return;
        }
        String formattedMessage = ((VelocityFormatter)this.formatter).formatSwitchServer(event.player.getUsername(), event.serverPrev, event.server);
        if (formattedMessage != null && !formattedMessage.isEmpty()) {
            this.sendPublicMessage(formattedMessage);
        }
    }

    @Subscribe
    public void onPlayerChatEvent(PlayerChatEvent event) {
        if (event == null) {
            return;
        }
        Player player = event.getPlayer();
        if (player == null || this.shouldIgnoreMessage(event.getMessage())) {
            return;
        }
        player.getCurrentServer().ifPresent(serverConnection -> {
            this.getEventHub().onUserChat(new MessageEvent(this.platform, serverConnection.getServerInfo().getName(), player.getUsername(), event.getMessage()));
            if (this.config.isCompleteTakeoverMode()) {
                event.setResult(PlayerChatEvent.ChatResult.denied());
            }
        });
    }

    @Subscribe
    public void onServerConnectedEvent(ServerConnectedEvent event) {
        String playerName;
        BindingManagerV2 bindingManager;
        QQAdaptor qqAdaptor;
        if (event == null) {
            return;
        }
        if (this.config.isQQEnableBinding() && (qqAdaptor = this.getEventHub().getQQAdaptor()) != null && !(bindingManager = qqAdaptor.getBindingManager()).isMcNameBound(playerName = event.getPlayer().getUsername())) {
            String verificationCode = bindingManager.generateVerificationCode(playerName);
            LocalDateTime expireTime = LocalDateTime.now().plusMinutes(5L);
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("HH:mm:ss");
            String expireTimeStr = expireTime.format(formatter);
            String kickMessage = "&l&k123456&6&l\u672a\u7ed1\u5b9a\u793e\u4ea4\u5e10\u53f7&9" + verificationCode + "&f&l&k123456\n&f&a\u8fdb\u5165\u670d\u52a1\u5668\u9700\u8981\u7ed1\u5b9a\u60a8\u7684\u793e\u4ea4\u5e10\u53f7\n\n&f\u8bf7\u5148\u52a0\u7fa4&e584593799\n&f\u53d1\u9001\u5185\u5bb9:&a\u7ed1\u5b9a " + verificationCode + "\n&f\u53d1\u9001\u540e\u65b9\u53ef\u8fdb\u5165\u670d\u52a1\u5668\n\n&7\u8bf7\u5728" + expireTimeStr + "\u524d\u5b8c\u6210\u9a8c\u8bc1\n&7\u8bf7\u4e0d\u8981\u5c06\u9a8c\u8bc1\u7801\u53d1\u9001\u7ed9\u5176\u4ed6\u4eba\n&7\u9a8c\u8bc1\u8d85\u65f6\u540e\u9700\u91cd\u65b0\u8fdb\u5165\u670d\u52a1\u5668";
            Component component = this.convertLegacyToComponent(kickMessage);
            event.getPlayer().disconnect(component);
            return;
        }
        ServerChangeEvent changeEvent = new ServerChangeEvent(event);
        switch (changeEvent.type) {
            case JOIN: {
                this.getEventHub().onJoinServer(changeEvent);
                break;
            }
            case LEAVE: {
                this.getEventHub().onLeaveServer(changeEvent);
                break;
            }
            case SWITCH: {
                this.getEventHub().onSwitchServer(changeEvent);
            }
        }
    }

    @Subscribe
    public void onPlayerDisconnect(DisconnectEvent event) {
        if (event == null) {
            return;
        }
        this.getEventHub().onLeaveServer(new ServerChangeEvent(event));
    }

    private Component convertLegacyToComponent(String text) {
        return LegacyComponentSerializer.legacyAmpersand().deserialize(text);
    }
}

