/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.moandjiezana.toml.Toml
 */
package com.zhanganzhi.chathub.core.config;

import com.moandjiezana.toml.Toml;
import com.zhanganzhi.chathub.core.config.MessageType;
import com.zhanganzhi.chathub.platforms.Platform;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.CopyOption;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;
import org.slf4j.Logger;

public class Config {
    private static final Config config = new Config();
    private Toml configToml;
    private Logger logger;
    private Path dataDirectory;
    private List<Pattern> minecraftIgnoreChatMessagePatterns = Collections.emptyList();
    private volatile Integer cachedThreadPoolSize;
    private volatile Boolean cachedCompleteTakeoverMode;
    private volatile Boolean cachedQQEnabled;
    private volatile String cachedQQGroupId;
    private volatile Long cachedQQWsReversePort;
    private volatile String cachedQQWsReversePath;
    private volatile String cachedQQRobotUserId;
    private volatile String cachedQQRobotNickname;
    private volatile Boolean cachedQQSendToMinecraft;
    private volatile Boolean cachedQQSendToQQ;
    private volatile Boolean cachedQQEnableBinding;
    private volatile List<String> cachedQQAdmins;
    private volatile String cachedQQAppId;
    private volatile String cachedQQAppSecret;

    private Config() {
    }

    public static Config getInstance() {
        return config;
    }

    public void loadConfig(Path dataDirectory, Logger logger) {
        this.logger = logger;
        this.dataDirectory = dataDirectory;
        File dataDir = dataDirectory.toFile();
        if (!dataDir.exists() && !dataDir.mkdirs()) {
            throw new RuntimeException("Failed to create data directory: " + String.valueOf(dataDirectory));
        }
        File configFile = new File(dataDir, "config.toml");
        if (!configFile.exists()) {
            try (InputStream defaultConfigStream = this.getClass().getClassLoader().getResourceAsStream("config.toml");){
                if (defaultConfigStream == null) {
                    throw new RuntimeException("Default config template not found in resources");
                }
                Files.copy(defaultConfigStream, configFile.toPath(), new CopyOption[0]);
            }
            catch (IOException e) {
                logger.error("Failed to create default config file", e);
                throw new RuntimeException("Failed to initialize config file", e);
            }
        }
        try {
            this.configToml = new Toml().read(configFile);
            this.loadMinecraftIgnoreChatMessagePatterns();
            this.invalidateCache();
        }
        catch (Exception e) {
            logger.error("Failed to read config file: {}", (Object)e.getMessage());
            throw new RuntimeException("Failed to read config file", e);
        }
    }

    private void invalidateCache() {
        this.cachedThreadPoolSize = null;
        this.cachedCompleteTakeoverMode = null;
        this.cachedQQEnabled = null;
        this.cachedQQGroupId = null;
        this.cachedQQWsReversePort = null;
        this.cachedQQWsReversePath = null;
        this.cachedQQRobotUserId = null;
        this.cachedQQRobotNickname = null;
        this.cachedQQSendToMinecraft = null;
        this.cachedQQSendToQQ = null;
        this.cachedQQEnableBinding = null;
        this.cachedQQAdmins = null;
        this.cachedQQAppId = null;
        this.cachedQQAppSecret = null;
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    public int getCoreThreadPoolSize() {
        if (this.cachedThreadPoolSize == null) {
            Config config = this;
            synchronized (config) {
                if (this.cachedThreadPoolSize == null) {
                    this.cachedThreadPoolSize = this.configToml.getLong("core.threadPoolSize", Long.valueOf(4L)).intValue();
                }
            }
        }
        return this.cachedThreadPoolSize;
    }

    public String getServername(String server) {
        String servername = this.configToml.getString("servername." + server);
        return servername != null ? servername : server;
    }

    public List<String> getServerOrder() {
        List serverOrder = this.configToml.getList("servername.order");
        if (serverOrder != null) {
            return serverOrder;
        }
        return List.of("lobby", "1", "2", "bkm", "ceshi");
    }

    public String getMessage(Platform platform, MessageType messageType) {
        String message = this.configToml.getString("%s.message.%s".formatted(platform.getConfigNamespace(), messageType.getName()));
        return message != null ? message : "";
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    public boolean isCompleteTakeoverMode() {
        if (this.cachedCompleteTakeoverMode == null) {
            Config config = this;
            synchronized (config) {
                if (this.cachedCompleteTakeoverMode == null) {
                    this.cachedCompleteTakeoverMode = this.configToml.getBoolean("minecraft.completeTakeoverMode", Boolean.valueOf(false));
                }
            }
        }
        return this.cachedCompleteTakeoverMode;
    }

    public List<String> getMinecraftIgnoreChatMessageRe() {
        List<String> patterns = this.configToml.getList("minecraft.ignoreChatMessageRe");
        return patterns != null ? patterns : Collections.emptyList();
    }

    public List<Pattern> getMinecraftIgnoreChatMessagePatterns() {
        return this.minecraftIgnoreChatMessagePatterns;
    }

    private void loadMinecraftIgnoreChatMessagePatterns() {
        List<String> patterns = this.getMinecraftIgnoreChatMessageRe();
        if (patterns.isEmpty()) {
            this.minecraftIgnoreChatMessagePatterns = Collections.emptyList();
            return;
        }
        ArrayList<Pattern> compiledPatterns = new ArrayList<Pattern>();
        for (String pattern : patterns) {
            if (pattern == null) {
                this.logger.warn("Ignore chat regex is null and will be skipped");
                continue;
            }
            try {
                compiledPatterns.add(Pattern.compile(pattern));
            }
            catch (PatternSyntaxException e) {
                this.logger.warn("Ignore chat regex is invalid and will be skipped: {}", (Object)pattern, (Object)e);
            }
        }
        this.minecraftIgnoreChatMessagePatterns = Collections.unmodifiableList(compiledPatterns);
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    public boolean isQQEnabled() {
        if (this.cachedQQEnabled == null) {
            Config config = this;
            synchronized (config) {
                if (this.cachedQQEnabled == null) {
                    this.cachedQQEnabled = this.configToml.getBoolean("qq.enable", Boolean.valueOf(false));
                }
            }
        }
        return this.cachedQQEnabled;
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    public String getQQGroupId() {
        if (this.cachedQQGroupId == null) {
            Config config = this;
            synchronized (config) {
                if (this.cachedQQGroupId == null) {
                    this.cachedQQGroupId = this.configToml.getString("qq.groupId", "");
                }
            }
        }
        return this.cachedQQGroupId;
    }

    public String getQQHost() {
        return this.configToml.getString("qq.api.host");
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    public Long getQQWsReversePort() {
        if (this.cachedQQWsReversePort == null) {
            Config config = this;
            synchronized (config) {
                if (this.cachedQQWsReversePort == null) {
                    this.cachedQQWsReversePort = this.configToml.getLong("qq.api.wsReversePort", Long.valueOf(9001L));
                }
            }
        }
        return this.cachedQQWsReversePort;
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    public String getQQWsReversePath() {
        if (this.cachedQQWsReversePath == null) {
            Config config = this;
            synchronized (config) {
                if (this.cachedQQWsReversePath == null) {
                    this.cachedQQWsReversePath = this.configToml.getString("qq.api.wsReversePath", "");
                }
            }
        }
        return this.cachedQQWsReversePath;
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    public String getQQRobotUserId() {
        if (this.cachedQQRobotUserId == null) {
            Config config = this;
            synchronized (config) {
                if (this.cachedQQRobotUserId == null) {
                    this.cachedQQRobotUserId = this.configToml.getString("qq.robotUserId", "379450326");
                }
            }
        }
        return this.cachedQQRobotUserId;
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    public String getQQRobotNickname() {
        if (this.cachedQQRobotNickname == null) {
            Config config = this;
            synchronized (config) {
                if (this.cachedQQRobotNickname == null) {
                    this.cachedQQRobotNickname = this.configToml.getString("qq.robotNickname", "\u55b5\u55b5\u55b5");
                }
            }
        }
        return this.cachedQQRobotNickname;
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    public boolean isQQSendToMinecraft() {
        if (this.cachedQQSendToMinecraft == null) {
            Config config = this;
            synchronized (config) {
                if (this.cachedQQSendToMinecraft == null) {
                    this.cachedQQSendToMinecraft = this.configToml.getBoolean("qq.sendToMinecraft", Boolean.valueOf(true));
                }
            }
        }
        return this.cachedQQSendToMinecraft;
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    public boolean isQQSendToQQ() {
        if (this.cachedQQSendToQQ == null) {
            Config config = this;
            synchronized (config) {
                if (this.cachedQQSendToQQ == null) {
                    this.cachedQQSendToQQ = this.configToml.getBoolean("qq.sendToQQ", Boolean.valueOf(false));
                }
            }
        }
        return this.cachedQQSendToQQ;
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    public boolean isQQEnableBinding() {
        if (this.cachedQQEnableBinding == null) {
            Config config = this;
            synchronized (config) {
                if (this.cachedQQEnableBinding == null) {
                    this.cachedQQEnableBinding = this.configToml.getBoolean("qq.enableBinding", Boolean.valueOf(false));
                }
            }
        }
        return this.cachedQQEnableBinding;
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    public List<String> getQQAdmins() {
        if (this.cachedQQAdmins == null) {
            Config config = this;
            synchronized (config) {
                if (this.cachedQQAdmins == null) {
                    List admins = this.configToml.getList("qq.admins");
                    this.cachedQQAdmins = admins != null ? admins : Collections.emptyList();
                }
            }
        }
        return this.cachedQQAdmins;
    }

    public boolean isQQAdmin(String qqId) {
        return this.getQQAdmins().contains(qqId);
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    public String getQQAppId() {
        if (this.cachedQQAppId == null) {
            Config config = this;
            synchronized (config) {
                if (this.cachedQQAppId == null) {
                    this.cachedQQAppId = this.configToml.getString("qq.bot.appId", "");
                }
            }
        }
        return this.cachedQQAppId;
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    public String getQQAppSecret() {
        if (this.cachedQQAppSecret == null) {
            Config config = this;
            synchronized (config) {
                if (this.cachedQQAppSecret == null) {
                    this.cachedQQAppSecret = this.configToml.getString("qq.bot.appSecret", "");
                }
            }
        }
        return this.cachedQQAppSecret;
    }

    public void reloadConfig() {
        this.invalidateCache();
        File configFile = new File(this.dataDirectory.toFile(), "config.toml");
        try {
            this.configToml = new Toml().read(configFile);
            this.loadMinecraftIgnoreChatMessagePatterns();
            this.invalidateCache();
        }
        catch (Exception e) {
            if (this.logger != null) {
                this.logger.error("Failed to reload config file", e);
            }
            throw new RuntimeException("Failed to reload config file", e);
        }
    }
}

