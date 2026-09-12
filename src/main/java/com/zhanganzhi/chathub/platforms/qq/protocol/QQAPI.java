package com.zhanganzhi.chathub.platforms.qq.protocol;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.zhanganzhi.chathub.ChatHubMod;
import com.zhanganzhi.chathub.core.config.Config;
import com.zhanganzhi.chathub.platforms.qq.dto.QQEvent;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class QQAPI {

    private static final Pattern FACE_PATTERN = Pattern.compile("\\[face:(\\d+)\\]");
    private static final Pattern IMAGE_PATTERN = Pattern.compile("\\[image:(.*?)\\]");

    private final Config config = Config.getInstance();
    private final Queue<QQEvent> qqEventQueue = new ConcurrentLinkedDeque<>();
    private final QQWsServer wsServer;
    private final String robotUserId;
    private final String robotNickname;

    public QQAPI(ChatHubMod chatHub) {
        this.wsServer = new QQWsServer(this.config.getQQHost(),
                this.config.getQQWsReversePort().intValue(),
                this.config.getQQWsReversePath(),
                this.qqEventQueue);
        this.wsServer.setLogger(chatHub.getLogger());
        this.robotUserId = this.config.getQQRobotUserId() != null ? this.config.getQQRobotUserId() : "379450326";
        this.robotNickname = this.config.getQQRobotNickname() != null ? this.config.getQQRobotNickname() : "ChatBot";
    }

    public Queue<QQEvent> getQqEventQueue() {
        return this.qqEventQueue;
    }

    public QQWsServer getWsServer() {
        return this.wsServer;
    }

    public String getRobotUserId() {
        return this.robotUserId;
    }

    public String getRobotNickname() {
        return this.robotNickname;
    }

    public void start() {
        this.wsServer.start();
    }

    public void stop() {
        try {
            this.wsServer.stop();
        } catch (Exception e) {
            // ignore on shutdown
        }
    }

    public void sendMessage(String message, String targetId) {
        this.wsServer.sendMessage(this.genSendReq(message, targetId));
    }

    public void sendReplyMessage(String message, String targetId, Long messageId) {
        this.wsServer.sendMessage(this.genReplySendReq(message, targetId, messageId));
    }

    public void sendForwardMessage(String[][] messageParts, String targetId) {
        this.wsServer.sendMessage(this.genForwardSendReq(messageParts, targetId));
    }

    private String genSendReq(String message, String targetId) {
        JsonObject req = new JsonObject();
        req.addProperty("action", "send_group_msg");
        JsonObject params = new JsonObject();
        params.addProperty("group_id", targetId);
        JsonArray parsedMessage = this.parseMessageContent(message);
        params.add("message", parsedMessage);
        req.add("params", params);
        return req.toString();
    }

    private String genReplySendReq(String message, String targetId, Long messageId) {
        JsonObject req = new JsonObject();
        req.addProperty("action", "send_group_msg");
        JsonObject params = new JsonObject();
        params.addProperty("group_id", targetId);
        JsonArray messageArray = new JsonArray();
        JsonObject replyElement = new JsonObject();
        replyElement.addProperty("type", "reply");
        JsonObject replyData = new JsonObject();
        replyData.addProperty("id", messageId);
        replyElement.add("data", replyData);
        messageArray.add(replyElement);
        messageArray.addAll(this.parseMessageContent(message));
        params.add("message", messageArray);
        req.add("params", params);
        return req.toString();
    }

    private String genForwardSendReq(String[][] messageParts, String targetId) {
        JsonObject req = new JsonObject();
        req.addProperty("action", "send_group_forward_msg");
        JsonObject params = new JsonObject();
        params.addProperty("group_id", Long.parseLong(targetId));
        JsonArray messagesArray = new JsonArray();
        for (String[] part : messageParts) {
            JsonObject node = new JsonObject();
            node.addProperty("type", "node");
            JsonObject data = new JsonObject();
            data.addProperty("uin", part[0]);
            data.addProperty("name", part[1]);
            JsonArray content = new JsonArray();
            JsonObject textMsg = new JsonObject();
            textMsg.addProperty("type", "text");
            JsonObject textData = new JsonObject();
            textData.addProperty("text", part[2]);
            textMsg.add("data", textData);
            content.add(textMsg);
            data.add("content", content);
            node.add("data", data);
            messagesArray.add(node);
        }
        params.add("messages", messagesArray);
        req.add("params", params);
        return req.toString();
    }

    private JsonArray parseMessageContent(String message) {
        JsonArray contentArray = new JsonArray();
        int lastIndex = 0;
        Matcher faceMatcher = FACE_PATTERN.matcher(message);
        Matcher imageMatcher = IMAGE_PATTERN.matcher(message);
        ArrayList<MatchResult> allMatches = new ArrayList<>();
        while (faceMatcher.find()) {
            allMatches.add(new MatchResult(faceMatcher.start(), faceMatcher.end(), "face", faceMatcher.group(1)));
        }
        while (imageMatcher.find()) {
            allMatches.add(new MatchResult(imageMatcher.start(), imageMatcher.end(), "image", imageMatcher.group(1)));
        }
        allMatches.sort(Comparator.comparingInt(m -> m.start));
        for (MatchResult match : allMatches) {
            if (match.start > lastIndex) {
                String textBefore = message.substring(lastIndex, match.start);
                if (!textBefore.isEmpty()) {
                    contentArray.add(this.createTextElement(textBefore));
                }
            }
            if ("face".equals(match.type)) {
                contentArray.add(this.createFaceElement(match.value));
            } else if ("image".equals(match.type)) {
                contentArray.add(this.createImageElement(match.value));
            }
            lastIndex = match.end;
        }
        if (lastIndex < message.length()) {
            String remainingText = message.substring(lastIndex);
            if (!remainingText.isEmpty()) {
                contentArray.add(this.createTextElement(remainingText));
            }
        }
        if (contentArray.isEmpty()) {
            contentArray.add(this.createTextElement(message));
        }
        return contentArray;
    }

    private JsonObject createTextElement(String text) {
        JsonObject element = new JsonObject();
        element.addProperty("type", "text");
        JsonObject data = new JsonObject();
        data.addProperty("text", text);
        element.add("data", data);
        return element;
    }

    private JsonObject createFaceElement(String id) {
        JsonObject element = new JsonObject();
        element.addProperty("type", "face");
        JsonObject data = new JsonObject();
        data.addProperty("id", id);
        element.add("data", data);
        return element;
    }

    private JsonObject createImageElement(String file) {
        JsonObject element = new JsonObject();
        element.addProperty("type", "image");
        JsonObject data = new JsonObject();
        data.addProperty("file", file);
        element.add("data", data);
        return element;
    }

    private static class MatchResult {
        final int start;
        final int end;
        final String type;
        final String value;

        MatchResult(int start, int end, String type, String value) {
            this.start = start;
            this.end = end;
            this.type = type;
            this.value = value;
        }
    }
}