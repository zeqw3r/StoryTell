// Event3Command.java
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

public class Event3Command {

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("event3")
                .requires(source -> source.hasPermission(2))
                .then(Commands.argument("target", EntityArgument.players())
                        .executes(context -> {
                            Collection<ServerPlayer> targets = EntityArgument.getPlayers(context, "target");
                            return executeEvent3(context.getSource(), targets);
                        })));
    }

    private static int executeEvent3(CommandSourceStack source, Collection<ServerPlayer> targets) {
        int successCount = 0;
        MinecraftServer server = source.getServer();

        for (ServerPlayer player : targets) {
            try {
                final String playerName = player.getScoreboardName();
                final CommandSourceStack commandSourceStack = server.createCommandSourceStack()
                        .withSuppressedOutput()
                        .withPermission(4);

                final CommandDispatcher<CommandSourceStack> commandDispatcher = server.getCommands().getDispatcher();

                // 1. Проигрываем звук event3
                executeSoundCommand(server, commandSourceStack, commandDispatcher, playerName);

                // 2. Выполняем команду /repospawn set true
                executeRepospawnCommand(server, commandSourceStack, commandDispatcher);

            } catch (Exception e) {
                e.printStackTrace();
            }
        }


        return successCount;
    }

    private static void executeSoundCommand(MinecraftServer server, CommandSourceStack commandSourceStack,
                                            CommandDispatcher<CommandSourceStack> commandDispatcher, String playerName) {
        try {
            String soundCommand = String.format("execute as %s at %s run playsound storytell:event3 master %s ~ ~ ~ 20.0",
                    playerName, playerName, playerName);
            ParseResults<CommandSourceStack> soundResults = commandDispatcher.parse(soundCommand, commandSourceStack);
            int soundResult = server.getCommands().performCommand(soundResults, soundCommand);

            if (soundResult > 0) {
                System.out.println("Event3: Successfully played sound for " + playerName);
            } else {
                System.err.println("Event3: Failed to play sound for " + playerName);
            }
        } catch (Exception e) {
            System.err.println("Event3: Error playing sound for player " + playerName + ": " + e.getMessage());
        }
    }

    private static void executeRepospawnCommand(MinecraftServer server, CommandSourceStack commandSourceStack,
                                                CommandDispatcher<CommandSourceStack> commandDispatcher) {
        try {
            String repospawnCommand = "repospawn set true";
            ParseResults<CommandSourceStack> repospawnResults = commandDispatcher.parse(repospawnCommand, commandSourceStack);
            int repospawnResult = server.getCommands().performCommand(repospawnResults, repospawnCommand);

            if (repospawnResult > 0) {
                System.out.println("Event3: Successfully executed repospawn command");
            } else {
                System.err.println("Event3: Failed to execute repospawn command");
            }
        } catch (Exception e) {
            System.err.println("Event3: Error executing repospawn command: " + e.getMessage());

            // Если команда не найдена, попробуем альтернативные варианты
            tryAlternativeRepospawnCommands(server, commandSourceStack, commandDispatcher);
        }
    }

    private static void tryAlternativeRepospawnCommands(MinecraftServer server, CommandSourceStack commandSourceStack,
                                                        CommandDispatcher<CommandSourceStack> commandDispatcher) {
        // Пробуем альтернативные варианты команды, если основная не сработала
        String[] alternativeCommands = {
                "execute run repospawn set true",
                "storytell:repospawn set true",
                "function storytell:repospawn_set_true"
        };

        for (String altCommand : alternativeCommands) {
            try {
                ParseResults<CommandSourceStack> altResults = commandDispatcher.parse(altCommand, commandSourceStack);
                int altResult = server.getCommands().performCommand(altResults, altCommand);

                if (altResult > 0) {
                    System.out.println("Event3: Successfully executed alternative command: " + altCommand);
                    return;
                }
            } catch (Exception ex) {
                System.err.println("Event3: Error with alternative command " + altCommand + ": " + ex.getMessage());
            }
        }

        System.err.println("Event3: All repospawn command attempts failed");
    }
}