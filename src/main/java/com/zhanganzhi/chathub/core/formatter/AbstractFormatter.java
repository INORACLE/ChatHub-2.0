package com.zhanganzhi.chathub.core.formatter;

import com.zhanganzhi.chathub.core.IPlayerManager;
import com.zhanganzhi.chathub.core.config.Config;
import com.zhanganzhi.chathub.core.config.MessageType;
import com.zhanganzhi.chathub.platforms.Platform;

import java.util.ArrayList;
import java.util.List;

public abstract class AbstractFormatter implements IFormatter {

    protected final Config config = Config.getInstance();
    protected final Platform platform;

    protected AbstractFormatter(Platform platform) {
        this.platform = platform;
    }

    @Override
    public Platform getPlatform() {
        return this.platform;
    }

    protected String getPlainString(String string) {
        if (string == null) {
            return "";
        }
        return string.replaceAll("\u00a7.", "");
    }

    protected String replaceAll(String message, FormattingContent content) {
        if (content.getServer() != null) {
            message = message.replace("{server}", this.config.getServername(content.getServer()))
                    .replace("{plainServer}", this.getPlainString(this.config.getServername(content.getServer())));
        }
        if (content.getDimension() != null) {
            message = message.replace("{dimension}", content.getDimension())
                    .replace("{plainDimension}", this.getPlainString(content.getDimension()));
        }
        if (content.getServerFrom() != null) {
            message = message.replace("{serverFrom}", this.config.getServername(content.getServerFrom()))
                    .replace("{plainServerFrom}", this.getPlainString(this.config.getServername(content.getServerFrom())));
        }
        if (content.getServerTo() != null) {
            message = message.replace("{serverTo}", this.config.getServername(content.getServerTo()))
                    .replace("{plainServerTo}", this.getPlainString(this.config.getServername(content.getServerTo())));
        }
        if (content.getName() != null) {
            message = message.replace("{name}", content.getName());
        }
        if (content.getSender() != null) {
            message = message.replace("{sender}", content.getSender());
        }
        if (content.getTarget() != null) {
            message = message.replace("{target}", content.getTarget());
        }
        if (content.getMessage() != null) {
            message = message.replace("{message}", content.getMessage());
        }
        if (content.getCount() != null) {
            message = message.replace("{count}", content.getCount().toString());
        }
        if (content.getPlayerList() != null) {
            message = message.replace("{playerList}", String.join(", ", content.getPlayerList()));
        }
        return message;
    }

    @Override
    public String formatUserChat(String server, String dimension, String name, String message) {
        return this.replaceAll(this.config.getMessage(this.platform, MessageType.CHAT),
                FormattingContent.builder().server(server).dimension(dimension).name(name).message(message).build());
    }

    @Override
    public String formatJoinServer(String server, String name) {
        return this.replaceAll(this.config.getMessage(this.platform, MessageType.JOIN),
                FormattingContent.builder().server(server).name(name).build());
    }

    @Override
    public String formatLeaveServer(String name) {
        return this.replaceAll(this.config.getMessage(this.platform, MessageType.LEAVE),
                FormattingContent.builder().name(name).build());
    }

    @Override
    public String formatMsgSender(String target, String message) {
        return this.replaceAll(this.config.getMessage(this.platform, MessageType.MSG_SENDER),
                FormattingContent.builder().target(target).message(message).build());
    }

    @Override
    public String formatMsgTarget(String sender, String message) {
        return this.replaceAll(this.config.getMessage(this.platform, MessageType.MSG_TARGET),
                FormattingContent.builder().sender(sender).message(message).build());
    }

    @Override
    public String formatList(String server, int count, List<String> playerList) {
        return this.replaceAll(this.config.getMessage(this.platform, MessageType.LIST),
                FormattingContent.builder().server(server).count(count).playerList(playerList).build());
    }

    @Override
    public String formatListEmpty() {
        return this.config.getMessage(this.platform, MessageType.LIST_EMPTY);
    }

    @Override
    public String formatListAll(IPlayerManager playerManager) {
        if (playerManager == null) {
            return this.formatListEmpty();
        }
        List<String> list = new ArrayList<>();
        String server = playerManager.getServerName();
        List<String> players = playerManager.getOnlinePlayerNames();
        if (!players.isEmpty()) {
            list.add(this.formatList(server, players.size(), players));
        }
        return list.isEmpty() ? this.formatListEmpty() : String.join("\n", list);
    }
}