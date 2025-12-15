// MeteorCommand.java
package com.example.storytell.init.event;

import com.example.storytell.init.network.*;
import com.example.storytell.init.ModSounds;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.coordinates.Vec3Argument;
import net.minecraft.world.phys.Vec3;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;
import net.minecraft.sounds.SoundSource;
import net.minecraftforge.network.PacketDistributor;

public class MeteorCommand {

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("meteor")
                .requires(source -> source.hasPermission(2))
                .then(Commands.argument("startPos", Vec3Argument.vec3())
                        .then(Commands.argument("endPos", Vec3Argument.vec3())
                                .then(Commands.argument("durationTicks", IntegerArgumentType.integer(1))
                                        .executes(context -> {
                                            Vec3 startPos = Vec3Argument.getVec3(context, "startPos");
                                            Vec3 endPos = Vec3Argument.getVec3(context, "endPos");
                                            int durationTicks = IntegerArgumentType.getInteger(context, "durationTicks");
                                            return executeMeteor(context.getSource(), startPos, endPos, durationTicks);
                                        })))));
    }

    private static int executeMeteor(CommandSourceStack source, Vec3 startPos, Vec3 endPos, int durationTicks) {
        try {
            MinecraftServer server = source.getServer();
            Level level = source.getLevel();

            // Получаем координаты из аргументов
            int startX = (int) startPos.x;
            int startY = (int) startPos.y;
            int startZ = (int) startPos.z;

            int endX = (int) endPos.x;
            int endY = (int) endPos.y;
            int endZ = (int) endPos.z;

            // Генерируем уникальный ID для метеорита
            String meteorId = "meteor_" + System.currentTimeMillis();

            // 1. Создаем метеор в начальной позиции
            AddWorldModelPacket addPacket = new AddWorldModelPacket(
                    meteorId,
                    new ResourceLocation("storytell", "star/meteor"),
                    startX, startY, startZ,
                    100, // размер
                    1    // модель
            );
            NetworkHandler.INSTANCE.send(PacketDistributor.ALL.noArg(), addPacket);

            // 2. Через 1 тик устанавливаем вращение метеорита
            scheduleDelayedTask(server, 1, () -> {
                executeRotationCommand(server, meteorId);
            });

            // 3. Через 1 тик запускаем движение метеорита к конечной позиции
            scheduleDelayedTask(server, 1, () -> {
                MoveWorldModelPacket movePacket = new MoveWorldModelPacket(
                        meteorId,
                        endX, endY, endZ,
                        durationTicks*50,
                        "easeInCubic"
                );
                NetworkHandler.INSTANCE.send(PacketDistributor.ALL.noArg(), movePacket);

                // Удаляем модель только после завершения движения
                scheduleDelayedTask(server, durationTicks+1, () -> {
                    RemoveWorldModelPacket removePacket = new RemoveWorldModelPacket(meteorId);
                    NetworkHandler.INSTANCE.send(PacketDistributor.ALL.noArg(), removePacket);
                });
            });

            // 4. Звук начинается за 9 секунд (180 тиков) до падения
            int soundDelay = durationTicks - 180;
            if (soundDelay > 0) {
                scheduleDelayedTask(server, soundDelay, () -> {
                    executeMeteorSound(level, endX, endY, endZ);
                });
            } else {
                // Если время полета меньше 9 секунд, запускаем звук сразу
                scheduleDelayedTask(server, 1, () -> {
                    executeMeteorSound(level, endX, endY, endZ);
                });
            }

            // 5. Взрыв появляется за 25 тиков до падения в конечной точке
            int explosionDelay = durationTicks - 1;
            if (explosionDelay > 0) {
                scheduleDelayedTask(server, explosionDelay, () -> {
                    createExplosion(level, endX, endY, endZ);
                    executeExplosionSound(level, endX, endY, endZ);
                    executeParticleCommand(server, endX, endY, endZ);
                });
            } else {
                // Если время полета меньше 25 тиков, взрыв сразу
                scheduleDelayedTask(server, 1, () -> {
                    createExplosion(level, endX, endY, endZ);
                    executeExplosionSound(level, endX, endY, endZ);
                    executeParticleCommand(server, endX, endY, endZ);
                });
            }

            return 1;

        } catch (Exception e) {
            source.sendFailure(Component.literal("Error launching meteor: " + e.getMessage()));
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
                System.err.println("Meteor: Delay thread interrupted: " + e.getMessage());
            }
        }).start();
    }

    // Метод для выполнения команды вращения
    private static void executeRotationCommand(MinecraftServer server, String meteorId) {
        try {
            CommandSourceStack commandSource = server.createCommandSourceStack()
                    .withPermission(4)
                    .withSuppressedOutput();

            String rotationCommand = "worldmodel rotation " + meteorId + " 1 1 1";
            int result = server.getCommands().performPrefixedCommand(commandSource, rotationCommand);

            if (result > 0) {
                System.out.println("Meteor: Rotation command executed successfully for " + meteorId);
            } else {
                System.err.println("Meteor: Rotation command failed for " + meteorId);
            }
        } catch (Exception e) {
            System.err.println("Meteor: Error executing rotation command: " + e.getMessage());
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
                System.out.println("Meteor: Particle command executed successfully");
            } else {
                System.err.println("Meteor: Particle command failed");
            }
        } catch (Exception e) {
            System.err.println("Meteor: Error executing particle command: " + e.getMessage());
        }
    }

    // Метод для воспроизведения звука метеорита
    private static void executeMeteorSound(Level level, int x, int y, int z) {
        try {
            // Воспроизводим звук на уровне для всех игроков
            level.playSound(null, x, y, z, ModSounds.EVENT4.get(), SoundSource.PLAYERS, 99999999.0F, 1.0F);
            System.out.println("Meteor: Sound played successfully at " + x + " " + y + " " + z);
        } catch (Exception e) {
            System.err.println("Meteor: Error playing sound: " + e.getMessage());
        }
    }

    // Метод для воспроизведения звука взрыва
    private static void executeExplosionSound(Level level, int x, int y, int z) {
        try {
            // Воспроизводим ванильный звук взрыва на уровне для всех игроков
            level.playSound(null, x, y, z,
                    net.minecraft.sounds.SoundEvents.GENERIC_EXPLODE,
                    SoundSource.BLOCKS, 20.0F, 1.0F);
            System.out.println("Meteor: Explosion sound played successfully");
        } catch (Exception e) {
            System.err.println("Meteor: Error playing explosion sound: " + e.getMessage());
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

            System.out.println("Meteor: Explosion created at " + x + " " + y + " " + z);
        } catch (Exception e) {
            System.err.println("Meteor: Error creating explosion: " + e.getMessage());

            // Fallback: попробуем команду explode с правильным форматом
            try {
                CommandSourceStack commandSource = level.getServer().createCommandSourceStack()
                        .withPermission(4)
                        .withSuppressedOutput();

                String explodeCommand = String.format("explode %d %d %d 100 true", x, y, z);
                int result = level.getServer().getCommands().performPrefixedCommand(commandSource, explodeCommand);

                if (result > 0) {
                    System.out.println("Meteor: Explosion command executed via fallback");
                } else {
                    System.err.println("Meteor: Explosion command fallback also failed");
                }
            } catch (Exception ex) {
                System.err.println("Meteor: Error in explosion fallback: " + ex.getMessage());
            }
        }
    }
}