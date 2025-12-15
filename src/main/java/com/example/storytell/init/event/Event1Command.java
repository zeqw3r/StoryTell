// Event1Command.java
package com.example.storytell.init.event;

import com.example.storytell.init.network.NetworkHandler;
import com.example.storytell.init.network.RedSkyPacket;
import com.example.storytell.init.star.StarManager;
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

        // Принудительная инициализация StarManager перед выполнением команды
        if (!StarManager.isInitialized()) {
            System.out.println("Event1: StarManager not initialized, initializing now...");
            StarManager.init();
        }

        for (ServerPlayer player : targets) {
            try {
                final String playerName = player.getScoreboardName();
                final CommandSourceStack commandSourceStack = server.createCommandSourceStack()
                        .withSuppressedOutput()
                        .withPermission(4);

                final CommandDispatcher<CommandSourceStack> commandDispatcher = server.getCommands().getDispatcher();

                // Проверяем, что звезда blue_star существует
                if (StarManager.getStarByName("blue_star") == null) {
                    System.err.println("Event1: CRITICAL - blue_star still not found after initialization!");
                    source.sendFailure(Component.literal("Error: star blue_star not found"));
                    return 0;
                }

                // 0. Сделать звезду видимой немедленно
                scheduleDelayedTask(server, 0, () -> {
                    StarManager.setStarVisibility("blue_star", true);
                    System.out.println("Event1: Made star blue_star visible for player " + playerName);
                });

                // 1. Look up command (immediate)
                scheduleDelayedTask(server, 0, () -> {
                    executeLookCommand(server, commandSourceStack, commandDispatcher, playerName, -85);
                });

                // 2. Screen shake after 2 ticks
                scheduleDelayedTask(server, 2, () -> {
                    executeShakeCommand(server, commandSourceStack, commandDispatcher, playerName);
                });

                // 3. Red sky after 4 ticks
                scheduleDelayedTask(server, 4, () -> {
                    executeRedSky(server, player);
                });

                // 4. Move star after 6 ticks
                scheduleDelayedTask(server, 6, () -> {
                    executeStarMovementInitial(server);
                });

                // 5. Camera breath after 8 ticks
                scheduleDelayedTask(server, 8, () -> {
                    executeCameraBreath(server, commandSourceStack, commandDispatcher, playerName);
                });

                // 6. Play sound after 10 ticks
                scheduleDelayedTask(server, 10, () -> {
                    executeSoundCommand(server, commandSourceStack, commandDispatcher, playerName);
                });

                // 7. Return star to original position after 17 seconds (340 ticks) from last command
                // 10 ticks (last command) + 340 ticks = 350 ticks total
                scheduleDelayedTask(server, 350, () -> {
                    executeStarMovementReturn(server);
                });

                System.out.println("Event1: Scheduled all effects for player " + playerName);
                successCount++;

            } catch (Exception e) {
                System.err.println("Event1: Error scheduling effects for player: " + e.getMessage());
                e.printStackTrace();
            }
        }

        if (successCount > 0) {
            source.sendSuccess(() -> Component.literal("Started event1 for   players"), true);
        }

        return successCount;
    }

    private static void scheduleDelayedTask(MinecraftServer server, int delayTicks, Runnable task) {
        new Thread(() -> {
            try {
                // Преобразуем тики в миллисекунды (1 тик = 50 мс)
                Thread.sleep(delayTicks * 50L);

                // Выполняем задачу в основном потоке сервера
                server.execute(task);
            } catch (InterruptedException e) {
                System.err.println("Event1: Delay thread interrupted: " + e.getMessage());
            }
        }).start();
    }

    private static void executeLookCommand(MinecraftServer server, CommandSourceStack commandSourceStack,
                                           CommandDispatcher<CommandSourceStack> commandDispatcher, String playerName, float angle) {
        try {
            String lookCommand = String.format(Locale.ROOT,
                    "execute as %s at %s run teleport %s ~ ~ ~ ~ %s",
                    playerName, playerName, playerName, angle);

            ParseResults<CommandSourceStack> lookResults = commandDispatcher.parse(lookCommand, commandSourceStack);
            int lookResult = server.getCommands().performCommand(lookResults, lookCommand);

        } catch (Exception e) {
            System.err.println("Event1: Error executing look command for " + playerName + ": " + e.getMessage());
        }
    }

    private static void executeShakeCommand(MinecraftServer server, CommandSourceStack commandSourceStack,
                                            CommandDispatcher<CommandSourceStack> commandDispatcher, String playerName) {
        try {
            String shakeCommand = String.format("screenshake %s 1.1 345", playerName);
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
            System.err.println("Event1: Error executing red sky: " + e.getMessage());
        }
    }

    private static void executeStarMovementInitial(MinecraftServer server) {
        try {
            // Перемещаем звезду в новые координаты: RA=45.0f, Dec=90.0f, Distance=20.0f
            StarManager.applyAbsolutePosition("blue_star", 45.0f, 90.0f, 20.0f, 340);

            System.out.println("Event1: Star moved to new position and synchronized with all clients");
        } catch (Exception e) {
            System.err.println("Event1: Error moving star to initial position: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private static void executeStarMovementReturn(MinecraftServer server) {
        try {
            // Возвращаем к исходным координатам: RA=45.0f, Dec=30.0f, Distance=150.0f
            StarManager.applyAbsolutePosition("blue_star", 45.0f, 30.0f, 150.0f, 1);

            System.out.println("Event1: Star returned to original position and synchronized with all clients");
        } catch (Exception e) {
            System.err.println("Event1: Error returning star to original position: " + e.getMessage());
            e.printStackTrace();
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