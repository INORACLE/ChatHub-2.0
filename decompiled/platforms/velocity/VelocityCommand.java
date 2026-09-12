/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.velocitypowered.api.command.CommandSource
 *  com.velocitypowered.api.command.SimpleCommand
 *  com.velocitypowered.api.command.SimpleCommand$Invocation
 *  com.velocitypowered.api.proxy.Player
 *  com.velocitypowered.api.proxy.ProxyServer
 *  net.kyori.adventure.text.Component
 *  net.kyori.adventure.text.format.NamedTextColor
 *  net.kyori.adventure.text.format.TextColor
 */
package com.zhanganzhi.chathub.platforms.velocity;

import com.velocitypowered.api.command.CommandSource;
import com.velocitypowered.api.command.SimpleCommand;
import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.ProxyServer;
import com.zhanganzhi.chathub.ChatHub;
import com.zhanganzhi.chathub.core.EventHub;
import com.zhanganzhi.chathub.core.adaptor.IAdaptor;
import com.zhanganzhi.chathub.core.formatter.IFormatter;
import com.zhanganzhi.chathub.platforms.Platform;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;

public final class VelocityCommand
implements SimpleCommand {
    private final ChatHub chatHub;
    private final ProxyServer proxyServer;
    private final EventHub eventHub;

    public VelocityCommand(ChatHub chatHub) {
        this.chatHub = chatHub;
        this.proxyServer = chatHub.getProxyServer();
        this.eventHub = chatHub.getEventHub();
    }

    public void execute(SimpleCommand.Invocation invocation) {
        CommandSource source2 = invocation.source();
        String[] args2 = (String[])invocation.arguments();
        IAdaptor<? extends IFormatter> velocityAdaptor = this.eventHub.getAdaptor(Platform.VELOCITY);
        if (args2.length == 1 && args2[0].equals("list")) {
            try {
                String fullListMessage = velocityAdaptor.getFormatter().formatListAll(this.proxyServer);
                if (fullListMessage == null || fullListMessage.trim().isEmpty()) {
                    source2.sendMessage(Component.text((String)"\u65e0\u6cd5\u83b7\u53d6\u73a9\u5bb6\u5217\u8868\u4fe1\u606f").color((TextColor)NamedTextColor.RED));
                    return;
                }
                for (String line : fullListMessage.split("\n")) {
                    if (line.trim().isEmpty()) continue;
                    source2.sendMessage((Component)Component.text((String)line));
                }
            }
            catch (Exception e) {
                source2.sendMessage(Component.text((String)("\u83b7\u53d6\u73a9\u5bb6\u5217\u8868\u65f6\u53d1\u751f\u9519\u8bef: " + e.getMessage())).color((TextColor)NamedTextColor.RED));
                this.chatHub.getLogger().error("\u83b7\u53d6\u73a9\u5bb6\u5217\u8868\u65f6\u53d1\u751f\u9519\u8bef", e);
            }
        } else if (args2.length >= 3 && args2[0].equals("msg")) {
            Optional optionalPlayer = this.proxyServer.getPlayer(args2[1]);
            if (optionalPlayer.isPresent()) {
                String senderName = source2 instanceof Player ? ((Player)source2).getUsername() : "Server";
                String message = String.join((CharSequence)" ", Arrays.copyOfRange(args2, 2, args2.length));
                source2.sendMessage((Component)Component.text((String)velocityAdaptor.getFormatter().formatMsgSender(args2[1], message)));
                ((Player)optionalPlayer.get()).sendMessage((Component)Component.text((String)velocityAdaptor.getFormatter().formatMsgTarget(senderName, message)));
            } else {
                source2.sendMessage((Component)Component.text((String)("Player \"" + args2[1] + "\" does not online!")));
            }
        } else if (args2.length == 1 && args2[0].equals("reload")) {
            try {
                this.chatHub.reloadConfig();
                this.chatHub.reloadBindings();
                source2.sendMessage(Component.text((String)"\u914d\u7f6e\u6587\u4ef6\u548c\u7ed1\u5b9a\u6587\u4ef6\u5df2\u6210\u529f\u91cd\u8f7d!").color((TextColor)NamedTextColor.GREEN));
            }
            catch (Exception e) {
                source2.sendMessage(Component.text((String)("\u91cd\u8f7d\u5931\u8d25: " + e.getMessage())).color((TextColor)NamedTextColor.RED));
                this.chatHub.getLogger().error("\u91cd\u8f7d\u914d\u7f6e\u548c\u7ed1\u5b9a\u6587\u4ef6\u5931\u8d25", e);
            }
        }
    }

    public List<String> suggest(SimpleCommand.Invocation invocation) {
        String[] args2 = (String[])invocation.arguments();
        if (args2.length <= 1) {
            return Stream.of("list", "msg", "reload").filter(s -> s.startsWith(args2.length > 0 ? args2[0] : "")).collect(Collectors.toList());
        }
        if (args2.length == 2 && args2[0].equals("msg")) {
            return this.proxyServer.getAllPlayers().stream().map(Player::getUsername).filter(s -> s.toLowerCase().startsWith(args2[1].toLowerCase())).collect(Collectors.toList());
        }
        return List.of();
    }
}

