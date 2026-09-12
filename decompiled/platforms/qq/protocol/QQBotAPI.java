/*
 * Decompiled with CFR 0.152.
 */
package com.zhanganzhi.chathub.platforms.qq.protocol;

import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import com.alibaba.fastjson2.JSONWriter;
import com.zhanganzhi.chathub.ChatHub;
import com.zhanganzhi.chathub.core.config.Config;
import com.zhanganzhi.chathub.platforms.qq.dto.QQBotEvent;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class QQBotAPI {
    private final Config config = Config.getInstance();
    private final Queue<QQBotEvent> qqBotEventQueue = new ConcurrentLinkedDeque<QQBotEvent>();
    private final OkHttpClient httpClient = new OkHttpClient.Builder().connectTimeout(10L, TimeUnit.SECONDS).readTimeout(30L, TimeUnit.SECONDS).writeTimeout(10L, TimeUnit.SECONDS).build();
    private final String appId = this.config.getQQAppId() != null ? this.config.getQQAppId() : "";
    private final String appSecret = this.config.getQQAppSecret() != null ? this.config.getQQAppSecret() : "";
    private volatile String accessToken;
    private volatile long tokenExpireTime;
    private static final Pattern FACE_PATTERN = Pattern.compile("\\[face:(\\d+)\\]");
    private static final Pattern IMAGE_PATTERN = Pattern.compile("\\[image:(.*?)\\]");

    public QQBotAPI(ChatHub chatHub) {
        new Thread(this::tokenRefreshLoop, "chathub-qqbot-token-refresh").start();
    }

    public Queue<QQBotEvent> getQqBotEventQueue() {
        return this.qqBotEventQueue;
    }

    public void start() {
        this.fetchAccessToken();
    }

    public void stop() {
        if (this.httpClient != null) {
            this.httpClient.dispatcher().executorService().shutdown();
            this.httpClient.connectionPool().evictAll();
        }
    }

    private void fetchAccessToken() {
        block10: {
            try {
                String url = "https://api.q.qq.com/cgi-bin/gettoken";
                Request request = new Request.Builder().url(url).get().addHeader("Content-Type", "application/json").build();
                try (Response response = this.httpClient.newCall(request).execute();){
                    if (response.isSuccessful() && response.body() != null) {
                        String responseBody = response.body().string();
                        JSONObject json = JSONObject.parseObject(responseBody);
                        if ("0".equals(json.getString("errcode"))) {
                            this.accessToken = json.getString("access_token");
                            long expiresIn = json.getLong("expires_in");
                            this.tokenExpireTime = System.currentTimeMillis() + expiresIn * 1000L;
                            break block10;
                        }
                        throw new RuntimeException("Failed to get access token: " + json.getString("errmsg"));
                    }
                    throw new RuntimeException("Failed to get access token, HTTP " + response.code());
                }
            }
            catch (IOException e) {
                throw new RuntimeException("Failed to fetch access token", e);
            }
        }
    }

    private void tokenRefreshLoop() {
        while (true) {
            try {
                while (true) {
                    long waitTime;
                    if ((waitTime = this.tokenExpireTime - System.currentTimeMillis() - 300000L) > 0L) {
                        Thread.sleep(waitTime);
                    }
                    this.fetchAccessToken();
                    Thread.sleep(2000L);
                }
            }
            catch (InterruptedException e) {
                break;
            }
            catch (Exception e) {
                try {
                    Thread.sleep(10000L);
                }
                catch (InterruptedException ie) {
                    break;
                }
            }
        }
    }

    public void sendMessage(String message, String targetId) {
        block11: {
            if (this.accessToken == null || this.accessToken.isEmpty()) {
                throw new RuntimeException("Access token is not available");
            }
            String url = "https://api.q.qq.com/cgi-bin/message/group_send?access_token=" + this.accessToken;
            try {
                JSONObject requestBody = new JSONObject();
                requestBody.put("chat_type", "group");
                requestBody.put("chat_id", targetId);
                JSONArray parsedMessage = this.parseMessageContent(message);
                requestBody.put("msg", parsedMessage);
                RequestBody body = RequestBody.create(requestBody.toJSONString(new JSONWriter.Feature[0]), MediaType.parse("application/json; charset=utf-8"));
                Request request = new Request.Builder().url(url).post(body).addHeader("Content-Type", "application/json").build();
                try (Response response = this.httpClient.newCall(request).execute();){
                    if (response.isSuccessful() && response.body() != null) {
                        String responseBody = response.body().string();
                        JSONObject json = JSONObject.parseObject(responseBody);
                        if (!"0".equals(json.getString("errcode"))) {
                            throw new RuntimeException("Failed to send message: " + json.getString("errmsg"));
                        }
                        break block11;
                    }
                    throw new RuntimeException("Failed to send message, HTTP " + response.code());
                }
            }
            catch (IOException e) {
                throw new RuntimeException("Failed to send message", e);
            }
        }
    }

    public void sendReplyMessage(String message, String targetId, Long messageId) {
        block11: {
            if (this.accessToken == null || this.accessToken.isEmpty()) {
                throw new RuntimeException("Access token is not available");
            }
            String url = "https://api.q.qq.com/cgi-bin/message/group_send?access_token=" + this.accessToken;
            try {
                JSONObject requestBody = new JSONObject();
                requestBody.put("chat_type", "group");
                requestBody.put("chat_id", targetId);
                JSONArray messageArray = new JSONArray();
                JSONObject replyElement = new JSONObject();
                replyElement.put("type", "reply");
                JSONObject replyData = new JSONObject();
                replyData.put("id", messageId);
                replyElement.put("data", replyData);
                messageArray.add(replyElement);
                JSONArray parsedMessage = this.parseMessageContent(message);
                messageArray.addAll(parsedMessage);
                requestBody.put("msg", messageArray);
                RequestBody body = RequestBody.create(requestBody.toJSONString(new JSONWriter.Feature[0]), MediaType.parse("application/json; charset=utf-8"));
                Request request = new Request.Builder().url(url).post(body).addHeader("Content-Type", "application/json").build();
                try (Response response = this.httpClient.newCall(request).execute();){
                    if (response.isSuccessful() && response.body() != null) {
                        String responseBody = response.body().string();
                        JSONObject json = JSONObject.parseObject(responseBody);
                        if (!"0".equals(json.getString("errcode"))) {
                            throw new RuntimeException("Failed to send reply message: " + json.getString("errmsg"));
                        }
                        break block11;
                    }
                    throw new RuntimeException("Failed to send reply message, HTTP " + response.code());
                }
            }
            catch (IOException e) {
                throw new RuntimeException("Failed to send reply message", e);
            }
        }
    }

    public void sendForwardMessage(String[][] messageParts, String targetId) {
        block12: {
            if (this.accessToken == null || this.accessToken.isEmpty()) {
                throw new RuntimeException("Access token is not available");
            }
            String url = "https://api.q.qq.com/cgi-bin/message/group_send?access_token=" + this.accessToken;
            try {
                JSONObject requestBody = new JSONObject();
                requestBody.put("chat_type", "group");
                requestBody.put("chat_id", targetId);
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
                requestBody.put("msg", messagesArray);
                RequestBody body = RequestBody.create(requestBody.toJSONString(new JSONWriter.Feature[0]), MediaType.parse("application/json; charset=utf-8"));
                Request request = new Request.Builder().url(url).post(body).addHeader("Content-Type", "application/json").build();
                try (Response response = this.httpClient.newCall(request).execute();){
                    if (response.isSuccessful() && response.body() != null) {
                        String responseBody = response.body().string();
                        JSONObject json = JSONObject.parseObject(responseBody);
                        if (!"0".equals(json.getString("errcode"))) {
                            throw new RuntimeException("Failed to send forward message: " + json.getString("errmsg"));
                        }
                        break block12;
                    }
                    throw new RuntimeException("Failed to send forward message, HTTP " + response.code());
                }
            }
            catch (IOException e) {
                throw new RuntimeException("Failed to send forward message", e);
            }
        }
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

