/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.velocitypowered.api.proxy.ProxyServer
 */
package com.zhanganzhi.chathub.core.formatter;

import com.velocitypowered.api.proxy.ProxyServer;
import com.zhanganzhi.chathub.platforms.Platform;
import java.util.List;

public interface IFormatter {
    public Platform getPlatform();

    public String formatUserChat(String var1, String var2, String var3);

    public String formatJoinServer(String var1, String var2);

    public String formatLeaveServer(String var1);

    public String formatSwitchServer(String var1, String var2, String var3);

    public String formatMsgSender(String var1, String var2);

    public String formatMsgTarget(String var1, String var2);

    public String formatList(String var1, int var2, List<String> var3);

    public String formatListEmpty();

    public String formatListAll(ProxyServer var1);
}

