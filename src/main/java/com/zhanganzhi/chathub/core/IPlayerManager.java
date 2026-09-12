package com.zhanganzhi.chathub.core;

import java.util.List;

/**
 * Abstraction over the running Minecraft server so that platform adaptors
 * (e.g. the QQ adapter) do not depend on Minecraft/Forge classes directly.
 */
public interface IPlayerManager {

    /** Display name of the current (single) server. */
    String getServerName();

    /** Usernames of all online players. */
    List<String> getOnlinePlayerNames();

    /** Disconnect the given player if they are online. */
    void kickPlayer(String playerName, String reason);
}