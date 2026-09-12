package com.zhanganzhi.chathub.platforms.forge;

import com.zhanganzhi.chathub.core.IPlayerManager;
import com.zhanganzhi.chathub.core.config.Config;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.server.ServerLifecycleHooks;

import java.util.List;

public class ForgePlayerManager implements IPlayerManager {

    @Override
    public String getServerName() {
        return Config.getInstance().getServerName();
    }

    @Override
    public List<String> getOnlinePlayerNames() {
        MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
        if (server == null) {
            return List.of();
        }
        return server.getPlayerList().getPlayers().stream()
                .map(player -> player.getGameProfile().getName())
                .toList();
    }

    @Override
    public void kickPlayer(String playerName, String reason) {
        MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
        if (server == null) {
            return;
        }
        ServerPlayer player = server.getPlayerList().getPlayerByName(playerName);
        if (player != null) {
            player.connection.disconnect(Component.literal(reason));
        }
    }
}