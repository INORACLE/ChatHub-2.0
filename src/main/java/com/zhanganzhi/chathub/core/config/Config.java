package com.zhanganzhi.chathub.core.config;

import com.zhanganzhi.chathub.platforms.Platform;
import org.apache.logging.log4j.Logger;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

/**
 * Singleton configuration holder backed by a {@code config.toml} file in the
 * mod data directory ({@code <server>/config/chathub/config.toml}).
 * A default config template is copied from the mod resources on first load.
 */
public class Config {

    private static final Config config = new Config();

    private Toml configToml;
    private Logger logger;
    private Path dataDirectory;

    private List<Pattern> minecraftIgnoreChatMessagePatterns = Collections.emptyList();

    private volatile Integer cachedThreadPoolSize;
    private volatile String cachedServerName;
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
            throw new RuntimeException("Failed to create data directory: " + dataDirectory);
        }
        File configFile = new File(dataDir, "config.toml");
        if (!configFile.exists()) {
            try (InputStream defaultConfigStream = this.getClass().getClassLoader().getResourceAsStream("config.toml")) {
                if (defaultConfigStream == null) {
                    throw new RuntimeException("Default config template not found in resources");
                }
                Files.copy(defaultConfigStream, configFile.toPath());
            } catch (IOException e) {
                logger.error("Failed to create default config file", e);
                throw new RuntimeException("Failed to initialize config file", e);
            }
        }
        try {
            this.configToml = Toml.read(configFile);
            this.loadMinecraftIgnoreChatMessagePatterns();
            this.invalidateCache();
        } catch (Exception e) {
            logger.error("Failed to read config file: " + e.getMessage());
            throw new RuntimeException("Failed to read config file", e);
        }
    }

    private void invalidateCache() {
        this.cachedThreadPoolSize = null;
        this.cachedServerName = null;
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

    private Toml tom() {
        return this.configToml;
    }

    public int getCoreThreadPoolSize() {
        if (this.cachedThreadPoolSize == null) {
            synchronized (this) {
                if (this.cachedThreadPoolSize == null) {
                    this.cachedThreadPoolSize = this.tom() != null
                            ? this.tom().getLong("core.threadPoolSize", 2L).intValue() : 2;
                }
            }
        }
        return this.cachedThreadPoolSize;
    }

    /** Display name of the current single server. */
    public String getServerName() {
        if (this.cachedServerName == null) {
            synchronized (this) {
                if (this.cachedServerName == null) {
                    this.cachedServerName = this.tom() != null
                            ? this.tom().getString("server.name", "Minecraft") : "Minecraft";
                }
            }
        }
        return this.cachedServerName;
    }

    /** Aliasing helper kept for template compatibility ({servername} mapping). */
    public String getServername(String server) {
        if (server == null) {
            return null;
        }
        if (this.tom() != null) {
            String servername = this.tom().getString("servername." + server);
            if (servername != null) {
                return servername;
            }
        }
        return server;
    }

    /** Display name for a dimension key, e.g. {@code the_nether} -> 下界. */
    public String getDimensionName(String dimension) {
        if (dimension == null) {
            return null;
        }
        if (this.tom() != null) {
            String name = this.tom().getString("dimension." + dimension);
            if (name != null) {
                return name;
            }
        }
        return dimension;
    }

    public String getMessage(Platform platform, MessageType messageType) {
        if (this.tom() == null) {
            return "";
        }
        String message = this.tom().getString(
                "%s.message.%s".formatted(platform.getConfigNamespace(), messageType.getName()));
        return message != null ? message : "";
    }

    public boolean isCompleteTakeoverMode() {
        if (this.cachedCompleteTakeoverMode == null) {
            synchronized (this) {
                if (this.cachedCompleteTakeoverMode == null) {
                    this.cachedCompleteTakeoverMode = this.tom() != null
                            && this.tom().getBoolean("minecraft.completeTakeoverMode", true);
                }
            }
        }
        return this.cachedCompleteTakeoverMode;
    }

    public List<String> getMinecraftIgnoreChatMessageRe() {
        if (this.tom() == null) {
            return Collections.emptyList();
        }
        List<?> patterns = this.tom().getList("minecraft.ignoreChatMessageRe");
        if (patterns == null) {
            return Collections.emptyList();
        }
        List<String> result = new ArrayList<>();
        for (Object pattern : patterns) {
            result.add(String.valueOf(pattern));
        }
        return result;
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
        List<Pattern> compiledPatterns = new ArrayList<>();
        for (String pattern : patterns) {
            if (pattern == null) {
                if (this.logger != null) {
                    this.logger.warn("Ignore chat regex is null and will be skipped");
                }
                continue;
            }
            try {
                compiledPatterns.add(Pattern.compile(pattern));
            } catch (PatternSyntaxException e) {
                if (this.logger != null) {
                    this.logger.warn("Ignore chat regex is invalid and will be skipped: " + pattern, e);
                }
            }
        }
        this.minecraftIgnoreChatMessagePatterns = Collections.unmodifiableList(compiledPatterns);
    }

    public boolean isQQEnabled() {
        if (this.cachedQQEnabled == null) {
            synchronized (this) {
                if (this.cachedQQEnabled == null) {
                    this.cachedQQEnabled = this.tom() != null
                            && this.tom().getBoolean("qq.enable", false);
                }
            }
        }
        return this.cachedQQEnabled;
    }

    public String getQQGroupId() {
        if (this.cachedQQGroupId == null) {
            synchronized (this) {
                if (this.cachedQQGroupId == null) {
                    this.cachedQQGroupId = this.tom() != null
                            ? this.tom().getString("qq.groupId", "") : "";
                }
            }
        }
        return this.cachedQQGroupId;
    }

    public String getQQHost() {
        if (this.tom() == null) {
            return "0.0.0.0";
        }
        String host = this.tom().getString("qq.api.host");
        return host != null ? host : "0.0.0.0";
    }

    public Long getQQWsReversePort() {
        if (this.cachedQQWsReversePort == null) {
            synchronized (this) {
                if (this.cachedQQWsReversePort == null) {
                    this.cachedQQWsReversePort = this.tom() != null
                            ? this.tom().getLong("qq.api.wsReversePort", 9001L) : 9001L;
                }
            }
        }
        return this.cachedQQWsReversePort;
    }

    public String getQQWsReversePath() {
        if (this.cachedQQWsReversePath == null) {
            synchronized (this) {
                if (this.cachedQQWsReversePath == null) {
                    this.cachedQQWsReversePath = this.tom() != null
                            ? this.tom().getString("qq.api.wsReversePath", "") : "";
                }
            }
        }
        return this.cachedQQWsReversePath;
    }

    public String getQQRobotUserId() {
        if (this.cachedQQRobotUserId == null) {
            synchronized (this) {
                if (this.cachedQQRobotUserId == null) {
                    this.cachedQQRobotUserId = this.tom() != null
                            ? this.tom().getString("qq.robotUserId", "") : "";
                }
            }
        }
        return this.cachedQQRobotUserId;
    }

    public String getQQRobotNickname() {
        if (this.cachedQQRobotNickname == null) {
            synchronized (this) {
                if (this.cachedQQRobotNickname == null) {
                    this.cachedQQRobotNickname = this.tom() != null
                            ? this.tom().getString("qq.robotNickname", "ChatBot") : "ChatBot";
                }
            }
        }
        return this.cachedQQRobotNickname;
    }

    public boolean isQQSendToMinecraft() {
        if (this.cachedQQSendToMinecraft == null) {
            synchronized (this) {
                if (this.cachedQQSendToMinecraft == null) {
                    this.cachedQQSendToMinecraft = this.tom() != null
                            && this.tom().getBoolean("qq.sendToMinecraft", true);
                }
            }
        }
        return this.cachedQQSendToMinecraft;
    }

    public boolean isQQSendToQQ() {
        if (this.cachedQQSendToQQ == null) {
            synchronized (this) {
                if (this.cachedQQSendToQQ == null) {
                    this.cachedQQSendToQQ = this.tom() != null
                            && this.tom().getBoolean("qq.sendToQQ", false);
                }
            }
        }
        return this.cachedQQSendToQQ;
    }

    public boolean isQQEnableBinding() {
        if (this.cachedQQEnableBinding == null) {
            synchronized (this) {
                if (this.cachedQQEnableBinding == null) {
                    this.cachedQQEnableBinding = this.tom() != null
                            && this.tom().getBoolean("qq.enableBinding", false);
                }
            }
        }
        return this.cachedQQEnableBinding;
    }

    public List<String> getQQAdmins() {
        if (this.cachedQQAdmins == null) {
            synchronized (this) {
                if (this.cachedQQAdmins == null) {
                    List<?> admins = this.tom() != null ? this.tom().getList("qq.admins") : null;
                    if (admins == null) {
                        this.cachedQQAdmins = Collections.emptyList();
                    } else {
                        List<String> result = new ArrayList<>();
                        for (Object admin : admins) {
                            result.add(String.valueOf(admin));
                        }
                        this.cachedQQAdmins = result;
                    }
                }
            }
        }
        return this.cachedQQAdmins;
    }

    public boolean isQQAdmin(String qqId) {
        return this.getQQAdmins().contains(qqId);
    }

    public String getQQAppId() {
        if (this.cachedQQAppId == null) {
            synchronized (this) {
                if (this.cachedQQAppId == null) {
                    this.cachedQQAppId = this.tom() != null
                            ? this.tom().getString("qq.bot.appId", "") : "";
                }
            }
        }
        return this.cachedQQAppId;
    }

    public String getQQAppSecret() {
        if (this.cachedQQAppSecret == null) {
            synchronized (this) {
                if (this.cachedQQAppSecret == null) {
                    this.cachedQQAppSecret = this.tom() != null
                            ? this.tom().getString("qq.bot.appSecret", "") : "";
                }
            }
        }
        return this.cachedQQAppSecret;
    }

    public void reloadConfig() {
        this.invalidateCache();
        if (this.dataDirectory == null) {
            throw new RuntimeException("Failed to reload config file: config not loaded yet");
        }
        File configFile = new File(this.dataDirectory.toFile(), "config.toml");
        try {
            this.configToml = Toml.read(configFile);
            this.loadMinecraftIgnoreChatMessagePatterns();
            this.invalidateCache();
        } catch (Exception e) {
            if (this.logger != null) {
                this.logger.error("Failed to reload config file", e);
            }
            throw new RuntimeException("Failed to reload config file", e);
        }
    }
}