/*
 * Decompiled with CFR 0.152.
 */
package com.zhanganzhi.chathub.core;

import com.zhanganzhi.chathub.ChatHub;
import com.zhanganzhi.chathub.core.adaptor.IAdaptor;
import com.zhanganzhi.chathub.core.binding.BindingManagerV2;
import com.zhanganzhi.chathub.core.config.Config;
import com.zhanganzhi.chathub.core.events.MessageEvent;
import com.zhanganzhi.chathub.core.events.ServerChangeEvent;
import com.zhanganzhi.chathub.core.formatter.IFormatter;
import com.zhanganzhi.chathub.platforms.Platform;
import com.zhanganzhi.chathub.platforms.qq.QQAdaptor;
import com.zhanganzhi.chathub.platforms.qq.QQBotAdapter;
import com.zhanganzhi.chathub.platforms.velocity.VelocityAdaptor;
import java.util.ArrayList;
import java.util.List;

public class EventHub {
    private final List<IAdaptor<? extends IFormatter>> adaptors;
    private BindingManagerV2 bindingManager;

    public EventHub(ChatHub chatHub) {
        Config config = Config.getInstance();
        this.adaptors = new ArrayList<IAdaptor<? extends IFormatter>>();
        this.adaptors.add(new VelocityAdaptor(chatHub));
        if (config.isQQEnabled()) {
            String appId = config.getQQAppId();
            if (appId != null && !appId.isEmpty()) {
                this.adaptors.add(new QQBotAdapter(chatHub));
            } else {
                this.adaptors.add(new QQAdaptor(chatHub));
            }
        }
        this.bindingManager = new BindingManagerV2(chatHub.getDataDirectory());
        this.bindingManager.setLogger(chatHub.getLogger());
        this.bindingManager.loadBindings();
    }

    public IAdaptor<? extends IFormatter> getAdaptor(Platform platform) {
        return this.adaptors.stream().filter(adaptor -> adaptor.getPlatform() == platform).findFirst().orElse(null);
    }

    public QQAdaptor getQQAdaptor() {
        return this.adaptors.stream().filter(adaptor -> adaptor.getPlatform() == Platform.QQ).map(adaptor -> (QQAdaptor)adaptor).findFirst().orElse(null);
    }

    public void onUserChat(MessageEvent event) {
        this.adaptors.stream().filter(adaptor -> event.platform() == Platform.VELOCITY || event.platform() != adaptor.getPlatform()).forEach(adaptor -> adaptor.onUserChat(event));
    }

    public void onJoinServer(ServerChangeEvent event) {
        this.adaptors.stream().filter(adaptor -> adaptor.getPlatform() != Platform.QQ).forEach(adaptor -> adaptor.onJoinServer(event));
    }

    public void onLeaveServer(ServerChangeEvent event) {
        this.adaptors.stream().filter(adaptor -> adaptor.getPlatform() != Platform.QQ).forEach(adaptor -> adaptor.onLeaveServer(event));
    }

    public void onSwitchServer(ServerChangeEvent event) {
        this.adaptors.stream().filter(adaptor -> adaptor.getPlatform() != Platform.QQ).forEach(adaptor -> adaptor.onSwitchServer(event));
    }

    public void shutdown() {
        this.adaptors.forEach(IAdaptor::stop);
    }

    public void start() {
        this.adaptors.forEach(IAdaptor::start);
    }

    public void reloadBindings() {
        if (this.bindingManager != null) {
            this.bindingManager.reloadBindings();
        }
    }

    public BindingManagerV2 getBindingManager() {
        return this.bindingManager;
    }
}

