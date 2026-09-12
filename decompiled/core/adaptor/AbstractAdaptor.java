/*
 * Decompiled with CFR 0.152.
 */
package com.zhanganzhi.chathub.core.adaptor;

import com.zhanganzhi.chathub.ChatHub;
import com.zhanganzhi.chathub.core.adaptor.IAdaptor;
import com.zhanganzhi.chathub.core.config.Config;
import com.zhanganzhi.chathub.core.events.MessageEvent;
import com.zhanganzhi.chathub.core.events.ServerChangeEvent;
import com.zhanganzhi.chathub.core.formatter.IFormatter;
import com.zhanganzhi.chathub.platforms.Platform;

public abstract class AbstractAdaptor<T extends IFormatter>
implements IAdaptor<T> {
    protected final Config config = Config.getInstance();
    protected final ChatHub chatHub;
    protected final Platform platform;
    protected final T formatter;

    protected AbstractAdaptor(ChatHub chatHub, Platform platform, T formatter) {
        this.chatHub = chatHub;
        this.platform = platform;
        this.formatter = formatter;
    }

    @Override
    public Platform getPlatform() {
        return this.platform;
    }

    @Override
    public T getFormatter() {
        return this.formatter;
    }

    @Override
    public void start() {
    }

    @Override
    public void stop() {
    }

    @Override
    public void restart() {
        this.stop();
        this.start();
    }

    @Override
    public void onUserChat(MessageEvent event) {
        if (event == null || event.content() == null) {
            return;
        }
        String formattedMessage = this.formatter.formatUserChat(event.getServerName(), event.user(), event.content());
        if (formattedMessage != null && !formattedMessage.isEmpty()) {
            this.sendPublicMessage(formattedMessage);
        }
    }

    @Override
    public void onJoinServer(ServerChangeEvent event) {
        if (event == null || event.player == null) {
            return;
        }
        String formattedMessage = this.formatter.formatJoinServer(event.server, event.player.getUsername());
        if (formattedMessage != null && !formattedMessage.isEmpty()) {
            this.sendPublicMessage(formattedMessage);
        }
    }

    @Override
    public void onLeaveServer(ServerChangeEvent event) {
        if (event == null || event.player == null) {
            return;
        }
        String formattedMessage = this.formatter.formatLeaveServer(event.player.getUsername());
        if (formattedMessage != null && !formattedMessage.isEmpty()) {
            this.sendPublicMessage(formattedMessage);
        }
    }

    @Override
    public void onSwitchServer(ServerChangeEvent event) {
        if (event == null || event.player == null) {
            return;
        }
        String formattedMessage = this.formatter.formatSwitchServer(event.player.getUsername(), event.serverPrev, event.server);
        if (formattedMessage != null && !formattedMessage.isEmpty()) {
            this.sendPublicMessage(formattedMessage);
        }
    }
}

