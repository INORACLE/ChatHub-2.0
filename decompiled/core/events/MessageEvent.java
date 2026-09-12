/*
 * Decompiled with CFR 0.152.
 */
package com.zhanganzhi.chathub.core.events;

import com.zhanganzhi.chathub.platforms.Platform;

public record MessageEvent(Platform platform, String server, String user, String content) {
    public String getServerName() {
        if (this.platform == Platform.VELOCITY) {
            return this.server;
        }
        return this.platform.getName();
    }
}

