/*
 * Decompiled with CFR 0.152.
 */
package com.zhanganzhi.chathub.platforms.qq.protocol;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONReader;
import com.zhanganzhi.chathub.platforms.qq.dto.QQEvent;
import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.List;
import java.util.Queue;
import org.java_websocket.WebSocket;
import org.java_websocket.handshake.ClientHandshake;
import org.java_websocket.server.WebSocketServer;
import org.slf4j.Logger;

public class QQWsServer
extends WebSocketServer {
    private final List<WebSocket> clients;
    private final String validResourcePath;
    private final Queue<QQEvent> qqEventDeque;
    private volatile Logger logger;

    public QQWsServer(String host, Integer port, String validResourcePath, Queue<QQEvent> qqEventDeque) {
        super(new InetSocketAddress(host, (int)port));
        this.validResourcePath = validResourcePath;
        this.qqEventDeque = qqEventDeque;
        this.clients = new ArrayList<WebSocket>();
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    @Override
    public void onOpen(WebSocket webSocket, ClientHandshake clientHandshake) {
        if (this.logger != null) {
            InetSocketAddress remoteSocketAddress = webSocket.getRemoteSocketAddress();
            String resourceDescriptor = webSocket.getResourceDescriptor();
            if (this.validResourcePath != null && this.validResourcePath.equals(resourceDescriptor)) {
                List<WebSocket> list = this.clients;
                synchronized (list) {
                    this.clients.add(webSocket);
                }
            } else {
                webSocket.close();
            }
        }
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    @Override
    public void onClose(WebSocket webSocket, int i, String s, boolean b) {
        List<WebSocket> list = this.clients;
        synchronized (list) {
            this.clients.remove(webSocket);
        }
    }

    @Override
    public void onMessage(WebSocket webSocket, String msg) {
        block4: {
            if (msg != null && !msg.isEmpty()) {
                try {
                    QQEvent event = JSON.parseObject(msg, QQEvent.class, JSONReader.Feature.SupportSmartMatch);
                    if (this.qqEventDeque != null) {
                        this.qqEventDeque.add(event);
                    }
                }
                catch (Exception e) {
                    if (this.logger == null) break block4;
                    this.logger.error("[QQ-WS] Failed to parse QQ event message: {}", (Object)msg, (Object)e);
                }
            }
        }
    }

    @Override
    public void onError(WebSocket webSocket, Exception e) {
        if (this.logger != null) {
            this.logger.error("QQ WebSocket connection error", e);
        }
    }

    @Override
    public void onStart() {
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    public void sendMessage(String message) {
        ArrayList<WebSocket> clientsCopy;
        if (message == null || message.isEmpty()) {
            return;
        }
        List<WebSocket> list = this.clients;
        synchronized (list) {
            clientsCopy = new ArrayList<WebSocket>(this.clients);
        }
        for (WebSocket webSocket : clientsCopy) {
            if (webSocket == null) continue;
            try {
                webSocket.send(message);
            }
            catch (Exception e) {
                block14: {
                    if (this.logger != null) {
                        this.logger.error("QQ WebSocket server send message error", e);
                    }
                    try {
                        if (!webSocket.isClosing() && !webSocket.isClosed()) {
                            webSocket.close();
                        }
                    }
                    catch (Exception closeEx) {
                        if (this.logger == null) break block14;
                        this.logger.error("Error closing WebSocket connection", closeEx);
                    }
                }
                List<WebSocket> list2 = this.clients;
                synchronized (list2) {
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

