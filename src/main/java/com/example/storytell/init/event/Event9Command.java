// Event9Command.java
package com.example.storytell.init.event;

import com.example.storytell.init.star.CustomStar;
import com.example.storytell.init.star.StarManager;
import com.mojang.brigadier.CommandDispatcher;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;

import java.util.Collection;

public class Event9Command {

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("event9")
                .requires(source -> source.hasPermission(2))
                .executes(context -> {
                    return executeEvent9(context.getSource());
                }));
    }

    private static int executeEvent9(CommandSourceStack source) {
        try {
            MinecraftServer server = source.getServer();
            Collection<ServerPlayer> players = server.getPlayerList().getPlayers();

            // 1. Немедленно проигрываем звук event9 для всех игроков
            executeSoundEvent9(server, players);

            // 2. Через 5 тиков запускаем тряску камеры на 8 секунд (160 тиков)
            scheduleDelayedTask(server, 5, () -> {
                executeScreenShakeCommand(server, "screenshake @a 1 160");
            });

            // 3. Через 10 тиков начинаем плавное изменение цвета звезды core на красный (300 тиков = 15 секунд)
            scheduleDelayedTask(server, 10, () -> {
                executeStarColorAnimation(server);
            });

            source.sendSuccess(() -> Component.literal("Event9 started!"), true);
            return 1;

        } catch (Exception e) {
            source.sendFailure(Component.literal("Error starting event9: " + e.getMessage()));
            e.printStackTrace();
            return 0;
        }
    }

    private static void executeSoundEvent9(MinecraftServer server, Collection<ServerPlayer> players) {
        try {
            CommandSourceStack commandSource = server.createCommandSourceStack()
                    .withPermission(4)
                    .withSuppressedOutput();

            for (ServerPlayer player : players) {
                String soundCommand = String.format(
                        "execute as %s at %s run playsound storytell:event9 master %s ~ ~ ~ 9999999999999.0 1.0",
                        player.getScoreboardName(),
                        player.getScoreboardName(),
                        player.getScoreboardName()
                );
                server.getCommands().performPrefixedCommand(commandSource, soundCommand);
            }
            System.out.println("Event9: Sound played successfully");
        } catch (Exception e) {
            System.err.println("Event9: Error playing sound: " + e.getMessage());
        }
    }

    private static void executeScreenShakeCommand(MinecraftServer server, String command) {
        try {
            CommandSourceStack commandSource = server.createCommandSourceStack()
                    .withPermission(4)
                    .withSuppressedOutput();

            int result = server.getCommands().performPrefixedCommand(commandSource, command);
            if (result > 0) {
                System.out.println("Event9: Screen shake command executed: " + command);
            } else {
                System.err.println("Event9: Screen shake command failed: " + command);
            }
        } catch (Exception e) {
            System.err.println("Event9: Error executing screen shake command: " + e.getMessage());
        }
    }

    private static void executeStarColorAnimation(MinecraftServer server) {
        try {
            // Находим звезду core (вместо blue_star)
            CustomStar coreStar = StarManager.getStarByName("core");
            if (coreStar != null) {
                // Сохраняем исходный цвет
                final int originalColor = coreStar.getColor();

                // Начинаем плавное изменение цвета на красный (0xFFFF0000 - красный) в течение 300 тиков (15 секунд)
                StarManager.startStarColorAnimation("core", 0xFFFF0000, 300);

                // Отправляем пакет всем клиентам
                StarManager.sendColorAnimationUpdate("core", 0xFFFF0000, 300);

                System.out.println("Event9: Started color animation for core to red over 15 seconds");

                // Через 310 тиков (15 секунд + небольшая задержка) скрываем core и показываем core_2
                scheduleDelayedTask(server, 310, () -> {
                    // Скрываем звезду core
                    StarManager.setStarVisibility("core", false);
                    StarManager.sendVisibilityUpdate("core", false, false, false);

                    // Показываем звезду core_2
                    StarManager.setStarVisibility("core_2", true);
                    StarManager.sendVisibilityUpdate("core_2", true, false, false);

                    System.out.println("Event9: Hidden core, shown core_2 after 15 seconds");
                });

            } else {
                System.err.println("Event9: core not found");
            }
        } catch (Exception e) {
            System.err.println("Event9: Error starting star color animation: " + e.getMessage());
        }
    }

    private static void scheduleDelayedTask(MinecraftServer server, int delayTicks, Runnable task) {
        new Thread(() -> {
            try {
                // Преобразуем тики в миллисекунды (1 тик = 50 мс)
                Thread.sleep(delayTicks * 50L);
                // Выполняем задачу в основном потоке сервера
                server.execute(task);
            } catch (InterruptedException e) {
                System.err.println("Event9: Delay thread interrupted: " + e.getMessage());
            }
        }).start();
    }
}