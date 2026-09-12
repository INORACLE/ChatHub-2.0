/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.google.common.util.concurrent.ThreadFactoryBuilder
 *  com.google.inject.Inject
 *  com.velocitypowered.api.command.Command
 *  com.velocitypowered.api.event.Subscribe
 *  com.velocitypowered.api.event.proxy.ProxyInitializeEvent
 *  com.velocitypowered.api.event.proxy.ProxyShutdownEvent
 *  com.velocitypowered.api.plugin.Plugin
 *  com.velocitypowered.api.plugin.annotation.DataDirectory
 *  com.velocitypowered.api.proxy.ProxyServer
 */
package com.zhanganzhi.chathub;

import com.google.common.util.concurrent.ThreadFactoryBuilder;
import com.google.inject.Inject;
import com.velocitypowered.api.command.Command;
import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.proxy.ProxyInitializeEvent;
import com.velocitypowered.api.event.proxy.ProxyShutdownEvent;
import com.velocitypowered.api.plugin.Plugin;
import com.velocitypowered.api.plugin.annotation.DataDirectory;
import com.velocitypowered.api.proxy.ProxyServer;
import com.zhanganzhi.chathub.core.EventHub;
import com.zhanganzhi.chathub.core.config.Config;
import com.zhanganzhi.chathub.platforms.velocity.VelocityCommand;
import java.nio.file.Path;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import org.slf4j.Logger;

@Plugin(id="chathub", name="ChatHub", version="1.9.1", url="https://github.com/AnzhiZhang/ChatHub", description="Chat hub for servers under velocity proxy", authors={"Andy Zhang", "ZhuRuoLing", "401U", "maoyihao"})
public class ChatHub {
    private final ProxyServer proxyServer;
    private final Logger logger;
    private final Path dataDirectory;
    private ThreadPoolExecutor threadPoolExecutor;
    private EventHub eventHub;

    @Inject
    public ChatHub(ProxyServer proxyServer, Logger logger, @DataDirectory Path dataDirectory) {
        this.proxyServer = proxyServer;
        this.logger = logger;
        this.dataDirectory = dataDirectory;
    }

    @Subscribe
    public void onInitialize(ProxyInitializeEvent event) {
        try {
            Config config = Config.getInstance();
            config.loadConfig(this.dataDirectory, this.logger);
            this.threadPoolExecutor = (ThreadPoolExecutor)Executors.newFixedThreadPool(config.getCoreThreadPoolSize(), new ThreadFactoryBuilder().setNameFormat("chathub-tasks-%d").build());
            this.eventHub = new EventHub(this);
            this.proxyServer.getCommandManager().register(this.proxyServer.getCommandManager().metaBuilder("chathub").plugin((Object)this).build(), (Command)new VelocityCommand(this));
            this.threadPoolExecutor.submit(() -> {
                try {
                    this.eventHub.start();
                }
                catch (Exception e) {
                    this.logger.error("Error starting event hub", e);
                }
            });
        }
        catch (Exception e) {
            this.logger.error("Failed to initialize ChatHub", e);
            throw e;
        }
    }

    @Subscribe
    public void onProxyShutdown(ProxyShutdownEvent event) {
        try {
            if (this.eventHub != null) {
                this.eventHub.shutdown();
            }
        }
        catch (Exception e) {
            this.logger.error("Error during ChatHub shutdown", e);
        }
        finally {
            if (this.threadPoolExecutor != null) {
                this.threadPoolExecutor.shutdown();
                try {
                    if (!this.threadPoolExecutor.awaitTermination(10L, TimeUnit.SECONDS)) {
                        this.logger.warn("Thread pool did not terminate in time, forcing shutdown");
                        this.threadPoolExecutor.shutdownNow();
                        if (!this.threadPoolExecutor.awaitTermination(5L, TimeUnit.SECONDS)) {
                            this.logger.error("Thread pool failed to terminate");
                        }
                    }
                }
                catch (InterruptedException e) {
                    this.threadPoolExecutor.shutdownNow();
                    Thread.currentThread().interrupt();
                }
            }
        }
    }

    public String getVersion() {
        return "1.9.1";
    }

    public ProxyServer getProxyServer() {
        return this.proxyServer;
    }

    public Logger getLogger() {
        return this.logger;
    }

    public ThreadPoolExecutor getThreadPoolExecutor() {
        return this.threadPoolExecutor;
    }

    public EventHub getEventHub() {
        return this.eventHub;
    }

    public Path getDataDirectory() {
        return this.dataDirectory;
    }

    public void reloadConfig() {
        try {
            Config.getInstance().reloadConfig();
            this.logger.info("\u914d\u7f6e\u6587\u4ef6\u5df2\u6210\u529f\u91cd\u8f7d");
        }
        catch (Exception e) {
            this.logger.error("\u91cd\u8f7d\u914d\u7f6e\u6587\u4ef6\u5931\u8d25", e);
            throw e;
        }
    }

    public void reloadBindings() {
        try {
            this.eventHub.getBindingManager().reloadBindings();
            this.logger.info("\u7ed1\u5b9a\u6587\u4ef6\u5df2\u6210\u529f\u91cd\u8f7d");
        }
        catch (Exception e) {
            this.logger.error("\u91cd\u8f7d\u7ed1\u5b9a\u6587\u4ef6\u5931\u8d25", e);
            throw e;
        }
    }
}

