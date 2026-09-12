package com.zhanganzhi.chathub.core.events;

import com.zhanganzhi.chathub.platforms.Platform;

/**
 * A chat message travelling through the event hub.
 *
 * @param platform  platform the message originated from
 * @param server    name of the server/dimension the message came from (game only)
 * @param dimension display name of the dimension the sender is in (game only)
 * @param user      sender display name
 * @param content   message text
 */
public record MessageEvent(Platform platform, String server, String dimension, String user, String content) {

    public String getServerName() {
        if (this.platform == Platform.MINECRAFT) {
            return this.server;
        }
        return this.platform.getName();
    }
}