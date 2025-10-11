// Event1Command.java
package com.example.storytell.init.event;

import com.example.storytell.init.network.NetworkHandler;
import com.example.storytell.init.network.RedSkyPacket;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.ParseResults;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraftforge.network.NetworkDirection;

import java.util.Collection;
import java.util.Locale;

public class Event1Command {

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("event1")
                .requires(source -> source.hasPermission(2))
                .then(Commands.argument("target", EntityArgument.players())
                        .executes(context -> {
                            Collection<ServerPlayer> targets = EntityArgument.getPlayers(context, "target");
                            return executeEvent1(context.getSource(), targets);
                        })));
    }

    private static int executeEvent1(CommandSourceStack source, Collection<ServerPlayer> targets) {
        int successCount = 0;
        MinecraftServer server = source.getServer();

        for (ServerPlayer player : targets) {
            try {
                final String playerName = player.getScoreboardName();
                final CommandSourceStack commandSourceStack = server.createCommandSourceStack()
                        .withSuppressedOutput()
                        .withPermission(4);

                final CommandDispatcher<CommandSourceStack> commandDispatcher = server.getCommands().getDispatcher();

                // Execute commands sequentially with delays
                server.execute(() -> {
                    // 1. Look up command (immediate)
                    executeLookCommand(server, commandSourceStack, commandDispatcher, playerName);

                    server.execute(() -> {
                        // 2. Screen shake after 2 ticks
                        executeShakeCommand(server, commandSourceStack, commandDispatcher, playerName);

                        server.execute(() -> {
                            // 3. Red sky after 4 ticks
                            executeRedSky(server, player);

                            server.execute(() -> {
                                // 4. Move star using star_move command after 6 ticks
                                executeStarMovement(server, commandSourceStack, commandDispatcher);

                                server.execute(() -> {
                                    // 5. Camera breath after 8 ticks
                                    executeCameraBreath(server, commandSourceStack, commandDispatcher, playerName);

                                    server.execute(() -> {
                                        // 6. Play sound after 10 ticks
                                        executeSoundCommand(server, commandSourceStack, commandDispatcher, playerName);
                                    });
                                });
                            });
                        });
                    });
                });

                System.out.println("Event1: Scheduled all effects for player " + playerName);
                successCount++;

            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        final int finalSuccessCount = successCount;
        return successCount;
    }

    private static void executeLookCommand(MinecraftServer server, CommandSourceStack commandSourceStack,
                                           CommandDispatcher<CommandSourceStack> commandDispatcher, String playerName) {
        try {
            String lookCommand = String.format(Locale.ROOT,
                    "execute as %s at %s run teleport %s ~ ~ ~ facing ~ 1000 ~",
                    playerName, playerName, playerName);

            ParseResults<CommandSourceStack> lookResults = commandDispatcher.parse(lookCommand, commandSourceStack);
            int lookResult = server.getCommands().performCommand(lookResults, lookCommand);

        } catch (Exception e) {
            System.err.println("Event1: Error executing look command for " + playerName + ": " + e.getMessage());
        }
    }

    private static void executeShakeCommand(MinecraftServer server, CommandSourceStack commandSourceStack,
                                            CommandDispatcher<CommandSourceStack> commandDispatcher, String playerName) {
        try {
            String shakeCommand = String.format("screenshake %s 1 345", playerName);
            ParseResults<CommandSourceStack> shakeResults = commandDispatcher.parse(shakeCommand, commandSourceStack);
            int shakeResult = server.getCommands().performCommand(shakeResults, shakeCommand);

        } catch (Exception e) {
            System.err.println("Event1: Error executing shake command for " + playerName + ": " + e.getMessage());
        }
    }

    private static void executeRedSky(MinecraftServer server, ServerPlayer player) {
        try {
            NetworkHandler.INSTANCE.sendTo(
                    new RedSkyPacket(true, 17),
                    player.connection.connection,
                    NetworkDirection.PLAY_TO_CLIENT
            );
        } catch (Exception e) {
        }
    }

    private static void executeStarMovement(MinecraftServer server, CommandSourceStack commandSourceStack,
                                            CommandDispatcher<CommandSourceStack> commandDispatcher) {
        try {
            // Move star using star_move command with specific coordinates: -92 -60 -92 for 1200 ticks (60 seconds)
            String starMoveCommand = "star_move blue_star -92 -60 -92 340";
            ParseResults<CommandSourceStack> starMoveResults = commandDispatcher.parse(starMoveCommand, commandSourceStack);
            int starMoveResult = server.getCommands().performCommand(starMoveResults, starMoveCommand);
        } catch (Exception e) {
            System.err.println("Event1: Error moving star: " + e.getMessage());
        }
    }

    private static void executeCameraBreath(MinecraftServer server, CommandSourceStack commandSourceStack,
                                            CommandDispatcher<CommandSourceStack> commandDispatcher, String playerName) {
        try {
            // Execute camera breath command with parameters: 1.0 intensity for 1200 ticks
            String cameraBreathCommand = String.format("camerabreath %s 1.0 340", playerName);
            ParseResults<CommandSourceStack> cameraBreathResults = commandDispatcher.parse(cameraBreathCommand, commandSourceStack);
            int cameraBreathResult = server.getCommands().performCommand(cameraBreathResults, cameraBreathCommand);
        } catch (Exception e) {
            System.err.println("Event1: Error executing camera breath command for " + playerName + ": " + e.getMessage());
        }
    }

    private static void executeSoundCommand(MinecraftServer server, CommandSourceStack commandSourceStack,
                                            CommandDispatcher<CommandSourceStack> commandDispatcher, String playerName) {
        try {
            CommandSourceStack delayedCommandSource = server.createCommandSourceStack()
                    .withSuppressedOutput()
                    .withPermission(4);

            String soundCommand = String.format("execute as %s at %s run playsound storytell:event1 master %s ~ ~ ~ 20.0", playerName, playerName, playerName);
            ParseResults<CommandSourceStack> soundResults = commandDispatcher.parse(soundCommand, delayedCommandSource);
            int soundResult = server.getCommands().performCommand(soundResults, soundCommand);
        } catch (Exception e) {
            System.err.println("Event1: Error playing sound for player " + playerName + ": " + e.getMessage());
        }
    }
}