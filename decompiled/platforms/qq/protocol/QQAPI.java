/*
 * Decompiled with CFR 0.152.
 */
package com.zhanganzhi.chathub.platforms.qq.protocol;

import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import com.alibaba.fastjson2.JSONWriter;
import com.zhanganzhi.chathub.ChatHub;
import com.zhanganzhi.chathub.core.config.Config;
import com.zhanganzhi.chathub.platforms.qq.dto.QQEvent;
import com.zhanganzhi.chathub.platforms.qq.protocol.QQWsServer;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class QQAPI {
    private final Config config = Config.getInstance();
    private final Queue<QQEvent> qqEventQueue = new ConcurrentLinkedDeque<QQEvent>();
    private final QQWsServer wsServer = new QQWsServer(this.config.getQQHost(), (Integer)this.config.getQQWsReversePort().intValue(), this.config.getQQWsReversePath(), this.qqEventQueue);
    private final String robotUserId;
    private final String robotNickname;
    private static final Pattern FACE_PATTERN = Pattern.compile("\\[face:(\\d+)\\]");
    private static final Pattern IMAGE_PATTERN = Pattern.compile("\\[image:(.*?)\\]");

    public QQAPI(ChatHub chatHub) {
        this.wsServer.setLogger(chatHub.getLogger());
        this.robotUserId = this.config.getQQRobotUserId() != null ? this.config.getQQRobotUserId() : "379450326";
        this.robotNickname = this.config.getQQRobotNickname() != null ? this.config.getQQRobotNickname() : "\u55b5\u55b5\u55b5";
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
        this.wsServer.stop();
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
        JSONObject req = new JSONObject();
        req.put("action", "send_group_msg");
        JSONObject params = new JSONObject();
        params.put("group_id", targetId);
        JSONArray parsedMessage = this.parseMessageContent(message);
        params.put("message", parsedMessage);
        req.put("params", params);
        return req.toJSONString(new JSONWriter.Feature[0]);
    }

    private String genReplySendReq(String message, String targetId, Long messageId) {
        JSONObject req = new JSONObject();
        req.put("action", "send_group_msg");
        JSONObject params = new JSONObject();
        params.put("group_id", targetId);
        JSONArray messageArray = new JSONArray();
        JSONObject replyElement = new JSONObject();
        replyElement.put("type", "reply");
        JSONObject replyData = new JSONObject();
        replyData.put("id", messageId);
        replyElement.put("data", replyData);
        messageArray.add(replyElement);
        JSONArray parsedMessage = this.parseMessageContent(message);
        messageArray.addAll(parsedMessage);
        params.put("message", messageArray);
        req.put("params", params);
        return req.toJSONString(new JSONWriter.Feature[0]);
    }

    private String genForwardSendReq(String[][] messageParts, String targetId) {
        JSONObject req = new JSONObject();
        req.put("action", "send_group_forward_msg");
        JSONObject params = new JSONObject();
        params.put("group_id", Long.parseLong(targetId));
        JSONArray messagesArray = new JSONArray();
        for (String[] part : messageParts) {
            JSONObject node = new JSONObject();
            node.put("type", "node");
            JSONObject data = new JSONObject();
            data.put("uin", part[0]);
            data.put("name", part[1]);
            JSONArray content = new JSONArray();
            JSONObject textMsg = new JSONObject();
            textMsg.put("type", "text");
            textMsg.put("data", new JSONObject().fluentPut("text", part[2]));
            content.add(textMsg);
            data.put("content", content);
            node.put("data", data);
            messagesArray.add(node);
        }
        params.put("messages", messagesArray);
        req.put("params", params);
        return req.toJSONString(new JSONWriter.Feature[0]);
    }

    private JSONArray parseMessageContent(String message) {
        String remainingText;
        JSONArray contentArray = new JSONArray();
        int lastIndex = 0;
        Matcher faceMatcher = FACE_PATTERN.matcher(message);
        Matcher imageMatcher = IMAGE_PATTERN.matcher(message);
        ArrayList<MatchResult> allMatches = new ArrayList<MatchResult>();
        while (faceMatcher.find()) {
            allMatches.add(new MatchResult(faceMatcher.start(), faceMatcher.end(), "face", faceMatcher.group(1)));
        }
        while (imageMatcher.find()) {
            allMatches.add(new MatchResult(imageMatcher.start(), imageMatcher.end(), "image", imageMatcher.group(1)));
        }
        allMatches.sort(Comparator.comparingInt(m -> m.start));
        for (int i = 0; i < allMatches.size(); ++i) {
            String textBefore;
            MatchResult match = (MatchResult)allMatches.get(i);
            if (match.start > lastIndex && !(textBefore = message.substring(lastIndex, match.start)).isEmpty()) {
                contentArray.add(this.createTextElement(textBefore));
            }
            if ("face".equals(match.type)) {
                contentArray.add(this.createFaceElement(match.value));
            } else if ("image".equals(match.type)) {
                contentArray.add(this.createImageElement(match.value));
            }
            lastIndex = match.end;
        }
        if (lastIndex < message.length() && !(remainingText = message.substring(lastIndex)).isEmpty()) {
            contentArray.add(this.createTextElement(remainingText));
        }
        if (contentArray.isEmpty()) {
            contentArray.add(this.createTextElement(message));
        }
        return contentArray;
    }

    private JSONObject createTextElement(String text) {
        JSONObject element = new JSONObject();
        element.put("type", "text");
        JSONObject data = new JSONObject();
        data.put("text", text);
        element.put("data", data);
        return element;
    }

    private JSONObject createFaceElement(String id) {
        JSONObject element = new JSONObject();
        element.put("type", "face");
        JSONObject data = new JSONObject();
        data.put("id", id);
        element.put("data", data);
        return element;
    }

    private JSONObject createImageElement(String file) {
        JSONObject element = new JSONObject();
        element.put("type", "image");
        JSONObject data = new JSONObject();
        data.put("file", file);
        element.put("data", data);
        return element;
    }

    private static class MatchResult {
        int start;
        int end;
        String type;
        String value;

        MatchResult(int start, int end, String type, String value) {
            this.start = start;
            this.end = end;
            this.type = type;
            this.value = value;
        }
    }
}

