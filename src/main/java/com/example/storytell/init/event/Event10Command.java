// Event10Command.java
package com.example.storytell.init.event;

import com.example.storytell.init.network.NetworkHandler;
import com.example.storytell.init.network.RedSkyPacket;
import com.mojang.brigadier.CommandDispatcher;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraftforge.network.NetworkDirection;

import java.util.Collection;

public class Event10Command {

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("event10")
                .requires(source -> source.hasPermission(2))
                .then(Commands.argument("target", EntityArgument.players())
                        .executes(context -> {
                            Collection<ServerPlayer> targets = EntityArgument.getPlayers(context, "target");
                            return executeEvent10(context.getSource(), targets);
                        })));
    }

    private static int executeEvent10(CommandSourceStack source, Collection<ServerPlayer> targets) {
        try {
            MinecraftServer server = source.getServer();

            // 1. Немедленно проигрываем звук event10 для выбранных игроков
            executeSoundEvent10(server, targets);

            // 2. Через 1 тик окрашиваем экран в красный (длительность 600 тиков = 30 секунд)
            scheduleDelayedTask(server, 1, () -> {
                executeRedSkyForPlayers(server, targets, 30);
            });

            // 3. Через 2 тика устанавливаем временные параметры title
            scheduleDelayedTask(server, 2, () -> {
                executeTitleTimes(server, targets);
            });

            // 4. Через 2 тика (за 1 тик до первого сообщения) - тряска экрана
            scheduleDelayedTask(server, 3, () -> {
                executeScreenShake(server, targets, 40);
            });

            // 5. Через 3 тика показываем первое сообщение
            scheduleDelayedTask(server, 4, () -> {
                executeTitleMessage(server, targets, "ВЫ СОВЕРШИЛИ", "ОШИБКУ");
            });

            // 6. Через 6 секунд (119 тиков) - тряска перед вторым сообщением
            scheduleDelayedTask(server, 119, () -> {
                executeScreenShake(server, targets, 40);
            });

            // 7. Через 6 секунд (120 тиков) показываем второе сообщение
            scheduleDelayedTask(server, 120, () -> {
                executeTitleMessage(server, targets, "ОНИ ИСПОЛЬЗОВАЛИ"," ВАС");
            });

            // 8. Через 12 секунд (239 тиков) - тряска перед третьим сообщением
            scheduleDelayedTask(server, 239, () -> {
                executeScreenShake(server, targets, 40);
            });

            // 9. Через 12 секунд (240 тиков) показываем третье сообщение
            scheduleDelayedTask(server, 240, () -> {
                executeTitleMessage(server, targets, "ОНИ ПРЕДАЛИ", "ВАС");
            });

            // 10. Через 18 секунд (359 тиков) - тряска перед четвертым сообщением
            scheduleDelayedTask(server, 359, () -> {
                executeScreenShake(server, targets, 40);
            });

            // 11. Через 18 секунд (360 тиков) показываем четвертое сообщение
            scheduleDelayedTask(server, 360, () -> {
                executeTitleMessage(server, targets, "НУЖНО ИХ", "ОСТАНОВИТЬ");
            });

            // 12. Через 24 секунды (479 тиков) - тряска перед пятым сообщением
            scheduleDelayedTask(server, 479, () -> {
                executeScreenShake(server, targets, 40);
            });

            // 13. Через 24 секунды (480 тиков) показываем пятое сообщение
            scheduleDelayedTask(server, 480, () -> {
                executeTitleMessage(server, targets, "КООРДИНАТЫ", "");
            });

            source.sendSuccess(() -> Component.literal("Event10 started for " + targets.size() + " players!"), true);
            return 1;

        } catch (Exception e) {
            source.sendFailure(Component.literal("Error starting event10: " + e.getMessage()));
            e.printStackTrace();
            return 0;
        }
    }

    private static void executeSoundEvent10(MinecraftServer server, Collection<ServerPlayer> targets) {
        try {
            CommandSourceStack commandSource = server.createCommandSourceStack()
                    .withPermission(4)
                    .withSuppressedOutput();

            for (ServerPlayer player : targets) {
                String soundCommand = String.format(
                        "execute as %s at %s run playsound storytell:event10 master %s ~ ~ ~ 9999999.0 1.0",
                        player.getScoreboardName(),
                        player.getScoreboardName(),
                        player.getScoreboardName()
                );
                server.getCommands().performPrefixedCommand(commandSource, soundCommand);
            }
            System.out.println("Event10: Sound played successfully");
        } catch (Exception e) {
            System.err.println("Event10: Error playing sound: " + e.getMessage());
        }
    }

    private static void executeRedSkyForPlayers(MinecraftServer server, Collection<ServerPlayer> targets, int duration) {
        try {
            for (ServerPlayer player : targets) {
                NetworkHandler.INSTANCE.sendTo(
                        new RedSkyPacket(true, duration),
                        player.connection.connection,
                        NetworkDirection.PLAY_TO_CLIENT
                );
            }
            System.out.println("Event10: Red sky effect applied for " + duration + " ticks");
        } catch (Exception e) {
            System.err.println("Event10: Error applying red sky: " + e.getMessage());
        }
    }

    private static void executeScreenShake(MinecraftServer server, Collection<ServerPlayer> targets, int duration) {
        try {
            CommandSourceStack commandSource = server.createCommandSourceStack()
                    .withPermission(4)
                    .withSuppressedOutput();

            for (ServerPlayer player : targets) {
                String shakeCommand = String.format("screenshake %s 1.0 %d",
                        player.getScoreboardName(), duration);
                server.getCommands().performPrefixedCommand(commandSource, shakeCommand);
            }
            System.out.println("Event10: Screen shake applied for " + duration + " ticks");
        } catch (Exception e) {
            System.err.println("Event10: Error applying screen shake: " + e.getMessage());
        }
    }

    private static void executeTitleTimes(MinecraftServer server, Collection<ServerPlayer> targets) {
        try {
            CommandSourceStack commandSource = server.createCommandSourceStack()
                    .withPermission(4)
                    .withSuppressedOutput();

            for (ServerPlayer player : targets) {
                String timesCommand = String.format("title %s times 1s 4s 1s",
                        player.getScoreboardName());
                server.getCommands().performPrefixedCommand(commandSource, timesCommand);
            }
            System.out.println("Event10: Title times set");
        } catch (Exception e) {
            System.err.println("Event10: Error setting title times: " + e.getMessage());
        }
    }

    private static void executeTitleMessage(MinecraftServer server, Collection<ServerPlayer> targets, String message, String message2) {
        try {
            CommandSourceStack commandSource = server.createCommandSourceStack()
                    .withPermission(4)
                    .withSuppressedOutput();
            for (ServerPlayer player : targets) {
                String timesCommand = String.format("title %s times 1s 4s 1s",
                        player.getScoreboardName());
                server.getCommands().performPrefixedCommand(commandSource, timesCommand);
            }
            for (ServerPlayer player : targets) {
                String titleCommand = String.format("title %s subtitle {\"text\":\"%s\",\"color\":\"white\",\"bold\":true}",
                        player.getScoreboardName(), message2);
                server.getCommands().performPrefixedCommand(commandSource, titleCommand);
            }
            for (ServerPlayer player : targets) {
                String titleCommand = String.format("title %s title {\"text\":\"%s\",\"color\":\"white\",\"bold\":false}",
                        player.getScoreboardName(), message);
                server.getCommands().performPrefixedCommand(commandSource, titleCommand);
            }
            System.out.println("Event10: Title message displayed: " + message);
        } catch (Exception e) {
            System.err.println("Event10: Error displaying title message: " + e.getMessage());
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
                System.err.println("Event10: Delay thread interrupted: " + e.getMessage());
            }
        }).start();
    }
}