package com.zhanganzhi.chathub.platforms.forge;

import com.zhanganzhi.chathub.ChatHubMod;
import com.zhanganzhi.chathub.core.adaptor.AbstractAdaptor;
import com.zhanganzhi.chathub.core.binding.BindingManagerV2;
import com.zhanganzhi.chathub.core.events.MessageEvent;
import com.zhanganzhi.chathub.core.events.ServerChangeEvent;
import com.zhanganzhi.chathub.platforms.Platform;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.event.ServerChatEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Forge adaptor that bridges between the in-game Minecraft chat and the event
 * hub. It attaches the sender's current dimension name to chat lines (the
 * {@code {dimension}} placeholder) and, in complete takeover mode, replaces
 * vanilla chat output with the configured formatted line.
 */
public class ForgeAdaptor extends AbstractAdaptor<ForgeFormatter> {

    private static final long VERIFY_TIMEOUT_MS = 120_000L;
    private static final long VERIFY_REMIND_INTERVAL_MS = 10_000L;

    private MinecraftServer server;
    private final BindingManagerV2 bindingManager;
    private final Map<UUID, VerificationStatus> pendingVerifications = new ConcurrentHashMap<>();
    private int tickCount;

    public ForgeAdaptor(ChatHubMod chatHub, ForgeFormatter formatter) {
        super(chatHub, Platform.MINECRAFT, formatter);
        this.bindingManager = this.chatHub.getEventHub().getBindingManager();
    }

    @Override
    public void start() {
        this.pendingVerifications.clear();
        this.tickCount = 0;
    }

    @Override
    public void stop() {
        this.pendingVerifications.clear();
    }

    public void setServer(MinecraftServer server) {
        this.server = server;
    }

    @Override
    public void sendPublicMessage(String message) {
        if (message == null || message.isEmpty()) {
            return;
        }
        if (this.server != null) {
            this.server.getPlayerList().broadcastSystemMessage(Component.literal(message), false);
        }
    }

    @SubscribeEvent
    public void onServerChat(ServerChatEvent event) {
        String content = event.getRawText();
        for (var pattern : this.config.getMinecraftIgnoreChatMessagePatterns()) {
            if (pattern.matcher(content).matches()) {
                return;
            }
        }
        String name = event.getUsername();
        ServerPlayer player = (ServerPlayer) event.getPlayer();
        String dimension = player.level().dimension().location().getPath();
        if (this.config.isCompleteTakeoverMode()) {
            event.setCanceled(true);
        }
        this.chatHub.getEventHub().onUserChat(
                new MessageEvent(Platform.MINECRAFT, this.config.getServerName(), dimension, name, content));
    }

    @SubscribeEvent
    public void onPlayerLoggedIn(PlayerEvent.PlayerLoggedInEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }
        String name = player.getGameProfile().getName();
        this.chatHub.getEventHub().onJoinServer(new ServerChangeEvent(name, null, this.config.getServerName()));
        this.maybeStartVerification(player);
    }

    @SubscribeEvent
    public void onPlayerLoggedOut(PlayerEvent.PlayerLoggedOutEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }
        String name = player.getGameProfile().getName();
        this.pendingVerifications.remove(player.getUUID());
        this.chatHub.getEventHub().onLeaveServer(new ServerChangeEvent(name, this.config.getServerName(), null));
    }

    @SubscribeEvent
    public void onServerTick(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) {
            return;
        }
        this.tickCount++;
        if (this.tickCount % 20 != 0) {
            return;
        }
        this.tickVerifications();
    }

    private void maybeStartVerification(ServerPlayer player) {
        if (!this.config.isQQEnableBinding()) {
            return;
        }
        String name = player.getGameProfile().getName();
        if (this.bindingManager.isMcNameBound(name)) {
            return;
        }
        String code = this.bindingManager.generateVerificationCode(name);
        this.pendingVerifications.put(player.getUUID(), new VerificationStatus(code, System.currentTimeMillis(), 0L));
        this.sendVerifyMessage(player, code);
    }

    private void sendVerifyMessage(ServerPlayer player, String code) {
        player.sendSystemMessage(Component.literal(
                "\u00a7e\u00a7l[ChatHub]\u00a7r QQ\u7ec4\u7ec7\u7ed1\u5b9a\u9a8c\u8bc1\u7801: \u00a76"
                        + code + "\u00a7r\uff0c\u8bf7\u5728\u7ec4\u5185\u53d1\u9001\u00a7b/chat\u00a7r "
                        + code + "\u00a7r\u5b8c\u6210\u7ed1\u5b9a\uff08\u00a7c120\u00a7r \u79d2\u540e\u81ea\u52a8\u62d2\u7edd\u8fdb\u5165\uff09"));
    }

    private void tickVerifications() {
        long now = System.currentTimeMillis();
        for (Map.Entry<UUID, VerificationStatus> entry : this.pendingVerifications.entrySet()) {
            VerificationStatus status = entry.getValue();
            ServerPlayer player = this.server != null ? this.server.getPlayerList().getPlayer(entry.getKey()) : null;
            String name = player != null ? player.getGameProfile().getName() : null;
            if (this.bindingManager.isMcNameBound(name)) {
                this.pendingVerifications.remove(entry.getKey());
                continue;
            }
            if (now - status.startTime > VERIFY_TIMEOUT_MS) {
                this.pendingVerifications.remove(entry.getKey());
                if (player != null) {
                    player.connection.disconnect(Component.literal("\u7ed1\u5b9a\u8d85\u65f6\uff0c\u8bf7\u91cd\u65b0\u52a0\u5165\u5e76\u5b8c\u6210\u9a8c\u8bc1"));
                }
                continue;
            }
            if (now - status.lastRemindTime > VERIFY_REMIND_INTERVAL_MS) {
                status.lastRemindTime = now;
                if (player != null) {
                    this.sendVerifyMessage(player, status.code);
                }
            }
        }
    }

    // In-game join/leave messages are already shown by vanilla; relaying them
    // here too would duplicate them, so they are intentionally not re-broadcast
    // in-game (they still reach other platforms through the event hub).
    @Override
    public void onJoinServer(ServerChangeEvent event) {
    }

    @Override
    public void onLeaveServer(ServerChangeEvent event) {
    }

    private static class VerificationStatus {
        final String code;
        final long startTime;
        long lastRemindTime;

        VerificationStatus(String code, long startTime, long lastRemindTime) {
            this.code = code;
            this.startTime = startTime;
            this.lastRemindTime = lastRemindTime;
        }
    }
}