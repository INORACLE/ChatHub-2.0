package com.zhanganzhi.chathub.platforms.forge;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.zhanganzhi.chathub.ChatHubMod;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;

public class ForgeCommand {

    private final ChatHubMod chatHub;

    public ForgeCommand(ChatHubMod chatHub) {
        this.chatHub = chatHub;
    }

    public void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("chathub")
                .then(Commands.literal("reload")
                        .requires(source -> source.hasPermission(2))
                        .executes(this::reloadConfig))
                .then(Commands.literal("list")
                        .requires(source -> source.hasPermission(0))
                        .executes(this::listPlayers))
                .then(Commands.literal("msg")
                        .requires(source -> source.hasPermission(0))
                        .then(Commands.argument("player", StringArgumentType.string())
                                .then(Commands.argument("message", StringArgumentType.greedyString())
                                        .executes(this::sendMessage)))));
    }

    private int reloadConfig(CommandContext<CommandSourceStack> context) {
        try {
            this.chatHub.reload();
            context.getSource().sendSuccess(
                    () -> Component.literal("\u00a7aChatHub \u5df2\u91cd\u65b0\u52a0\u8f7d\u914d\u7f6e"), true);
        } catch (Exception e) {
            context.getSource().sendFailure(
                    Component.literal("\u00a7cChatHub \u91cd\u65b0\u52a0\u8f7d\u5931\u8d25: " + e.getMessage()));
        }
        return 1;
    }

    private int listPlayers(CommandContext<CommandSourceStack> context) {
        String result = this.chatHub.getForgeFormatter().formatListAll(this.chatHub.getPlayerManager());
        context.getSource().getServer().getPlayerList()
                .broadcastSystemMessage(Component.literal(result), false);
        return 1;
    }

    private int sendMessage(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        String target = StringArgumentType.getString(context, "player");
        String content = StringArgumentType.getString(context, "message");
        MinecraftServer server = context.getSource().getServer();
        ServerPlayer targetPlayer = server.getPlayerList().getPlayerByName(target);
        if (targetPlayer == null) {
            context.getSource().sendFailure(Component.literal("\u00a7c\u73a9\u5bb6 " + target + " \u4e0d\u5728\u7ebf"));
            return 0;
        }
        ServerPlayer sender = context.getSource().getPlayerOrException();
        var formatter = this.chatHub.getForgeFormatter();
        targetPlayer.sendSystemMessage(Component.literal(
                formatter.formatMsgTarget(sender.getGameProfile().getName(), content)));
        context.getSource().sendSuccess(() -> Component.literal(
                formatter.formatMsgSender(target, content)), false);
        return 1;
    }
}