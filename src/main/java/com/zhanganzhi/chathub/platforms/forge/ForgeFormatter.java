package com.zhanganzhi.chathub.platforms.forge;

import com.zhanganzhi.chathub.core.formatter.AbstractFormatter;
import com.zhanganzhi.chathub.platforms.Platform;

public class ForgeFormatter extends AbstractFormatter {

    public ForgeFormatter() {
        super(Platform.MINECRAFT);
    }

    /**
     * {@code dimension} may be the raw dimension key (from a Minecraft event) or
     * null (from an external platform, e.g. QQ); in the latter case the server
     * display name is used instead so the {@code {dimension}} placeholder never
     * renders empty. Raw keys are mapped to their configured display names.
     */
    @Override
    public String formatUserChat(String server, String dimension, String name, String message) {
        String effectiveDimension = dimension != null ? this.config.getDimensionName(dimension) : server;
        return super.formatUserChat(server, effectiveDimension, name, message);
    }
}