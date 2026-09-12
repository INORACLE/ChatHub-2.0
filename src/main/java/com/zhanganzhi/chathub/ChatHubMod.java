package com.zhanganzhi.chathub;

import com.zhanganzhi.chathub.core.EventHub;
import com.zhanganzhi.chathub.core.adaptor.IAdaptor;
import com.zhanganzhi.chathub.core.config.Config;
import com.zhanganzhi.chathub.core.formatter.IFormatter;
import com.zhanganzhi.chathub.platforms.forge.ForgeAdaptor;
import com.zhanganzhi.chathub.platforms.forge.ForgeCommand;
import com.zhanganzhi.chathub.platforms.forge.ForgeFormatter;
import com.zhanganzhi.chathub.platforms.forge.ForgePlayerManager;
import com.zhanganzhi.chathub.platforms.qq.QQAdaptor;
import com.zhanganzhi.chathub.platforms.qq.QQBotAdapter;
import com.zhanganzhi.chathub.platforms.qq.QQBotFormatter;
import com.zhanganzhi.chathub.platforms.qq.QQFormatter;
import com.zhanganzhi.chathub.platforms.qq.protocol.QQAPI;
import com.zhanganzhi.chathub.platforms.qq.protocol.QQBotAPI;
import net.minecraft.server.MinecraftServer;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.server.ServerStartedEvent;
import net.minecraftforge.event.server.ServerStoppingEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.loading.FMLPaths;
import net.minecraftforge.fml.IExtensionPoint;
import net.minecraftforge.fml.common.Mod;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * ChatHub 2.0 - a Minecraft Forge 1.20.1 port of the ChatHub plugin.
 *
 * Removed the original multi-proxy (Velocity) features; instead the mod runs
 * directly inside a single server and adds a dimension-name prefix before the
 * player name in the chat box. Chat can be relayed to a QQ group through a
 * OneBot reverse web socket or the QQ official bot API.
 */
@Mod(ChatHubMod.MOD_ID)
public class ChatHubMod {

    public static final String MOD_ID = "chathub";

    private final Logger logger = LogManager.getLogger(ChatHubMod.class);
    private final Path dataDirectory;
    private final ExecutorService executorService;
    private final EventHub eventHub;
    private final List<IAdaptor<? extends IFormatter>> adaptors = new ArrayList<>();

    private ForgeAdaptor forgeAdaptor;
    private ForgePlayerManager playerManager;
    private MinecraftServer server;

    public ChatHubMod() {
        ModLoadingContext.get().registerExtensionPoint(
                IExtensionPoint.DisplayTest.class,
                () -> new IExtensionPoint.DisplayTest(() -> IExtensionPoint.DisplayTest.IGNORESERVERONLY, (a, b) -> true));

        this.dataDirectory = FMLPaths.CONFIGDIR.get().resolve(MOD_ID);
        Config.getInstance().loadConfig(this.dataDirectory, this.logger);

        int threadPoolSize = Config.getInstance().getCoreThreadPoolSize();
        this.executorService = Executors.newFixedThreadPool(Math.max(1, threadPoolSize));
        this.eventHub = new EventHub(this, this.logger);
        this.eventHub.initBindingManager();

        MinecraftForge.EVENT_BUS.register(this);
    }

    @SubscribeEvent
    public void onServerStarted(ServerStartedEvent event) {
        this.server = event.getServer();
        this.logger.info("[ChatHub] Server started, initializing adaptors");
        if (this.adaptors.isEmpty()) {
            this.initAdaptors();
        }
        this.restartAdaptors();
    }

    @SubscribeEvent
    public void onServerStopping(ServerStoppingEvent event) {
        this.stopAdaptors();
    }

    @SubscribeEvent
    public void onRegisterCommands(RegisterCommandsEvent event) {
        new ForgeCommand(this).register(event.getDispatcher());
    }

    private void initAdaptors() {
        this.playerManager = new ForgePlayerManager();
        this.forgeAdaptor = new ForgeAdaptor(this, new ForgeFormatter());
        MinecraftForge.EVENT_BUS.register(this.forgeAdaptor);
        this.adaptors.add(this.forgeAdaptor);

        boolean qqEnabled = Config.getInstance().isQQEnabled();
        if (qqEnabled) {
            if (Config.getInstance().getQQAppId() != null && !Config.getInstance().getQQAppId().isEmpty()) {
                QQBotAPI qqBotApi = new QQBotAPI(this, Config.getInstance().getQQAppId(), Config.getInstance().getQQAppSecret());
                QQBotAdapter qqBotAdapter = new QQBotAdapter(this, new QQBotFormatter(), qqBotApi);
                this.adaptors.add(qqBotAdapter);
            }
            if (Config.getInstance().getQQWsReversePort() != null && Config.getInstance().getQQWsReversePort() > 0) {
                QQAPI qqApi = new QQAPI(this);
                QQAdaptor qqAdaptor = new QQAdaptor(this, new QQFormatter(), qqApi);
                this.adaptors.add(qqAdaptor);
            }
        }
    }

    private void restartAdaptors() {
        for (IAdaptor<? extends IFormatter> adaptor : this.adaptors) {
            this.executorService.submit(adaptor::restart);
        }
    }

    private void stopAdaptors() {
        for (IAdaptor<? extends IFormatter> adaptor : this.adaptors) {
            try {
                adaptor.stop();
            } catch (Exception e) {
                this.logger.error("[ChatHub] Failed to stop adaptor " + adaptor.getPlatform().getName(), e);
            }
        }
    }

    /** Reloads config and bindings, then restarts all adaptors. */
    public void reload() {
        this.stopAdaptors();
        if (this.forgeAdaptor != null) {
            MinecraftForge.EVENT_BUS.unregister(this.forgeAdaptor);
        }
        Config.getInstance().reloadConfig();
        this.eventHub.reloadBindingManager();
        this.adaptors.clear();
        this.initAdaptors();
        this.forgeAdaptor.setServer(this.server);
        this.restartAdaptors();
        this.logger.info("[ChatHub] Reloaded config and restarted adaptors");
    }

    public Logger getLogger() {
        return this.logger;
    }

    public Path getDataDirectory() {
        return this.dataDirectory;
    }

    public ExecutorService getExecutorService() {
        return this.executorService;
    }

    public EventHub getEventHub() {
        return this.eventHub;
    }

    public List<IAdaptor<? extends IFormatter>> getAdaptors() {
        return this.adaptors;
    }

    public ForgeFormatter getForgeFormatter() {
        return (ForgeFormatter) this.forgeAdaptor.getFormatter();
    }

    public ForgePlayerManager getPlayerManager() {
        return this.playerManager;
    }
}