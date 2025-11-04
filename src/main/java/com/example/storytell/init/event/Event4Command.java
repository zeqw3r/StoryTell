// Event4Command.java
package com.example.storytell.init.event;

import com.example.storytell.init.network.*;
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
import net.minecraftforge.network.PacketDistributor;

public class Event4Command {

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("event4")
                .requires(source -> source.hasPermission(2))
                .then(Commands.argument("pos", Vec3Argument.vec3())
                        .executes(context -> {
                            Vec3 position = Vec3Argument.getVec3(context, "pos");
                            return executeEvent4(context.getSource(), position);
                        })));
    }

    private static int executeEvent4(CommandSourceStack source, Vec3 position) {
        try {
            MinecraftServer server = source.getServer();
            Level level = source.getLevel();

            // Получаем координаты из аргумента
            int sourceX = (int) position.x;
            int sourceY = (int) position.y;
            int sourceZ = (int) position.z;

            // 1. Создаем метеор с смещением +200 +400 +200 от указанных координат
            int meteorX = sourceX + 200;
            int meteorY = sourceY + 400;
            int meteorZ = sourceZ + 200;

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

            // 4. Через 10 секунд (200 тиков) проигрываем звук
            scheduleDelayedTask(server, 200, () -> {
                executeSoundCommand(server);
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
                executeExplosionSoundCommand(server, sourceX, sourceY, sourceZ);
                executeParticleCommand(server, sourceX, sourceY, sourceZ);
            });

            // 9. Через 1 секунду после взрыва (400 тиков) удаляем модель метеора
            scheduleDelayedTask(server, 395, () -> {
                RemoveWorldModelPacket removePacket = new RemoveWorldModelPacket("m");
                NetworkHandler.INSTANCE.send(PacketDistributor.ALL.noArg(), removePacket);
                System.out.println("Event4: Meteor model removed 1 second after explosion");
            });

            // 10. Через 1 тик (401 тик) создаем сундук на указанных координатах
            scheduleDelayedTask(server, 396, () -> {
                executeChestCommand(server, sourceX, sourceY, sourceZ);
            });

            source.sendSuccess(() -> Component.literal("Event4 started at coordinates " + sourceX + " " + sourceY + " " + sourceZ + "!"), true);
            return 1;

        } catch (Exception e) {
            source.sendFailure(Component.literal("Error starting event4: " + e.getMessage()));
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
                System.err.println("Event4: Delay thread interrupted: " + e.getMessage());
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
                System.out.println("Event4: Rotation command executed successfully");
            } else {
                System.err.println("Event4: Rotation command failed");
            }
        } catch (Exception e) {
            System.err.println("Event4: Error executing rotation command: " + e.getMessage());
        }
    }

    // Новый метод для выполнения команды частиц
    private static void executeParticleCommand(MinecraftServer server, int x, int y, int z) {
        try {
            CommandSourceStack commandSource = server.createCommandSourceStack()
                    .withPermission(4)
                    .withSuppressedOutput();

            // Заменяем ~ ~ ~ на конкретные координаты
            String particleCommand = String.format("particle minecraft:campfire_signal_smoke %d %d %d 1 1 1 0.1 1000 force", x, y, z);
            int result = server.getCommands().performPrefixedCommand(commandSource, particleCommand);

            if (result > 0) {
                System.out.println("Event4: Particle command executed successfully");
            } else {
                System.err.println("Event4: Particle command failed");
            }
        } catch (Exception e) {
            System.err.println("Event4: Error executing particle command: " + e.getMessage());
        }
    }

    private static void executeSoundCommand(MinecraftServer server) {
        try {
            // Создаем CommandSourceStack с правами для выполнения команды
            CommandSourceStack commandSource = server.createCommandSourceStack()
                    .withPermission(4)
                    .withSuppressedOutput();

            // Выполняем команду звука
            String soundCommand = "execute as @a at @a run playsound storytell:event4 master @a ~ ~ ~ 20";
            server.getCommands().performPrefixedCommand(commandSource, soundCommand);

            System.out.println("Event4: Sound command executed");
        } catch (Exception e) {
            System.err.println("Event4: Error executing sound command: " + e.getMessage());
        }
    }

    private static void executeExplosionSoundCommand(MinecraftServer server, int x, int y, int z) {
        try {
            CommandSourceStack commandSource = server.createCommandSourceStack()
                    .withPermission(4)
                    .withSuppressedOutput();

            // Звук взрыва в координатах взрыва с увеличенной громкостью в 5 раз
            String explosionSoundCommand = String.format("playsound minecraft:entity.generic.explode master @a %d %d %d 20.0 1.0", x, y, z);
            server.getCommands().performPrefixedCommand(commandSource, explosionSoundCommand);

            System.out.println("Event4: Explosion sound command executed with 5x volume");
        } catch (Exception e) {
            System.err.println("Event4: Error executing explosion sound command: " + e.getMessage());
        }
    }

    private static void createExplosion(Level level, int x, int y, int z) {
        try {
            // Создаем взрыв напрямую через API Minecraft для версии 1.20.1
            level.explode(
                    null, // Источник взрыва (null - нет источника)
                    (double) x, (double) y, (double) z, // Координаты
                    10.0f, // Сила взрыва
                    true, // Создает огонь
                    Level.ExplosionInteraction.TNT // Тип взрыва - как у TNT
            );

            System.out.println("Event4: Explosion created at " + x + " " + y + " " + z);
        } catch (Exception e) {
            System.err.println("Event4: Error creating explosion: " + e.getMessage());

            // Fallback: попробуем команду explode с правильным форматом
            try {
                CommandSourceStack commandSource = level.getServer().createCommandSourceStack()
                        .withPermission(4)
                        .withSuppressedOutput();

                String explodeCommand = String.format("explode %d %d %d 100 true", x, y, z);
                int result = level.getServer().getCommands().performPrefixedCommand(commandSource, explodeCommand);

                if (result > 0) {
                    System.out.println("Event4: Explosion command executed via fallback");
                } else {
                    System.err.println("Event4: Explosion command fallback also failed");
                }
            } catch (Exception ex) {
                System.err.println("Event4: Error in explosion fallback: " + ex.getMessage());
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
                            "{Count:1b,Slot:3b,id:\"minecraft:oak_planks\"}," +
                            "{Count:1b,Slot:4b,id:\"minecraft:netherite_ingot\"}," +
                            "{Count:1b,Slot:5b,id:\"minecraft:oak_planks\"}," +
                            "{Count:1b,Slot:12b,id:\"minecraft:oak_planks\"}," +
                            "{Count:1b,Slot:13b,id:\"minecraft:redstone\"}," +
                            "{Count:1b,Slot:14b,id:\"minecraft:oak_planks\"}," +
                            "{Count:1b,Slot:16b,id:\"storytell:radio\"}," +
                            "{Count:1b,Slot:21b,id:\"minecraft:oak_planks\"}," +
                            "{Count:1b,Slot:22b,id:\"minecraft:note_block\"}," +
                            "{Count:1b,Slot:23b,id:\"minecraft:oak_planks\"}" +
                            "]}", x, y, z);

            int result = server.getCommands().performPrefixedCommand(commandSource, chestCommand);

            if (result > 0) {
                System.out.println("Event4: Chest command executed successfully");
            } else {
                System.err.println("Event4: Chest command failed");
            }
        } catch (Exception e) {
            System.err.println("Event4: Error executing chest command: " + e.getMessage());
        }
    }

    private static void executeScreenShakeCommand(MinecraftServer server, String command) {
        try {
            CommandSourceStack commandSource = server.createCommandSourceStack()
                    .withPermission(4)
                    .withSuppressedOutput();

            int result = server.getCommands().performPrefixedCommand(commandSource, command);

            if (result > 0) {
                System.out.println("Event4: Screen shake command executed: " + command);
            } else {
                System.err.println("Event4: Screen shake command failed: " + command);
            }
        } catch (Exception e) {
            System.err.println("Event4: Error executing screen shake command: " + e.getMessage());
        }
    }
}