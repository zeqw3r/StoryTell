// Event2Command.java
package com.example.storytell.init.event;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.ParseResults;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;

import java.util.Collection;
import java.util.Locale;

public class Event2Command {

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("event2")
                .requires(source -> source.hasPermission(2))
                .then(Commands.argument("target", EntityArgument.players())
                        .executes(context -> {
                            Collection<ServerPlayer> targets = EntityArgument.getPlayers(context, "target");
                            return executeEvent2(context.getSource(), targets);
                        })));
    }

    private static int executeEvent2(CommandSourceStack source, Collection<ServerPlayer> targets) {
        int successCount = 0;
        MinecraftServer server = source.getServer();



        for (ServerPlayer player : targets) {
            try {
                final String playerName = player.getScoreboardName();
                final CommandSourceStack commandSourceStack = server.createCommandSourceStack()
                        .withSuppressedOutput()
                        .withPermission(4);

                final CommandDispatcher<CommandSourceStack> commandDispatcher = server.getCommands().getDispatcher();

                // 1. Добавляем тег игроку для идентификации в функциях
                String tagCommand = String.format("tag %s add event2_target", playerName);
                ParseResults<CommandSourceStack> tagResults = commandDispatcher.parse(tagCommand, commandSourceStack);
                server.getCommands().performCommand(tagResults, tagCommand);

                // 2. Запускаем основную функцию события
                String functionCommand = "function storytell:event2/start";
                ParseResults<CommandSourceStack> functionResults = commandDispatcher.parse(functionCommand, commandSourceStack);
                int functionResult = server.getCommands().performCommand(functionResults, functionCommand);
                successCount++;

                source.sendSuccess(() -> Component.literal("Started event2 for player: " + playerName), true);

            } catch (Exception e) {
                source.sendFailure(Component.literal("Failed to start event2 for player " + player.getScoreboardName() + ": " + e.getMessage()));
                e.printStackTrace();
            }
        }

        return successCount;
    }
}