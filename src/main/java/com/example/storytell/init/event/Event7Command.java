// Event7Command.java
package com.example.storytell.init.event;

import com.example.storytell.init.HologramConfig;
import com.example.storytell.init.ModSounds;
import com.example.storytell.init.blocks.HologramEntity;
import com.mojang.brigadier.CommandDispatcher;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.sounds.SoundSource;

public class Event7Command {

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("event7")
                .requires(source -> source.hasPermission(2))
                .executes(context -> {
                    return executeEvent7(context.getSource());
                }));
    }

    private static int executeEvent7(CommandSourceStack source) {
        try {
            MinecraftServer server = source.getServer();

            // Запускаем анимацию исчезновения всех голограмм
            for (ServerLevel level : server.getAllLevels()) {
                for (Entity e : level.getAllEntities()) {
                    if (e instanceof HologramEntity) {
                        ((HologramEntity) e).startDisappearing();
                    }
                }
            }

            // Устанавливаем текстуру извинения
            HologramConfig.setHologramTexture("storytell:textures/entity/sorry.png");

            // Возвращаем дефолтный ambient sound
            HologramConfig.setHologramAmbientSound("storytell:hologram_ambient");

            // Разблокируем возможность ввода текста и смены голограмм
            HologramConfig.setHologramLocked(false);

            // Через 1 секунду воспроизводим звук восстановления
            scheduleDelayedTask(server, 20, () -> {
                for (ServerLevel level : server.getAllLevels()) {
                    try {
                        level.playSound(null, 0, 64, 0,
                                ModSounds.HOLOGRAM_APPEAR.get(), SoundSource.BLOCKS, 0.5F, 1.0F);
                    } catch (IllegalStateException e) {
                        // Игнорируем ошибки воспроизведения звука
                    }
                }
            });

            return 1;

        } catch (Exception e) {
            return 0;
        }
    }

    private static void scheduleDelayedTask(MinecraftServer server, int delayTicks, Runnable task) {
        new Thread(() -> {
            try {
                Thread.sleep(delayTicks * 50L);
                server.execute(task);
            } catch (InterruptedException e) {
                // Игнорируем прерывание
            }
        }).start();
    }
}