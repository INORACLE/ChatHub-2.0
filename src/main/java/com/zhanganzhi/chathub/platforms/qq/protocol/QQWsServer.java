package com.zhanganzhi.chathub.platforms.qq.protocol;

import com.google.gson.JsonSyntaxException;
import com.zhanganzhi.chathub.platforms.qq.dto.QQEvent;
import org.java_websocket.WebSocket;
import org.java_websocket.handshake.ClientHandshake;
import org.java_websocket.server.WebSocketServer;
import org.apache.logging.log4j.Logger;

import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.List;
import java.util.Queue;

public class QQWsServer extends WebSocketServer {

    private static final com.google.gson.Gson GSON = new com.google.gson.Gson();

    private final List<WebSocket> clients;
    private final String validResourcePath;
    private final Queue<QQEvent> qqEventDeque;
    private volatile Logger logger;

    public QQWsServer(String host, int port, String validResourcePath, Queue<QQEvent> qqEventDeque) {
        super(new InetSocketAddress(host, port));
        this.validResourcePath = validResourcePath;
        this.qqEventDeque = qqEventDeque;
        this.clients = new ArrayList<>();
    }

    @Override
    public void onOpen(WebSocket webSocket, ClientHandshake clientHandshake) {
        if (this.logger != null) {
            InetSocketAddress remoteSocketAddress = webSocket.getRemoteSocketAddress();
            String resourceDescriptor = webSocket.getResourceDescriptor();
            if (this.validResourcePath != null && this.validResourcePath.equals(resourceDescriptor)) {
                synchronized (this.clients) {
                    this.clients.add(webSocket);
                }
                this.logger.info("[QQ-WS] Client connected from {} via {}", remoteSocketAddress, resourceDescriptor);
            } else {
                this.logger.warn("[QQ-WS] Rejecting client {} with invalid path {}", remoteSocketAddress, resourceDescriptor);
                webSocket.close();
            }
        }
    }

    @Override
    public void onClose(WebSocket webSocket, int code, String reason, boolean remote) {
        synchronized (this.clients) {
            this.clients.remove(webSocket);
        }
    }

    @Override
    public void onMessage(WebSocket webSocket, String msg) {
        if (msg != null && !msg.isEmpty()) {
            try {
                QQEvent event = GSON.fromJson(msg, QQEvent.class);
                if (this.qqEventDeque != null && event != null) {
                    this.qqEventDeque.add(event);
                }
            } catch (JsonSyntaxException e) {
                if (this.logger != null) {
                    this.logger.error("[QQ-WS] Failed to parse QQ event message: {}", msg, e);
                }
            }
        }
    }

    @Override
    public void onError(WebSocket webSocket, Exception e) {
        if (this.logger != null) {
            this.logger.error("[QQ-WS] WebSocket connection error", e);
        }
    }

    @Override
    public void onStart() {
        if (this.logger != null) {
            this.logger.info("[QQ-WS] Reverse WebSocket server listening on {}:{}", getAddress().getHostString(), getPort());
        }
    }

    public void sendMessage(String message) {
        if (message == null || message.isEmpty()) {
            return;
        }
        final List<WebSocket> clientsCopy;
        synchronized (this.clients) {
            clientsCopy = new ArrayList<>(this.clients);
        }
        for (WebSocket webSocket : clientsCopy) {
            if (webSocket == null) {
                continue;
            }
            try {
                webSocket.send(message);
            } catch (Exception e) {
                if (this.logger != null) {
                    this.logger.error("[QQ-WS] Send message error", e);
                }
                try {
                    if (!webSocket.isClosing() && !webSocket.isClosed()) {
                        webSocket.close();
                    }
                } catch (Exception closeEx) {
                    if (this.logger != null) {
                        this.logger.error("[QQ-WS] Error closing WebSocket connection", closeEx);
                    }
                }
                synchronized (this.clients) {
                    this.clients.remove(webSocket);
                }
            }
        }
    }

    public Logger getLogger() {
        return this.logger;
    }

    public void setLogger(Logger logger) {
        this.logger = logger;
    }
}