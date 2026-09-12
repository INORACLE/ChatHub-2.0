package com.zhanganzhi.chathub.core;

import com.zhanganzhi.chathub.ChatHubMod;
import com.zhanganzhi.chathub.core.adaptor.AbstractAdaptor;
import com.zhanganzhi.chathub.core.adaptor.IAdaptor;
import com.zhanganzhi.chathub.core.binding.BindingManagerV2;
import com.zhanganzhi.chathub.core.config.Config;
import com.zhanganzhi.chathub.core.events.MessageEvent;
import com.zhanganzhi.chathub.core.events.ServerChangeEvent;
import com.zhanganzhi.chathub.core.formatter.IFormatter;
import com.zhanganzhi.chathub.platforms.Platform;
import com.zhanganzhi.chathub.platforms.qq.QQAdaptor;
import com.zhanganzhi.chathub.platforms.qq.QQBotAdapter;
import org.apache.logging.log4j.Logger;

import java.util.ArrayList;
import java.util.List;

public class EventHub {

    private final Logger logger;
    private final ChatHubMod chatHub;
    private final BindingManagerV2 bindingManager;

    public EventHub(ChatHubMod chatHub, Logger logger) {
        this.chatHub = chatHub;
        this.logger = logger;
        this.bindingManager = new BindingManagerV2(chatHub.getDataDirectory());
        this.bindingManager.setLogger(logger);
    }

    public BindingManagerV2 getBindingManager() {
        return this.bindingManager;
    }

    public void initBindingManager() {
        this.bindingManager.loadBindings();
    }

    public void reloadBindingManager() {
        this.bindingManager.reloadBindings();
    }

    public void onUserChat(MessageEvent event) {
        if (event == null || event.user() == null || event.content() == null) {
            return;
        }
        for (IAdaptor<? extends IFormatter> adaptor : this.chatHub.getAdaptors()) {
            if (event.platform() == Platform.MINECRAFT || event.platform() != adaptor.getPlatform()) {
                adaptor.onUserChat(event);
            }
        }
    }

    public void onJoinServer(ServerChangeEvent event) {
        if (event == null || event.playerName == null) {
            return;
        }
        for (IAdaptor<? extends IFormatter> adaptor : this.chatHub.getAdaptors()) {
            adaptor.onJoinServer(event);
        }
    }

    public void onLeaveServer(ServerChangeEvent event) {
        if (event == null || event.playerName == null) {
            return;
        }
        for (IAdaptor<? extends IFormatter> adaptor : this.chatHub.getAdaptors()) {
            adaptor.onLeaveServer(event);
        }
    }

    public void sendPublicMessage(String message) {
        for (IAdaptor<? extends IFormatter> adaptor : this.chatHub.getAdaptors()) {
            adaptor.sendPublicMessage(message);
        }
    }

    public void sendPublicMessageFromQQ(String message) {
        for (IAdaptor<? extends IFormatter> adaptor : this.chatHub.getAdaptors()) {
            if (adaptor instanceof QQAdaptor || adaptor instanceof QQBotAdapter) {
                continue;
            }
            adaptor.sendPublicMessage(message);
        }
    }

    public void sendMinecraftMessageFromQQ(String message) {
        this.sendPublicMessageFromQQ(message);
    }

    public List<IAdaptor<? extends IFormatter>> getAdaptors() {
        List<IAdaptor<? extends IFormatter>> adaptors = new ArrayList<>(this.chatHub.getAdaptors());
        return adaptors;
    }

    public Logger getLogger() {
        return this.logger;
    }
}