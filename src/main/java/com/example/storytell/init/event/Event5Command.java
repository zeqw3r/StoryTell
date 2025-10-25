// Event5Command.java
package com.example.storytell.init.event;

import com.example.storytell.init.network.*;
import com.example.storytell.init.ModSounds;
import com.mojang.brigadier.CommandDispatcher;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.coordinates.Vec3Argument;
import net.minecraft.world.phys.Vec3;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.sounds.SoundSource;
import net.minecraftforge.network.PacketDistributor;

public class Event5Command {

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("event5")
                .requires(source -> source.hasPermission(2))
                .then(Commands.argument("pos", Vec3Argument.vec3())
                        .executes(context -> {
                            Vec3 position = Vec3Argument.getVec3(context, "pos");
                            return executeEvent5(context.getSource(), position);
                        })));
    }

    private static int executeEvent5(CommandSourceStack source, Vec3 position) {
        try {
            MinecraftServer server = source.getServer();
            Level level = source.getLevel();

            // Получаем координаты из аргумента
            int sourceX = (int) position.x;
            int sourceY = (int) position.y;
            int sourceZ = (int) position.z;

            // 1. Создаем метеор с смещением -250 +500 +300 от указанных координат
            int meteorX = sourceX - 250;
            int meteorY = sourceY + 500;
            int meteorZ = sourceZ + 300;

            // Создаем и отправляем пакет для добавления модели
            AddWorldModelPacket addPacket = new AddWorldModelPacket(
                    "m",
                    new ResourceLocation("storytell", "star/meteor"),
                    meteorX, meteorY, meteorZ,
                    100,
                    1
            );
            NetworkHandler.INSTANCE.send(PacketDistributor.ALL.noArg(), addPacket);

            // 2. Через 1 тик устанавливаем вращение метеорита
            scheduleDelayedTask(server, 1, () -> {
                executeRotationCommand(server);
            });

            // 3. Задержка 1 тик и перемещаем метеор к указанным координатам
            scheduleDelayedTask(server, 1, () -> {
                MoveWorldModelPacket movePacket = new MoveWorldModelPacket(
                        "m",
                        sourceX, sourceY, sourceZ,
                        20000, // 20 секунд
                        "easeInCubic"
                );
                NetworkHandler.INSTANCE.send(PacketDistributor.ALL.noArg(), movePacket);
            });

            // 4. Через 10 секунд (200 тиков) проигрываем звук через level.playSound
            scheduleDelayedTask(server, 200, () -> {
                executeTabletSound(level, sourceX, sourceY, sourceZ);
            });

            // 5. Через 5 секунд (100 тиков) screenshake @a 1 40
            scheduleDelayedTask(server, 100, () -> {
                executeScreenShakeCommand(server, "screenshake @a 1 20");
            });

            // 6. Через 11 секунд (220 тиков) screenshake @a 1 40
            scheduleDelayedTask(server, 220, () -> {
                executeScreenShakeCommand(server, "screenshake @a 1 20");
            });

            // 7. Через 17 секунд (340 тиков) screenshake @a 1 100
            scheduleDelayedTask(server, 340, () -> {
                executeScreenShakeCommand(server, "screenshake @a 1 20");
            });

            // 8. Через 19 секунд (380 тиков) создаем взрыв, звук и частицы
            scheduleDelayedTask(server, 394, () -> {
                createExplosion(level, sourceX, sourceY, sourceZ);
                executeExplosionSound(level, sourceX, sourceY, sourceZ);
                executeParticleCommand(server, sourceX, sourceY, sourceZ);
            });

            // 9. Через 1 секунду после взрыва (400 тиков) удаляем модель метеора
            scheduleDelayedTask(server, 395, () -> {
                RemoveWorldModelPacket removePacket = new RemoveWorldModelPacket("m");
                NetworkHandler.INSTANCE.send(PacketDistributor.ALL.noArg(), removePacket);
                System.out.println("Event5: Meteor model removed 1 second after explosion");
            });

            // 10. Через 1 тик (401 тик) создаем сундук на указанных координатах
            scheduleDelayedTask(server, 396, () -> {
                executeChestCommand(server, sourceX, sourceY, sourceZ);
            });

            source.sendSuccess(() -> Component.literal("Event5 started at coordinates " + sourceX + " " + sourceY + " " + sourceZ + "!"), true);
            return 1;

        } catch (Exception e) {
            source.sendFailure(Component.literal("Error starting event5: " + e.getMessage()));
            e.printStackTrace();
            return 0;
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
                System.err.println("Event5: Delay thread interrupted: " + e.getMessage());
            }
        }).start();
    }

    // Метод для выполнения команды вращения
    private static void executeRotationCommand(MinecraftServer server) {
        try {
            CommandSourceStack commandSource = server.createCommandSourceStack()
                    .withPermission(4)
                    .withSuppressedOutput();

            String rotationCommand = "worldmodel rotation m 1 1 1";
            int result = server.getCommands().performPrefixedCommand(commandSource, rotationCommand);

            if (result > 0) {
                System.out.println("Event5: Rotation command executed successfully");
            } else {
                System.err.println("Event5: Rotation command failed");
            }
        } catch (Exception e) {
            System.err.println("Event5: Error executing rotation command: " + e.getMessage());
        }
    }

    // Новый метод для выполнения команды частиц
    private static void executeParticleCommand(MinecraftServer server, int x, int y, int z) {
        try {
            CommandSourceStack commandSource = server.createCommandSourceStack()
                    .withPermission(4)
                    .withSuppressedOutput();

            // Заменяем ~ ~ ~ на конкретные координаты
            String particleCommand = String.format("particle minecraft:campfire_signal_smoke %d %d %d 5 5 5 1 10000 force", x, y, z);
            int result = server.getCommands().performPrefixedCommand(commandSource, particleCommand);

            if (result > 0) {
                System.out.println("Event5: Particle command executed successfully");
            } else {
                System.err.println("Event5: Particle command failed");
            }
        } catch (Exception e) {
            System.err.println("Event5: Error executing particle command: " + e.getMessage());
        }
    }

    // Метод для воспроизведения звука через level.playSound
    private static void executeTabletSound(Level level, int x, int y, int z) {
        try {
            // Воспроизводим звук на уровне для всех игроков
            level.playSound(null, x, y, z, ModSounds.EVENT4.get(), SoundSource.PLAYERS, 20.0F, 1.0F);
            System.out.println("Event5: Tablet sound played successfully");
        } catch (Exception e) {
            System.err.println("Event5: Error playing tablet sound: " + e.getMessage());
        }
    }

    // Метод для воспроизведения звука взрыва через level.playSound
    private static void executeExplosionSound(Level level, int x, int y, int z) {
        try {
            // Воспроизводим ванильный звук взрыва на уровне для всех игроков
            level.playSound(null, x, y, z,
                    net.minecraft.sounds.SoundEvents.GENERIC_EXPLODE,
                    SoundSource.BLOCKS, 20.0F, 1.0F);
            System.out.println("Event5: Explosion sound played successfully");
        } catch (Exception e) {
            System.err.println("Event5: Error playing explosion sound: " + e.getMessage());
        }
    }

    private static void createExplosion(Level level, int x, int y, int z) {
        try {
            // Создаем взрыв напрямую через API Minecraft для версии 1.20.1
            level.explode(
                    null, // Источник взрыва (null - нет источника)
                    (double) x, (double) y, (double) z, // Координаты
                    100.0f, // Сила взрыва
                    true, // Создает огонь
                    Level.ExplosionInteraction.TNT // Тип взрыва - как у TNT
            );

            System.out.println("Event5: Explosion created at " + x + " " + y + " " + z);
        } catch (Exception e) {
            System.err.println("Event5: Error creating explosion: " + e.getMessage());

            // Fallback: попробуем команду explode с правильным форматом
            try {
                CommandSourceStack commandSource = level.getServer().createCommandSourceStack()
                        .withPermission(4)
                        .withSuppressedOutput();

                String explodeCommand = String.format("explode %d %d %d 100 true", x, y, z);
                int result = level.getServer().getCommands().performPrefixedCommand(commandSource, explodeCommand);

                if (result > 0) {
                    System.out.println("Event5: Explosion command executed via fallback");
                } else {
                    System.err.println("Event5: Explosion command fallback also failed");
                }
            } catch (Exception ex) {
                System.err.println("Event5: Error in explosion fallback: " + ex.getMessage());
            }
        }
    }

    private static void executeChestCommand(MinecraftServer server, int x, int y, int z) {
        try {
            CommandSourceStack commandSource = server.createCommandSourceStack()
                    .withPermission(4)
                    .withSuppressedOutput();

            String chestCommand = String.format(
                    "setblock %d %d %d minecraft:chest[facing=west,type=single,waterlogged=false]{Items:[" +
                            "{Count:1b,Slot:4b,id:\"create:display_link\"}," +
                            "{Count:1b,Slot:12b,id:\"minecraft:copper_block\"}," +
                            "{Count:1b,Slot:13b,id:\"immersiveengineering:transformer_hv\"}," +
                            "{Count:1b,Slot:14b,id:\"minecraft:copper_block\"}," +
                            "{Count:1b,Slot:16b,id:\"storytell:upgrade_transmitter\"}," +
                            "{Count:1b,Slot:21b,id:\"minecraft:netherite_ingot\"}," +
                            "{Count:1b,Slot:22b,id:\"immersiveengineering:capacitor_hv\"}," +
                            "{Count:1b,Slot:23b,id:\"minecraft:netherite_ingot\"}" +
                            "]}", x, y, z);

            int result = server.getCommands().performPrefixedCommand(commandSource, chestCommand);

            if (result > 0) {
                System.out.println("Event5: Chest command executed successfully");
            } else {
                System.err.println("Event5: Chest command failed");
            }
        } catch (Exception e) {
            System.err.println("Event5: Error executing chest command: " + e.getMessage());
        }
    }

    private static void executeScreenShakeCommand(MinecraftServer server, String command) {
        try {
            CommandSourceStack commandSource = server.createCommandSourceStack()
                    .withPermission(4)
                    .withSuppressedOutput();

            int result = server.getCommands().performPrefixedCommand(commandSource, command);

            if (result > 0) {
                System.out.println("Event5: Screen shake command executed: " + command);
            } else {
                System.err.println("Event5: Screen shake command failed: " + command);
            }
        } catch (Exception e) {
            System.err.println("Event5: Error executing screen shake command: " + e.getMessage());
        }
    }
}