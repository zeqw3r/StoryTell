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
import java.util.List;
import java.util.ArrayList;

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

        // Создаем список имен игроков для использования в командах
        List<String> playerNames = new ArrayList<>();
        for (ServerPlayer player : targets) {
            playerNames.add(player.getScoreboardName());
            // Синхронизируем звезды с каждым игроком
            com.example.storytell.init.star.StarManager.syncStarsToPlayer(player);
            successCount++;
        }

        if (playerNames.isEmpty()) {
            source.sendFailure(Component.literal("No target players found"));
            return 0;
        }

        // Создаем селектор для всех целевых игроков
        String playerSelector = String.join(",", playerNames);

        // 1. Немедленно проиграть звук (0 тиков)
        scheduleDelayedTask(server, 0, () -> {
            executeSoundCommand(server, playerSelector);
        });

        // 2. Серия screenshake с задержками
        // Первый screenshake через 1 секунду (20 тиков)
        scheduleDelayedTask(server, 20, () -> {
            executeScreenShakeCommand(server, playerSelector, 1.1f, 10);
        });

        // Второй screenshake через 3 секунды (60 тиков)
        scheduleDelayedTask(server, 60, () -> {
            executeScreenShakeCommand(server, playerSelector, 1.1f, 10);
        });

        // Третий screenshake через 5 секунд (100 тиков)
        scheduleDelayedTask(server, 100, () -> {
            executeScreenShakeCommand(server, playerSelector, 1.1f, 10);
        });

        // Четвертый screenshake через 7 секунд (140 тиков)
        scheduleDelayedTask(server, 140, () -> {
            executeScreenShakeCommand(server, playerSelector, 1.1f, 10);
        });

        // Пятый screenshake через 9 секунд (180 тиков)
        scheduleDelayedTask(server, 180, () -> {
            executeScreenShakeCommand(server, playerSelector, 1.1f, 10);
        });

        // Шестой screenshake через 11 секунд (220 тиков)
        scheduleDelayedTask(server, 220, () -> {
            executeScreenShakeCommand(server, playerSelector, 1.1f, 10);
        });

        // Седьмой screenshake через 13 секунд (260 тиков)
        scheduleDelayedTask(server, 260, () -> {
            executeScreenShakeCommand(server, playerSelector, 1.1f, 10);
        });

        // Восьмой screenshake через 15 секунд (300 тиков)
        scheduleDelayedTask(server, 300, () -> {
            executeScreenShakeCommand(server, playerSelector, 1.1f, 10);
        });

        // Финальный шаг: установка видимости звезды через 17 секунд (340 тиков)
        scheduleDelayedTask(server, 340, () -> {
            // Используем прямой вызов StarManager вместо команды
            com.example.storytell.init.star.StarManager.setStarVisibility("blue_star", true);

            // Дополнительная синхронизация звезд
            for (ServerPlayer player : targets) {
                com.example.storytell.init.star.StarManager.syncStarsToPlayer(player);
            }

            System.out.println("Event2: Star visibility set to true and synchronized");
        });

        source.sendSuccess(() -> Component.literal("Started event2 for  players"), true);
        return successCount;
    }

    private static void scheduleDelayedTask(MinecraftServer server, int delayTicks, Runnable task) {
        new Thread(() -> {
            try {
                Thread.sleep(delayTicks * 50L);
                server.execute(task);
            } catch (InterruptedException e) {
                System.err.println("Event2: Delay thread interrupted: " + e.getMessage());
                Thread.currentThread().interrupt();
            }
        }).start();
    }

    private static void executeSoundCommand(MinecraftServer server, String playerSelector) {
        try {
            CommandSourceStack commandSource = server.createCommandSourceStack()
                    .withPermission(4)
                    .withSuppressedOutput();

            // Используем правильный синтаксис для команды playsound
            String soundCommand = String.format("execute as %s at %s run playsound storytell:event2 master @s ~ ~ ~ 20.0",
                    playerSelector, playerSelector);

            // Используем парсинг команды как в Event1Command
            ParseResults<CommandSourceStack> parseResults = server.getCommands().getDispatcher().parse(soundCommand, commandSource);
            int result = server.getCommands().performCommand(parseResults, soundCommand);

            if (result > 0) {
                System.out.println("Event2: Sound command executed successfully for players: " + playerSelector);
            } else {
                System.err.println("Event2: Sound command failed - no players affected");
            }
        } catch (Exception e) {
            System.err.println("Event2: Error executing sound command: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private static void executeScreenShakeCommand(MinecraftServer server, String playerSelector, float intensity, int duration) {
        try {
            CommandSourceStack commandSource = server.createCommandSourceStack()
                    .withPermission(4)
                    .withSuppressedOutput();

            // Прямой вызов screenshake для выбранных игроков
            String shakeCommand = String.format("screenshake @a 1 10", playerSelector, intensity, duration);

            // Используем парсинг команды как в Event1Command
            ParseResults<CommandSourceStack> parseResults = server.getCommands().getDispatcher().parse(shakeCommand, commandSource);
            int result = server.getCommands().performCommand(parseResults, shakeCommand);

            if (result > 0) {
                System.out.println("Event2: Screen shake command executed for players: " + playerSelector);
            } else {
                System.err.println("Event2: Screen shake command failed - no players affected");
            }
        } catch (Exception e) {
            System.err.println("Event2: Error executing screen shake command: " + e.getMessage());
            e.printStackTrace();
        }
    }
}