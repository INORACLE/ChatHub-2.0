/*
 * Decompiled with CFR 0.152.
 */
package com.zhanganzhi.chathub.core.adaptor;

import com.zhanganzhi.chathub.core.events.MessageEvent;
import com.zhanganzhi.chathub.core.events.ServerChangeEvent;
import com.zhanganzhi.chathub.core.formatter.IFormatter;
import com.zhanganzhi.chathub.platforms.Platform;

public interface IAdaptor<T extends IFormatter> {
    public Platform getPlatform();

    public T getFormatter();

    public void start();

    public void stop();

    public void restart();

    public void sendPublicMessage(String var1);

    public void onUserChat(MessageEvent var1);

    public void onJoinServer(ServerChangeEvent var1);

    public void onLeaveServer(ServerChangeEvent var1);

    public void onSwitchServer(ServerChangeEvent var1);
}

