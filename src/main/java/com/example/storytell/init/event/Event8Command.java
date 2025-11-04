// Event8Command.java
package com.example.storytell.init.event;

import com.example.storytell.init.network.NetworkHandler;
import com.example.storytell.init.network.RedSkyPacket;
import com.mojang.brigadier.CommandDispatcher;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraftforge.network.NetworkDirection;

import java.util.Collection;

public class Event8Command {

    // Переменная для указания моба (можно изменить на нужного)
    private static final String MOB_TO_SUMMON = "mowziesmobs:ferrous_wroughtnaut";

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("event8")
                .requires(source -> source.hasPermission(2))
                .executes(context -> {
                    return executeEvent8(context.getSource());
                }));
    }

    private static int executeEvent8(CommandSourceStack source) {
        try {
            MinecraftServer server = source.getServer();
            Collection<ServerPlayer> players = server.getPlayerList().getPlayers();

            // 1. Немедленно проигрываем звук event8 для всех игроков
            executeSoundEvent8(server, players);

            // 2. Немедленно делаем камеру красной на 2 секунды (40 тиков)
            executeRedSky(server, players);

            // 3. Немедленно запускаем тряску камеры
            executeScreenShakeCommand(server, "screenshake @a 1 40");

            // 4. Через 20 секунд (400 тиков) спавним мобов над игроками
            scheduleDelayedTask(server, 400, () -> {
                executeMobSpawning(server, players);
            });

            source.sendSuccess(() -> Component.literal("Event8 started!"), true);
            return 1;

        } catch (Exception e) {
            source.sendFailure(Component.literal("Error starting event8: " + e.getMessage()));
            e.printStackTrace();
            return 0;
        }
    }

    private static void executeSoundEvent8(MinecraftServer server, Collection<ServerPlayer> players) {
        try {
            CommandSourceStack commandSource = server.createCommandSourceStack()
                    .withPermission(4)
                    .withSuppressedOutput();

            for (ServerPlayer player : players) {
                String soundCommand = String.format(
                        "execute as %s at %s run playsound storytell:event8 master %s ~ ~ ~ 20.0 1.0",
                        player.getScoreboardName(),
                        player.getScoreboardName(),
                        player.getScoreboardName()
                );
                server.getCommands().performPrefixedCommand(commandSource, soundCommand);
            }
            System.out.println("Event8: Sound played successfully");
        } catch (Exception e) {
            System.err.println("Event8: Error playing sound: " + e.getMessage());
        }
    }

    private static void executeRedSky(MinecraftServer server, Collection<ServerPlayer> players) {
        try {
            for (ServerPlayer player : players) {
                NetworkHandler.INSTANCE.sendTo(
                        new RedSkyPacket(true, 40), // 40 тиков = 2 секунды
                        player.connection.connection,
                        NetworkDirection.PLAY_TO_CLIENT
                );
            }
            System.out.println("Event8: Red sky effect applied");

            // Отключаем эффект через 2 секунды
            scheduleDelayedTask(server, 40, () -> {
                for (ServerPlayer player : players) {
                    NetworkHandler.INSTANCE.sendTo(
                            new RedSkyPacket(false, 0),
                            player.connection.connection,
                            NetworkDirection.PLAY_TO_CLIENT
                    );
                }
                System.out.println("Event8: Red sky effect removed");
            });

        } catch (Exception e) {
            System.err.println("Event8: Error applying red sky effect: " + e.getMessage());
        }
    }

    private static void executeScreenShakeCommand(MinecraftServer server, String command) {
        try {
            CommandSourceStack commandSource = server.createCommandSourceStack()
                    .withPermission(4)
                    .withSuppressedOutput();

            int result = server.getCommands().performPrefixedCommand(commandSource, command);
            if (result > 0) {
                System.out.println("Event8: Screen shake command executed: " + command);
            } else {
                System.err.println("Event8: Screen shake command failed: " + command);
            }
        } catch (Exception e) {
            System.err.println("Event8: Error executing screen shake command: " + e.getMessage());
        }
    }

    private static void executeMobSpawning(MinecraftServer server, Collection<ServerPlayer> players) {
        try {
            CommandSourceStack commandSource = server.createCommandSourceStack()
                    .withPermission(4)
                    .withSuppressedOutput();

            for (ServerPlayer player : players) {
                // Получаем координаты игрока и добавляем +50 к Y
                int x = (int) player.getX();
                int y = (int) player.getY() + 50;
                int z = (int) player.getZ();

                String summonCommand = String.format(
                        "summon %s %d %d %d",
                        MOB_TO_SUMMON, x, y, z
                );

                int result = server.getCommands().performPrefixedCommand(commandSource, summonCommand);
                if (result > 0) {
                    System.out.println("Event8: Mob spawned for player " + player.getScoreboardName());
                } else {
                    System.err.println("Event8: Mob spawn failed for player " + player.getScoreboardName());
                }
            }
        } catch (Exception e) {
            System.err.println("Event8: Error spawning mobs: " + e.getMessage());
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
                System.err.println("Event8: Delay thread interrupted: " + e.getMessage());
            }
        }).start();
    }
}