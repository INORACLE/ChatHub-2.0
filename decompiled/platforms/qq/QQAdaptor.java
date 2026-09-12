/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.velocitypowered.api.proxy.Player
 *  net.kyori.adventure.text.Component
 *  net.kyori.adventure.text.TextComponent
 */
package com.zhanganzhi.chathub.platforms.qq;

import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import com.velocitypowered.api.proxy.Player;
import com.zhanganzhi.chathub.ChatHub;
import com.zhanganzhi.chathub.core.adaptor.AbstractAdaptor;
import com.zhanganzhi.chathub.core.binding.BindingManagerV2;
import com.zhanganzhi.chathub.core.config.Config;
import com.zhanganzhi.chathub.core.events.MessageEvent;
import com.zhanganzhi.chathub.platforms.Platform;
import com.zhanganzhi.chathub.platforms.qq.QQFormatter;
import com.zhanganzhi.chathub.platforms.qq.dto.QQEvent;
import com.zhanganzhi.chathub.platforms.qq.dto.Sender;
import com.zhanganzhi.chathub.platforms.qq.protocol.QQAPI;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Optional;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;

public class QQAdaptor
extends AbstractAdaptor<QQFormatter> {
    private final QQAPI qqAPI;
    private final Thread eventListener;
    private volatile boolean listenerStopFlag = false;
    private final BindingManagerV2 bindingManager;

    public QQAdaptor(ChatHub chatHub) {
        super(chatHub, Platform.QQ, new QQFormatter());
        this.qqAPI = new QQAPI(chatHub);
        this.eventListener = new Thread(this::eventListener, "chathub-qq-event-listener");
        this.bindingManager = new BindingManagerV2(chatHub.getDataDirectory());
        this.bindingManager.setLogger(chatHub.getLogger());
    }

    @Override
    public void start() {
        try {
            this.bindingManager.loadBindings();
            this.qqAPI.start();
            this.eventListener.start();
        }
        catch (Exception e) {
            this.chatHub.getLogger().error("Failed to start QQ integration", e);
            throw new RuntimeException("Failed to start QQ integration", e);
        }
    }

    @Override
    public void stop() {
        try {
            this.listenerStopFlag = true;
            if (this.eventListener != null) {
                this.eventListener.interrupt();
            }
            this.qqAPI.stop();
        }
        catch (Exception e) {
            this.chatHub.getLogger().error("Error stopping QQ integration", e);
            throw new RuntimeException("Error stopping QQ integration", e);
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
        this.chatHub.getThreadPoolExecutor().submit(() -> {
            try {
                this.qqAPI.sendMessage(message, this.config.getQQGroupId());
            }
            catch (Exception e) {
                this.chatHub.getLogger().error("Failed to send message to QQ group", e);
            }
        });
    }

    public void sendListMessage() {
        this.chatHub.getThreadPoolExecutor().submit(() -> {
            try {
                String fullListMessage = ((QQFormatter)this.getFormatter()).formatListAll(this.chatHub.getProxyServer());
                if (fullListMessage == null || fullListMessage.trim().isEmpty()) {
                    this.sendSimpleMessage("\u65e0\u6cd5\u83b7\u53d6\u73a9\u5bb6\u5217\u8868\u4fe1\u606f");
                    return;
                }
                String[] lines = fullListMessage.split("\n");
                ArrayList<String[]> messageParts = new ArrayList<String[]>();
                StringBuilder firstMessage = new StringBuilder();
                ArrayList<Integer> detailIndices = new ArrayList<Integer>();
                for (int i = 0; i < lines.length; ++i) {
                    if (lines[i] == null || lines[i].trim().isEmpty()) continue;
                    if (lines[i].contains("\u3011\uff1a") && !lines[i].contains("\u5728\u7ebf\u4eba\u6570\uff1a") && !lines[i].startsWith("\u5728\u7ebf\u73a9\u5bb6\u603b\u4eba\u6570\uff1a")) {
                        if (!firstMessage.isEmpty()) {
                            firstMessage.append("\n");
                        }
                        firstMessage.append(lines[i]);
                        continue;
                    }
                    if (lines[i].startsWith("\u5728\u7ebf\u73a9\u5bb6\u603b\u4eba\u6570\uff1a")) {
                        if (!firstMessage.isEmpty()) {
                            firstMessage.append("\n");
                        }
                        firstMessage.append(lines[i]);
                        continue;
                    }
                    if (!lines[i].contains("\u3011\u5728\u7ebf\u4eba\u6570\uff1a")) continue;
                    detailIndices.add(i);
                }
                if (!firstMessage.isEmpty()) {
                    messageParts.add(this.createMessageNode(firstMessage.toString()));
                }
                Iterator i = detailIndices.iterator();
                while (i.hasNext()) {
                    int detailIndex = (Integer)i.next();
                    StringBuilder serverMessage = new StringBuilder();
                    serverMessage.append(lines[detailIndex]);
                    if (detailIndex + 1 < lines.length && lines[detailIndex + 1] != null && !lines[detailIndex + 1].contains("\u3011\u5728\u7ebf\u4eba\u6570\uff1a") && !lines[detailIndex + 1].contains("\u3011\uff1a")) {
                        serverMessage.append("\n").append(lines[detailIndex + 1]);
                    }
                    messageParts.add(this.createMessageNode(serverMessage.toString()));
                }
                if (messageParts.isEmpty()) {
                    this.sendSimpleMessage("\u6ca1\u6709\u6709\u6548\u7684\u73a9\u5bb6\u5217\u8868\u4fe1\u606f");
                } else {
                    String[][] messagePartsArray = (String[][])messageParts.toArray((T[])new String[0][]);
                    this.qqAPI.sendForwardMessage(messagePartsArray, this.config.getQQGroupId());
                }
            }
            catch (Exception e) {
                this.chatHub.getLogger().error("\u53d1\u9001\u73a9\u5bb6\u5217\u8868\u65f6\u53d1\u751f\u9519\u8bef", e);
                this.sendSimpleMessage("\u53d1\u9001\u73a9\u5bb6\u5217\u8868\u65f6\u53d1\u751f\u9519\u8bef: " + e.getMessage());
            }
        });
    }

    private String[] createMessageNode(String content) {
        String[] messagePart = new String[]{this.config.getQQRobotUserId(), this.config.getQQRobotNickname(), content};
        return messagePart;
    }

    private void sendSimpleMessage(String content) {
        this.qqAPI.sendMessage(content, this.config.getQQGroupId());
    }

    public void eventListener() {
        while (!this.listenerStopFlag) {
            this.consumeEvent();
            try {
                Thread.sleep(2000L);
            }
            catch (InterruptedException e) {
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
        QQEvent currentEvent;
        while ((currentEvent = this.qqAPI.getQqEventQueue().poll()) != null) {
            if ("notice".equals(currentEvent.getPostType())) {
                this.handleNoticeEvent(currentEvent);
                continue;
            }
            if (!this.isValidQQGroupMessage(currentEvent)) continue;
            this.handleQQMessage(currentEvent);
        }
    }

    private void handleNoticeEvent(QQEvent event) {
        try {
            String qqId;
            String noticeType = event.getNoticeType();
            String subType = event.getSubType();
            Long userId = event.getUserId();
            Long groupId = event.getGroupId();
            if ("group_decrease".equals(noticeType) && "leave".equals(subType) && userId != null && this.bindingManager.isQQBound(qqId = userId.toString())) {
                List<String> boundNames = this.bindingManager.getMcNamesByQQ(qqId);
                this.bindingManager.adminUnbindAll(qqId);
                for (String mcName : boundNames) {
                    this.kickPlayerByName(mcName, "\u68c0\u6d4b\u5230\u60a8\u5df2\u9000\u51faQQ\u7fa4\uff0c\u8d26\u53f7\u7ed1\u5b9a\u5df2\u81ea\u52a8\u89e3\u9664\u3002");
                }
                this.chatHub.getLogger().info("[Binding] Auto-unbound QQ {} due to group leave", (Object)qqId);
            }
        }
        catch (Exception e) {
            this.chatHub.getLogger().error("[Binding] Failed to handle notice event", e);
        }
    }

    private void kickPlayerByName(String playerName, String reason) {
        try {
            Optional playerOpt = this.chatHub.getProxyServer().getPlayer(playerName);
            if (playerOpt.isPresent()) {
                Player player = (Player)playerOpt.get();
                TextComponent kickComponent = Component.text((String)reason);
                player.disconnect((Component)kickComponent);
                this.chatHub.getLogger().info("[Binding] Kicked player {} after unbind: {}", (Object)playerName, (Object)reason);
            }
        }
        catch (Exception e) {
            this.chatHub.getLogger().error("[Binding] Failed to kick player {}", (Object)playerName, (Object)e);
        }
    }

    private boolean isValidQQGroupMessage(QQEvent event) {
        String eventGroupId;
        if (event == null) {
            return false;
        }
        if (!"message".equals(event.getPostType())) {
            return false;
        }
        if (!"group".equals(event.getMessageType())) {
            return false;
        }
        if (!"array".equals(event.getMessageFormat())) {
            return false;
        }
        if (this.config.getQQGroupId() == null) {
            return false;
        }
        String string = eventGroupId = event.getGroupId() != null ? event.getGroupId().toString() : "null";
        return this.config.getQQGroupId().equals(eventGroupId);
    }

    private void handleQQMessage(QQEvent event) {
        JSONArray message = event.getMessage();
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
                this.chatHub.getLogger().warn("[Binding] Cannot get user ID from event");
                return;
            }
            String rawText = this.parseQQMessageContent(message).trim().toLowerCase();
            boolean bl = isUnbind = rawText.startsWith("\u89e3\u7ed1") || rawText.startsWith("unbind");
            if (isUnbind) {
                if (params == null || params[0] == null) {
                    this.qqAPI.sendMessage("\u274c \u4f7f\u7528\u683c\u5f0f\uff1a\n\u89e3\u7ed1 <QQ\u53f7> - \u89e3\u7ed1\u8be5\u7528\u6237\u7684\u6240\u6709\u8d26\u53f7\n\u89e3\u7ed1 <QQ\u53f7> <\u6e38\u620f\u540d> - \u89e3\u7ed1\u6307\u5b9a\u8d26\u53f7", event.getGroupId().toString());
                    return;
                }
                this.handleBindCommand(senderQqId, params[0], params[1], event.getGroupId(), isUnbind, messageId);
            } else {
                if (params == null || params[0] == null) {
                    this.qqAPI.sendMessage("\u274c \u4f7f\u7528\u683c\u5f0f\uff1a\n\u7ed1\u5b9a \u9a8c\u8bc1\u7801 \u6216 \u7ed1\u5b9a\u9a8c\u8bc1\u7801", event.getGroupId().toString());
                    return;
                }
                this.handleBindCommand(senderQqId, null, params[0], event.getGroupId(), isUnbind, messageId);
            }
            return;
        }
        if (!this.config.isQQSendToMinecraft()) {
            return;
        }
        String content = this.parseQQMessageContent(message);
        String senderName = this.getQQSenderName(event.getSender());
        this.chatHub.getEventHub().onUserChat(new MessageEvent(this.platform, null, senderName, content));
    }

    private boolean isListCommand(JSONArray message) {
        if (message == null || message.isEmpty()) {
            return false;
        }
        try {
            if (message.size() != 1) {
                return false;
            }
            JSONObject firstPart = message.getJSONObject(0);
            if (firstPart == null || !"text".equals(firstPart.getString("type"))) {
                return false;
            }
            JSONObject data = firstPart.getJSONObject("data");
            if (data == null) {
                return false;
            }
            String text = data.getString("text");
            if (text == null) {
                return false;
            }
            String trimmedText = text.trim();
            return "/\u5728\u7ebf".equals(trimmedText) || "/list".equalsIgnoreCase(trimmedText);
        }
        catch (Exception e) {
            this.chatHub.getLogger().warn("[QQ] Error checking list command: {}", (Object)e.getMessage());
            return false;
        }
    }

    private String parseQQMessageContent(JSONArray message) {
        if (message == null) {
            return "";
        }
        ArrayList<String> messages = new ArrayList<String>();
        for (int i = 0; i < message.size(); ++i) {
            String partContent;
            JSONObject part = message.getJSONObject(i);
            if (part == null || (partContent = this.parseSingleMessagePart(part)) == null) continue;
            messages.add(partContent);
        }
        return String.join((CharSequence)" ", messages);
    }

    private String parseSingleMessagePart(JSONObject part) {
        String type = part.getString("type");
        if (type == null) {
            return null;
        }
        return switch (type) {
            case "text" -> part.getJSONObject("data").getString("text");
            case "image" -> null;
            case "face" -> null;
            default -> null;
        };
    }

    public String getQQSenderName(Sender sender) {
        if (sender == null) {
            return "Unknown";
        }
        String card = sender.getCard();
        return card != null && !card.isEmpty() ? card : sender.getNickname();
    }

    public BindingManagerV2 getBindingManager() {
        return this.bindingManager;
    }

    private boolean isBindCommand(JSONArray message) {
        if (message == null || message.isEmpty()) {
            return false;
        }
        try {
            if (message.size() != 1) {
                return false;
            }
            JSONObject firstPart = message.getJSONObject(0);
            if (firstPart == null || !"text".equals(firstPart.getString("type"))) {
                return false;
            }
            JSONObject data = firstPart.getJSONObject("data");
            if (data == null) {
                return false;
            }
            String text = data.getString("text");
            if (text == null) {
                return false;
            }
            String trimmedText = text.trim().toLowerCase();
            return trimmedText.startsWith("\u7ed1\u5b9a") || trimmedText.startsWith("bind") || trimmedText.startsWith("\u89e3\u7ed1") || trimmedText.startsWith("unbind");
        }
        catch (Exception e) {
            return false;
        }
    }

    private String[] getBindCommandParams(JSONArray message) {
        if (message == null || message.isEmpty()) {
            return null;
        }
        try {
            String trimmedText;
            String text;
            JSONObject data;
            String codeOrQqId = null;
            String extraParam = null;
            for (int i = 0; i < message.size(); ++i) {
                String cleanedText;
                String trimmedText2;
                String text2;
                JSONObject data2;
                String type;
                JSONObject part = message.getJSONObject(i);
                if (part == null || (type = part.getString("type")) == null) continue;
                if ("at".equals(type)) {
                    String qq;
                    data2 = part.getJSONObject("data");
                    if (data2 == null || (qq = data2.getString("qq")) == null || "all".equals(qq)) continue;
                    codeOrQqId = qq;
                    continue;
                }
                if (!"text".equals(type) || (data2 = part.getJSONObject("data")) == null || (text2 = data2.getString("text")) == null || (trimmedText2 = text2.trim()).isEmpty() || (cleanedText = trimmedText2.replaceFirst("^(\u7ed1\u5b9a|bind|\u89e3\u7ed1|unbind)#?\\s*", "").trim()).isEmpty()) continue;
                if (codeOrQqId != null) {
                    extraParam = cleanedText;
                    continue;
                }
                codeOrQqId = cleanedText;
            }
            if (codeOrQqId == null) {
                String cleanedText;
                JSONObject firstPart = message.getJSONObject(0);
                if (firstPart != null && "text".equals(firstPart.getString("type")) && (data = firstPart.getJSONObject("data")) != null && (text = data.getString("text")) != null && !(cleanedText = (trimmedText = text.trim()).replaceFirst("^(\u7ed1\u5b9a|bind|\u89e3\u7ed1|unbind)#?\\s*", "").trim()).isEmpty()) {
                    String command = trimmedText.split("[#\\s]+")[0].toLowerCase();
                    if (command.equals("\u7ed1\u5b9a") || command.equals("bind")) {
                        return new String[]{cleanedText, null};
                    }
                    if (command.equals("\u89e3\u7ed1") || command.equals("unbind")) {
                        if (cleanedText.matches("\\d+")) {
                            return new String[]{cleanedText, "\u5168\u90e8"};
                        }
                        return new String[]{null, cleanedText};
                    }
                }
                return null;
            }
            JSONObject firstPart = message.getJSONObject(0);
            if (firstPart != null && "text".equals(firstPart.getString("type")) && (data = firstPart.getJSONObject("data")) != null && (text = data.getString("text")) != null) {
                trimmedText = text.trim().toLowerCase();
                if (trimmedText.startsWith("\u7ed1\u5b9a") || trimmedText.startsWith("bind")) {
                    if (codeOrQqId != null) {
                        return new String[]{codeOrQqId, null};
                    }
                } else if ((trimmedText.startsWith("\u89e3\u7ed1") || trimmedText.startsWith("unbind")) && codeOrQqId != null) {
                    if (extraParam != null) {
                        return new String[]{codeOrQqId, extraParam};
                    }
                    return new String[]{codeOrQqId, "\u5168\u90e8"};
                }
            }
            return null;
        }
        catch (Exception e) {
            this.chatHub.getLogger().error("[Binding] Failed to parse command params", e);
            return null;
        }
    }

    private void handleBindCommand(String senderQqId, String targetQqId, String codeOrGameName, Long groupId, boolean isUnbind, Long messageId) {
        if (isUnbind) {
            if (!Config.getInstance().isQQAdmin(senderQqId)) {
                this.qqAPI.sendMessage("\u274c \u4f60\u6ca1\u6709\u6743\u9650\u6267\u884c\u89e3\u7ed1\u64cd\u4f5c\uff0c\u53ea\u6709\u7ba1\u7406\u5458\u53ef\u4ee5\u89e3\u7ed1\u8d26\u53f7\u3002", groupId.toString());
                return;
            }
            if (targetQqId == null) {
                this.qqAPI.sendMessage("\u274c \u89e3\u7ed1\u683c\u5f0f\u9519\u8bef\uff0c\u8bf7\u4f7f\u7528\uff1a\u89e3\u7ed1 @\u7528\u6237 \u6216 \u89e3\u7ed1 @\u7528\u6237 \u6e38\u620f\u540d", groupId.toString());
                return;
            }
            String unbindTargetQqId = targetQqId;
            if ("\u5168\u90e8".equals(codeOrGameName) || "all".equalsIgnoreCase(codeOrGameName)) {
                List<String> boundNames = this.bindingManager.getMcNamesByQQ(unbindTargetQqId);
                boolean success = this.bindingManager.adminUnbindAll(unbindTargetQqId);
                if (success) {
                    this.qqAPI.sendMessage(String.format("\u2705 \u5df2\u89e3\u7ed1 QQ: %s \u7684\u6240\u6709\u8d26\u53f7\uff01", unbindTargetQqId), groupId.toString());
                    for (String mcName : boundNames) {
                        this.kickPlayerByName(mcName, "\u60a8\u7684\u8d26\u53f7\u5df2\u88ab\u7ba1\u7406\u5458\u89e3\u7ed1\u3002");
                    }
                } else {
                    this.qqAPI.sendMessage("\u274c \u8be5\u7528\u6237\u672a\u7ed1\u5b9a\u4efb\u4f55\u8d26\u53f7\u3002", groupId.toString());
                }
            } else {
                String existingQqId = this.bindingManager.getQQByMcName(codeOrGameName);
                if (existingQqId != null && existingQqId.equals(unbindTargetQqId)) {
                    boolean success = this.bindingManager.adminUnbind(unbindTargetQqId, codeOrGameName);
                    if (success) {
                        this.qqAPI.sendMessage(String.format("\u2705 \u5df2\u89e3\u7ed1 QQ: %s \u7684\u6e38\u620f\u540d: %s", unbindTargetQqId, codeOrGameName), groupId.toString());
                        this.kickPlayerByName(codeOrGameName, "\u60a8\u7684\u8d26\u53f7\u5df2\u88ab\u7ba1\u7406\u5458\u89e3\u7ed1\u3002");
                    } else {
                        this.qqAPI.sendMessage("\u274c \u8be5\u7528\u6237\u672a\u7ed1\u5b9a\u6b64\u6e38\u620f\u540d\u6216\u89e3\u7ed1\u5931\u8d25\u3002", groupId.toString());
                    }
                } else {
                    this.qqAPI.sendMessage("\u274c \u8be5\u7528\u6237\u672a\u7ed1\u5b9a\u6b64\u6e38\u620f\u540d\u3002", groupId.toString());
                    boolean success = false;
                }
            }
        } else {
            if (codeOrGameName == null || codeOrGameName.isEmpty()) {
                this.qqAPI.sendMessage("\u274c \u4f7f\u7528\u683c\u5f0f\uff1a\n\u7ed1\u5b9a \u9a8c\u8bc1\u7801 \u6216 \u7ed1\u5b9a\u9a8c\u8bc1\u7801", groupId.toString());
                return;
            }
            String code = codeOrGameName.trim().replaceFirst("^#?\\s*", "").trim();
            String gameName = this.bindingManager.verifyCode(code);
            if (gameName == null) {
                this.qqAPI.sendMessage("\u274c \u9a8c\u8bc1\u7801\u65e0\u6548\u6216\u5df2\u8fc7\u671f\uff0c\u8bf7\u91cd\u65b0\u8fdb\u5165\u6e38\u620f\u83b7\u53d6\u65b0\u7684\u9a8c\u8bc1\u7801\u3002", groupId.toString());
                return;
            }
            String existingQqId = this.bindingManager.getQQByCode(code);
            if (existingQqId != null && !existingQqId.equals(senderQqId)) {
                this.qqAPI.sendMessage("\u274c \u8be5\u9a8c\u8bc1\u7801\u5df2\u88ab\u5176\u4ed6\u7528\u6237\u4f7f\u7528\u3002", groupId.toString());
                return;
            }
            this.bindingManager.setQQForCode(code, senderQqId);
            int result = this.bindingManager.bindOnce(senderQqId, gameName);
            switch (result) {
                case 0: {
                    this.bindingManager.clearVerificationCode(code);
                    if (messageId != null) {
                        this.qqAPI.sendReplyMessage(String.format("\u7ed1\u5b9a%s\u6210\u529f! \u4f60\u53ef\u4ee5\u8fdb\u5165\u670d\u52a1\u5668\u4e86!", gameName), groupId.toString(), messageId);
                        break;
                    }
                    this.qqAPI.sendMessage(String.format("\u7ed1\u5b9a%s\u6210\u529f! \u4f60\u53ef\u4ee5\u8fdb\u5165\u670d\u52a1\u5668\u4e86!", gameName), groupId.toString());
                    break;
                }
                case 1: {
                    this.qqAPI.sendMessage("\u274c \u4f60\u5df2\u7ecf\u7ed1\u5b9a\u8fc7\u6e38\u620f\u8d26\u53f7\u4e86\uff0c\u6bcf\u4eba\u53ea\u80fd\u7ed1\u5b9a\u4e00\u4e2a\u8d26\u53f7\u3002", groupId.toString());
                    break;
                }
                case 2: {
                    this.qqAPI.sendMessage(String.format("\u274c \u6e38\u620f\u540d '%s' \u5df2\u88ab\u5176\u4ed6\u73a9\u5bb6\u7ed1\u5b9a\uff01", gameName), groupId.toString());
                    break;
                }
                default: {
                    this.qqAPI.sendMessage("\u274c \u7ed1\u5b9a\u5931\u8d25\uff0c\u8bf7\u91cd\u8bd5\u3002", groupId.toString());
                }
            }
        }
    }
}

