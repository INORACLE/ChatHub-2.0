package com.zhanganzhi.chathub.platforms.qq;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.zhanganzhi.chathub.ChatHubMod;
import com.zhanganzhi.chathub.core.IPlayerManager;
import com.zhanganzhi.chathub.core.adaptor.AbstractAdaptor;
import com.zhanganzhi.chathub.core.binding.BindingManagerV2;
import com.zhanganzhi.chathub.core.config.Config;
import com.zhanganzhi.chathub.core.events.MessageEvent;
import com.zhanganzhi.chathub.platforms.Platform;
import com.zhanganzhi.chathub.platforms.qq.dto.QQBotEvent;
import com.zhanganzhi.chathub.platforms.qq.dto.Sender;
import com.zhanganzhi.chathub.platforms.qq.protocol.QQBotAPI;

import java.util.ArrayList;
import java.util.List;

/**
 * QQ official bot API adapter. Receives events polled from
 * {@link QQBotAPI QQBotAPI} and forwards in-game chat to the group.
 */
public class QQBotAdapter extends AbstractAdaptor<QQBotFormatter> {

    private final QQBotAPI qqBotAPI;
    private final Thread eventListener;
    private final BindingManagerV2 bindingManager;
    private volatile boolean listenerStopFlag = false;

    public QQBotAdapter(ChatHubMod chatHub, QQBotFormatter formatter, QQBotAPI qqBotAPI) {
        super(chatHub, Platform.QQ, formatter);
        this.qqBotAPI = qqBotAPI;
        this.eventListener = new Thread(this::eventListener, "chathub-qqbot-event-listener");
        this.eventListener.setDaemon(true);
        this.bindingManager = chatHub.getEventHub().getBindingManager();
    }

    @Override
    public void start() {
        try {
            this.bindingManager.loadBindings();
            this.qqBotAPI.start();
            this.eventListener.start();
        } catch (Exception e) {
            this.chatHub.getLogger().error("Failed to start QQ Bot adapter", e);
            throw new RuntimeException("Failed to start QQ Bot adapter", e);
        }
    }

    @Override
    public void stop() {
        try {
            this.listenerStopFlag = true;
            if (this.eventListener != null) {
                this.eventListener.interrupt();
            }
            this.qqBotAPI.stop();
        } catch (Exception e) {
            this.chatHub.getLogger().error("Error stopping QQ Bot adapter", e);
        }
    }

    @Override
    public void sendPublicMessage(String message) {
        if (!this.config.isQQSendToQQ()) {
            return;
        }
        if (message == null || message.isEmpty()) {
            return;
        }
        this.chatHub.getExecutorService().submit(() -> {
            try {
                this.qqBotAPI.sendMessage(message, this.config.getQQGroupId());
            } catch (Exception e) {
                this.chatHub.getLogger().error("Failed to send message to QQ group via QQ Bot API", e);
            }
        });
    }

    public void sendListMessage() {
        this.chatHub.getExecutorService().submit(() -> {
            try {
                String fullListMessage = this.getFormatter().formatListAll(this.chatHub.getPlayerManager());
                if (fullListMessage == null || fullListMessage.trim().isEmpty()) {
                    this.sendSimpleMessage("无法获取玩家列表信息");
                    return;
                }
                this.qqBotAPI.sendForwardMessage(new String[][]{{this.config.getQQRobotUserId(), this.config.getQQRobotNickname(), fullListMessage}},
                        this.config.getQQGroupId());
            } catch (Exception e) {
                this.chatHub.getLogger().error("发送玩家列表时发生错误", e);
                this.sendSimpleMessage("发送玩家列表时发生错误: " + e.getMessage());
            }
        });
    }

    private void sendSimpleMessage(String content) {
        this.qqBotAPI.sendMessage(content, this.config.getQQGroupId());
    }

    public void eventListener() {
        while (!this.listenerStopFlag) {
            this.consumeEvent();
            try {
                Thread.sleep(2000L);
            } catch (InterruptedException e) {
                if (this.listenerStopFlag) {
                    this.consumeEvent();
                    break;
                }
                Thread.currentThread().interrupt();
                break;
            }
        }
    }

    public void consumeEvent() {
        QQBotEvent currentEvent;
        while ((currentEvent = this.qqBotAPI.getQqBotEventQueue().poll()) != null) {
            if (!this.isValidQQGroupMessage(currentEvent)) {
                continue;
            }
            this.handleQQBotMessage(currentEvent);
        }
    }

    private boolean isValidQQGroupMessage(QQBotEvent event) {
        if (event == null) {
            return false;
        }
        if (!"message".equals(event.getPostType())) {
            return false;
        }
        if (!"group".equals(event.getMessageType())) {
            return false;
        }
        if (this.config.getQQGroupId() == null) {
            return false;
        }
        String eventGroupId = event.getGroupId() != null ? event.getGroupId().toString() : "null";
        return this.config.getQQGroupId().equals(eventGroupId);
    }

    private void handleQQBotMessage(QQBotEvent event) {
        JsonArray message = event.getMessage();
        if (this.isListCommand(message)) {
            this.sendListMessage();
            return;
        }
        if (this.isBindCommand(message)) {
            boolean isUnbind;
            String[] params = this.getBindCommandParams(message);
            String senderQqId = event.getSender() != null ? event.getSender().getUserId().toString() : null;
            Long messageId = event.getMessageId();
            if (senderQqId == null) {
                this.chatHub.getLogger().warn("[QQBot] Cannot get user ID from event");
                return;
            }
            String rawText = this.parseQQBotMessageContent(message).trim().toLowerCase();
            isUnbind = rawText.startsWith("解绑") || rawText.startsWith("unbind");
            if (isUnbind) {
                if (params == null || params[0] == null) {
                    this.qqBotAPI.sendMessage("❌ 使用格式：\n解绑 <QQ号> - 解绑该用户的所有账号\n解绑 <QQ号> <游戏名> - 解绑指定账号", event.getGroupId().toString());
                    return;
                }
                this.handleBindCommand(senderQqId, params[0], params[1], event.getGroupId(), isUnbind, messageId);
            } else {
                if (params == null || params[0] == null) {
                    this.qqBotAPI.sendMessage("❌ 使用格式：\n绑定 验证码 或 绑定验证码", event.getGroupId().toString());
                    return;
                }
                this.handleBindCommand(senderQqId, null, params[0], event.getGroupId(), isUnbind, messageId);
            }
            return;
        }
        if (!this.config.isQQSendToMinecraft()) {
            return;
        }
        String content = this.parseQQBotMessageContent(message);
        String senderName = this.getQQBotSenderName(event.getSender());
        this.chatHub.getEventHub().onUserChat(new MessageEvent(this.platform, null, null, senderName, content));
    }

    private boolean isListCommand(JsonArray message) {
        if (message == null || message.isEmpty()) {
            return false;
        }
        try {
            if (message.size() != 1) {
                return false;
            }
            JsonObject firstPart = JsonUtil.getObject(message, 0);
            if (firstPart == null || !"text".equals(JsonUtil.getString(firstPart, "type"))) {
                return false;
            }
            JsonObject data = JsonUtil.getJsonObject(firstPart, "data");
            if (data == null) {
                return false;
            }
            String text = JsonUtil.getString(data, "text");
            if (text == null) {
                return false;
            }
            String trimmedText = text.trim();
            return "/在线".equals(trimmedText) || "/list".equalsIgnoreCase(trimmedText);
        } catch (Exception e) {
            this.chatHub.getLogger().warn("[QQBot] Error checking list command: {}", e.getMessage());
            return false;
        }
    }

    private String parseQQBotMessageContent(JsonArray message) {
        if (message == null) {
            return "";
        }
        ArrayList<String> messages = new ArrayList<>();
        for (int i = 0; i < message.size(); ++i) {
            String partContent;
            JsonObject part = JsonUtil.getObject(message, i);
            if (part == null || (partContent = this.parseSingleMessagePart(part)) == null) {
                continue;
            }
            messages.add(partContent);
        }
        return String.join(" ", messages);
    }

    private String parseSingleMessagePart(JsonObject part) {
        String type = JsonUtil.getString(part, "type");
        if (type == null) {
            return null;
        }
        return switch (type) {
            case "text" -> {
                JsonObject data = JsonUtil.getJsonObject(part, "data");
                yield data != null ? JsonUtil.getString(data, "text") : null;
            }
            case "image", "face" -> null;
            default -> null;
        };
    }

    public String getQQBotSenderName(Sender sender) {
        if (sender == null) {
            return "Unknown";
        }
        String card = sender.getCard();
        return card != null && !card.isEmpty() ? card : sender.getNickname();
    }

    public BindingManagerV2 getBindingManager() {
        return this.bindingManager;
    }

    private boolean isBindCommand(JsonArray message) {
        if (message == null || message.isEmpty()) {
            return false;
        }
        try {
            if (message.size() != 1) {
                return false;
            }
            JsonObject firstPart = JsonUtil.getObject(message, 0);
            if (firstPart == null || !"text".equals(JsonUtil.getString(firstPart, "type"))) {
                return false;
            }
            JsonObject data = JsonUtil.getJsonObject(firstPart, "data");
            if (data == null) {
                return false;
            }
            String text = JsonUtil.getString(data, "text");
            if (text == null) {
                return false;
            }
            String trimmedText = text.trim().toLowerCase();
            return trimmedText.startsWith("绑定") || trimmedText.startsWith("bind") || trimmedText.startsWith("解绑") || trimmedText.startsWith("unbind");
        } catch (Exception e) {
            return false;
        }
    }

    private String[] getBindCommandParams(JsonArray message) {
        if (message == null || message.isEmpty()) {
            return null;
        }
        try {
            String codeOrQqId = null;
            String extraParam = null;
            for (int i = 0; i < message.size(); ++i) {
                JsonObject part = JsonUtil.getObject(message, i);
                if (part == null) {
                    continue;
                }
                String type = JsonUtil.getString(part, "type");
                if ("at".equals(type)) {
                    JsonObject data = JsonUtil.getJsonObject(part, "data");
                    String qq = data != null ? JsonUtil.getString(data, "qq") : null;
                    if (qq == null || "all".equals(qq)) {
                        continue;
                    }
                    codeOrQqId = qq;
                    continue;
                }
                if (!"text".equals(type)) {
                    continue;
                }
                JsonObject data = JsonUtil.getJsonObject(part, "data");
                String text = data != null ? JsonUtil.getString(data, "text") : null;
                if (text == null) {
                    continue;
                }
                String trimmedText = text.trim();
                if (trimmedText.isEmpty()) {
                    continue;
                }
                String cleanedText = trimmedText.replaceFirst("^(绑定|bind|解绑|unbind)#?\\s*", "").trim();
                if (cleanedText.isEmpty()) {
                    continue;
                }
                if (codeOrQqId != null) {
                    extraParam = cleanedText;
                } else {
                    codeOrQqId = cleanedText;
                }
            }
            if (codeOrQqId == null) {
                JsonObject firstPart = JsonUtil.getObject(message, 0);
                if (firstPart != null && "text".equals(JsonUtil.getString(firstPart, "type"))) {
                    JsonObject data = JsonUtil.getJsonObject(firstPart, "data");
                    String text = data != null ? JsonUtil.getString(data, "text") : null;
                    if (text != null) {
                        String trimmedText = text.trim();
                        String cleanedText = trimmedText.replaceFirst("^(绑定|bind|解绑|unbind)#?\\s*", "").trim();
                        if (!cleanedText.isEmpty()) {
                            String command = trimmedText.split("[#\\s]+")[0].toLowerCase();
                            if (command.equals("绑定") || command.equals("bind")) {
                                return new String[]{cleanedText, null};
                            }
                            if (command.equals("解绑") || command.equals("unbind")) {
                                if (cleanedText.matches("\\d+")) {
                                    return new String[]{cleanedText, "全部"};
                                }
                                return new String[]{null, cleanedText};
                            }
                        }
                    }
                }
                return null;
            }
            JsonObject firstPart = JsonUtil.getObject(message, 0);
            if (firstPart != null && "text".equals(JsonUtil.getString(firstPart, "type"))) {
                JsonObject data = JsonUtil.getJsonObject(firstPart, "data");
                String text = data != null ? JsonUtil.getString(data, "text") : null;
                if (text != null) {
                    String trimmedText = text.trim().toLowerCase();
                    if (trimmedText.startsWith("绑定") || trimmedText.startsWith("bind")) {
                        if (codeOrQqId != null) {
                            return new String[]{codeOrQqId, null};
                        }
                    } else if ((trimmedText.startsWith("解绑") || trimmedText.startsWith("unbind")) && codeOrQqId != null) {
                        if (extraParam != null) {
                            return new String[]{codeOrQqId, extraParam};
                        }
                        return new String[]{codeOrQqId, "全部"};
                    }
                }
            }
            return null;
        } catch (Exception e) {
            this.chatHub.getLogger().error("[QQBot] Failed to parse command params", e);
            return null;
        }
    }

    private void handleBindCommand(String senderQqId, String targetQqId, String codeOrGameName, Long groupId, boolean isUnbind, Long messageId) {
        if (isUnbind) {
            if (!Config.getInstance().isQQAdmin(senderQqId)) {
                this.qqBotAPI.sendMessage("❌ 你没有权限执行解绑操作，只有管理员可以解绑账号。", groupId.toString());
                return;
            }
            if (targetQqId == null) {
                this.qqBotAPI.sendMessage("❌ 解绑格式错误，请使用：解绑 @用户 或 解绑 @用户 游戏名", groupId.toString());
                return;
            }
            String unbindTargetQqId = targetQqId;
            if ("全部".equals(codeOrGameName) || "all".equalsIgnoreCase(codeOrGameName)) {
                List<String> boundNames = this.bindingManager.getMcNamesByQQ(unbindTargetQqId);
                boolean success = this.bindingManager.adminUnbindAll(unbindTargetQqId);
                if (success) {
                    this.qqBotAPI.sendMessage(String.format("✅ 已解绑 QQ: %s 的所有账号！", unbindTargetQqId), groupId.toString());
                    for (String mcName : boundNames) {
                        this.kickPlayerByName(mcName, "您的账号已被管理员解绑。");
                    }
                } else {
                    this.qqBotAPI.sendMessage("❌ 该用户未绑定任何账号。", groupId.toString());
                }
            } else {
                String existingQqId = this.bindingManager.getQQByMcName(codeOrGameName);
                if (existingQqId != null && existingQqId.equals(unbindTargetQqId)) {
                    boolean success = this.bindingManager.adminUnbind(unbindTargetQqId, codeOrGameName);
                    if (success) {
                        this.qqBotAPI.sendMessage(String.format("✅ 已解绑 QQ: %s 的游戏名: %s", unbindTargetQqId, codeOrGameName), groupId.toString());
                        this.kickPlayerByName(codeOrGameName, "您的账号已被管理员解绑。");
                    } else {
                        this.qqBotAPI.sendMessage("❌ 该用户未绑定此游戏名或解绑失败。", groupId.toString());
                    }
                } else {
                    this.qqBotAPI.sendMessage("❌ 该用户未绑定此游戏名。", groupId.toString());
                }
            }
        } else {
            if (codeOrGameName == null || codeOrGameName.isEmpty()) {
                this.qqBotAPI.sendMessage("❌ 使用格式：\n绑定 验证码 或 绑定验证码", groupId.toString());
                return;
            }
            String code = codeOrGameName.trim().replaceFirst("^#?\\s*", "").trim();
            String gameName = this.bindingManager.verifyCode(code);
            if (gameName == null) {
                this.qqBotAPI.sendMessage("❌ 验证码无效或已过期，请重新进入游戏获取新的验证码。", groupId.toString());
                return;
            }
            String existingQqId = this.bindingManager.getQQByCode(code);
            if (existingQqId != null && !existingQqId.equals(senderQqId)) {
                this.qqBotAPI.sendMessage("❌ 该验证码已被其他用户使用。", groupId.toString());
                return;
            }
            this.bindingManager.setQQForCode(code, senderQqId);
            int result = this.bindingManager.bindOnce(senderQqId, gameName);
            switch (result) {
                case 0:
                    this.bindingManager.clearVerificationCode(code);
                    if (messageId != null) {
                        this.qqBotAPI.sendReplyMessage(String.format("绑定%s成功! 你可以进入服务器了!", gameName), groupId.toString(), messageId);
                    } else {
                        this.qqBotAPI.sendMessage(String.format("绑定%s成功! 你可以进入服务器了!", gameName), groupId.toString());
                    }
                    break;
                case 1:
                    this.qqBotAPI.sendMessage("❌ 你已经绑定过游戏账号了，每人只能绑定一个账号。", groupId.toString());
                    break;
                case 2:
                    this.qqBotAPI.sendMessage(String.format("❌ 游戏名 '%s' 已被其他玩家绑定！", gameName), groupId.toString());
                    break;
                default:
                    this.qqBotAPI.sendMessage("❌ 绑定失败，请重试。", groupId.toString());
            }
        }
    }

    private void kickPlayerByName(String playerName, String reason) {
        try {
            IPlayerManager playerManager = this.chatHub.getPlayerManager();
            if (playerManager != null) {
                playerManager.kickPlayer(playerName, reason);
                this.chatHub.getLogger().info("[Binding] Kicked player {} after unbind: {}", playerName, reason);
            }
        } catch (Exception e) {
            this.chatHub.getLogger().error("[Binding] Failed to kick player {}: {}", playerName, e);
        }
    }
}