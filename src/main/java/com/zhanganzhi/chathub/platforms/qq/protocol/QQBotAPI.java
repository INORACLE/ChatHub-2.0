package com.zhanganzhi.chathub.platforms.qq.protocol;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.zhanganzhi.chathub.ChatHubMod;
import com.zhanganzhi.chathub.core.config.Config;
import com.zhanganzhi.chathub.platforms.qq.JsonUtil;
import com.zhanganzhi.chathub.platforms.qq.dto.QQBotEvent;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * QQ official bot API client. Uses the JDK {@link java.net.http.HttpClient}
 * instead of OkHttp. Requires a valid app id/secret to fetch an access token
 * from {@code https://api.q.qq.com}.
 */
public class QQBotAPI {

    private static final Pattern FACE_PATTERN = Pattern.compile("\\[face:(\\d+)\\]");
    private static final Pattern IMAGE_PATTERN = Pattern.compile("\\[image:(.*?)\\]");

    private final Config config = Config.getInstance();
    private final Queue<QQBotEvent> qqBotEventQueue = new ConcurrentLinkedDeque<>();
    private final HttpClient httpClient;
    private final String appId;
    private final String appSecret;
    private volatile String accessToken;
    private volatile long tokenExpireTime;
    private volatile boolean running = true;

    public QQBotAPI(ChatHubMod chatHub, String appId, String appSecret) {
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(10))
                .build();
        this.appId = appId;
        this.appSecret = appSecret;
        Thread refreshThread = new Thread(this::tokenRefreshLoop, "chathub-qqbot-token-refresh");
        refreshThread.setDaemon(true);
        refreshThread.start();
    }

    public Queue<QQBotEvent> getQqBotEventQueue() {
        return this.qqBotEventQueue;
    }

    public void start() {
        this.fetchAccessToken();
    }

    public void stop() {
        this.running = false;
    }

    private void fetchAccessToken() {
        if (this.appId == null || this.appId.isEmpty()) {
            throw new RuntimeException("Failed to get access token: QQ bot appId is not configured");
        }
        String url = "https://api.q.qq.com/cgi-bin/gettoken?appid=" + this.appId + "&secret=" + this.appSecret;
        HttpRequest request = HttpRequest.newBuilder(URI.create(url))
                .timeout(Duration.ofSeconds(30))
                .header("Content-Type", "application/json")
                .GET()
                .build();
        try {
            HttpResponse<String> response = this.httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() == 200 && response.body() != null) {
                JsonObject json = JsonParser.parseString(response.body()).getAsJsonObject();
                if ("0".equals(JsonUtil.getString(json, "errcode"))) {
                    this.accessToken = JsonUtil.getString(json, "access_token");
                    this.tokenExpireTime = System.currentTimeMillis() + json.get("expires_in").getAsLong() * 1000L;
                    return;
                }
                throw new RuntimeException("Failed to get access token: " + JsonUtil.getString(json, "errmsg"));
            }
            throw new RuntimeException("Failed to get access token, HTTP " + response.statusCode());
        } catch (IOException | InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("Failed to fetch access token", e);
        }
    }

    private void tokenRefreshLoop() {
        while (this.running) {
            try {
                long waitTime = this.tokenExpireTime - System.currentTimeMillis() - 300000L;
                if (waitTime > 0L) {
                    Thread.sleep(waitTime);
                }
                this.fetchAccessToken();
                Thread.sleep(2000L);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            } catch (Exception e) {
                try {
                    Thread.sleep(10000L);
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }
        }
    }

    private void sendGroupMessage(String targetId, JsonArray msgArray) {
        if (this.accessToken == null || this.accessToken.isEmpty()) {
            throw new RuntimeException("Access token is not available");
        }
        String url = "https://api.q.qq.com/cgi-bin/message/group_send?access_token=" + this.accessToken;
        JsonObject requestBody = new JsonObject();
        requestBody.addProperty("chat_type", "group");
        requestBody.addProperty("chat_id", targetId);
        requestBody.add("msg", msgArray);
        HttpRequest request = HttpRequest.newBuilder(URI.create(url))
                .timeout(Duration.ofSeconds(30))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(new Gson().toJson(requestBody)))
                .build();
        try {
            HttpResponse<String> response = this.httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() == 200 && response.body() != null) {
                JsonObject json = JsonParser.parseString(response.body()).getAsJsonObject();
                if (!"0".equals(JsonUtil.getString(json, "errcode"))) {
                    throw new RuntimeException("Failed to send group message: " + JsonUtil.getString(json, "errmsg"));
                }
                return;
            }
            throw new RuntimeException("Failed to send group message, HTTP " + response.statusCode());
        } catch (IOException | InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("Failed to send group message", e);
        }
    }

    public void sendMessage(String message, String targetId) {
        this.sendGroupMessage(targetId, this.parseMessageContent(message));
    }

    public void sendReplyMessage(String message, String targetId, Long messageId) {
        JsonArray messageArray = new JsonArray();
        JsonObject replyElement = new JsonObject();
        replyElement.addProperty("type", "reply");
        JsonObject replyData = new JsonObject();
        replyData.addProperty("id", messageId);
        replyElement.add("data", replyData);
        messageArray.add(replyElement);
        messageArray.addAll(this.parseMessageContent(message));
        this.sendGroupMessage(targetId, messageArray);
    }

    public void sendForwardMessage(String[][] messageParts, String targetId) {
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
        this.sendGroupMessage(targetId, messagesArray);
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