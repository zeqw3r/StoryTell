// OrbitalStrikeHandler.java
package com.example.storytell.init.strike;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;

import java.util.Random;

public class OrbitalStrikeHandler {
    private static final Random RANDOM = new Random();
    private static final int MAX_WORLD_HEIGHT = 312;

    public static void startStrike(ServerLevel level, Vec3 targetPos) {
        // 5 повторений звуков и эффектов с задержкой 5 секунд
        for (int repetition = 0; repetition < 5; repetition++) {
            final int currentRep = repetition;
            new Thread(() -> {
                try {
                    Thread.sleep(5000 * (currentRep + 1)); // Задержка 5, 10, 15, 20, 25 секунд

                    level.getServer().execute(() -> {
                        // Звуки предупреждения
                        level.playSound(null, BlockPos.containing(targetPos),
                                SoundEvents.RESPAWN_ANCHOR_CHARGE, SoundSource.HOSTILE, 1.0F, 0.5F);

                        level.playSound(null, BlockPos.containing(targetPos),
                                SoundEvents.BEACON_ACTIVATE, SoundSource.HOSTILE, 1.0F, 1.0F);

                        // Эффекты на игроках в области
                        AABB effectArea = new AABB(targetPos, targetPos).inflate(20);
                        for (Entity entity : level.getEntities(null, effectArea)) {
                            if (entity instanceof LivingEntity living) {
                                // Эффекты замедления и дрожи
                                living.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 100, 2));
                                living.addEffect(new MobEffectInstance(MobEffects.CONFUSION, 100, 1));

                                // Визуальные эффекты вокруг игрока
                                for (int i = 0; i < 10; i++) {
                                    double offsetX = (RANDOM.nextDouble() - 0.5) * 3;
                                    double offsetY = RANDOM.nextDouble() * 2;
                                    double offsetZ = (RANDOM.nextDouble() - 0.5) * 3;

                                    level.sendParticles(new DustParticleOptions(
                                                    new Vector3f(1.0f, 0.5f, 0.0f), 1.5f),
                                            entity.getX() + offsetX,
                                            entity.getY() + offsetY,
                                            entity.getZ() + offsetZ,
                                            3, 0.1, 0.1, 0.1, 0.05);
                                }

                                // Выполняем команду screenshake для игроков в области
                                if (entity instanceof Player) {
                                    String command = "screenshake " + entity.getDisplayName().getString() + " 10 60";
                                    level.getServer().getCommands().performPrefixedCommand(
                                            level.getServer().createCommandSourceStack().withSuppressedOutput(),
                                            command
                                    );
                                }
                            }
                        }
                    });
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }).start();
        }

        // После 5 повторений (25 секунд) - лазер, взрыв, смерть
        new Thread(() -> {
            try {
                Thread.sleep(25000); // Общая задержка 25 секунд

                level.getServer().execute(() -> {
                    performFinalStrike(level, targetPos);
                });
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }).start();
    }

    private static void performFinalStrike(ServerLevel level, Vec3 targetPos) {
        // Создаем лазер с максимальной высоты
        createLaserBeam(level, targetPos);

        // Создаем взрыв
        createPowerfulExplosion(level, targetPos);

        // Убиваем всех игроков в области
        applyLethalDamage(level, targetPos);
    }

    private static void createLaserBeam(ServerLevel level, Vec3 targetPos) {
        Vec3 skyPos = new Vec3(targetPos.x, MAX_WORLD_HEIGHT, targetPos.z);

        // Разрушаем блоки на пути лазера
        destroyBlocksAlongLaser(level, skyPos, targetPos);

        // Создаем визуальный эффект лазера
        createLaserWithParticles(level, skyPos, targetPos);

        level.playSound(
                null,
                targetPos.x, targetPos.y, targetPos.z,
                net.minecraft.sounds.SoundEvents.LIGHTNING_BOLT_THUNDER,
                net.minecraft.sounds.SoundSource.HOSTILE,
                3.0F,
                0.5F
        );
    }

    private static void destroyBlocksAlongLaser(ServerLevel level, Vec3 start, Vec3 end) {
        Vec3 direction = end.subtract(start);
        double length = direction.length();
        direction = direction.normalize();

        int steps = (int) (length * 2); // Проверяем каждые 0.5 блока
        double stepSize = length / steps;

        for (int i = 0; i < steps; i++) {
            double progress = i * stepSize;
            Vec3 checkPos = start.add(direction.scale(progress));
            BlockPos blockPos = BlockPos.containing(checkPos);

            // Разрушаем блок, если он не является бедроком
            BlockState blockState = level.getBlockState(blockPos);
            if (!blockState.isAir() && blockState.getBlock() != Blocks.BEDROCK && blockState.getDestroySpeed(level, blockPos) >= 0) {
                level.destroyBlock(blockPos, true); // true - выпадают ли предметы

                // Создаем частицы разрушения
                level.sendParticles(
                        ParticleTypes.EXPLOSION,
                        checkPos.x, checkPos.y, checkPos.z,
                        3, 0.2, 0.2, 0.2, 0.05
                );
            }
        }
    }

    private static void createLaserWithParticles(ServerLevel level, Vec3 start, Vec3 end) {
        Vec3 direction = end.subtract(start);
        double length = direction.length();
        direction = direction.normalize();

        int particleCount = (int) (length * 20);
        double step = length / particleCount;

        for (int i = 0; i < particleCount; i++) {
            double progress = i * step;
            Vec3 particlePos = start.add(direction.scale(progress));

            level.sendParticles(
                    new DustParticleOptions(new Vector3f(1.0f, 0.0f, 0.0f), 2.0f),
                    particlePos.x, particlePos.y, particlePos.z,
                    5, 0.05, 0.05, 0.05, 0.0
            );

            if (i % 2 == 0) {
                level.sendParticles(
                        new DustParticleOptions(new Vector3f(1.0f, 1.0f, 1.0f), 1.5f),
                        particlePos.x, particlePos.y, particlePos.z,
                        3, 0.02, 0.02, 0.02, 0.0
                );
            }
        }

        createBeamEndEffects(level, start, end);
    }

    private static void createBeamEndEffects(ServerLevel level, Vec3 start, Vec3 end) {
        for (int i = 0; i < 25; i++) {
            double offsetX = (Math.random() - 0.5) * 2;
            double offsetY = (Math.random() - 0.5) * 2;
            double offsetZ = (Math.random() - 0.5) * 2;

            level.sendParticles(
                    new DustParticleOptions(new Vector3f(0.8f, 0.8f, 1.0f), 2.0f),
                    start.x + offsetX, start.y + offsetY, start.z + offsetZ,
                    3, 0.1, 0.1, 0.1, 0.05
            );
        }

        for (int i = 0; i < 40; i++) {
            double offsetX = (Math.random() - 0.5) * 3;
            double offsetY = Math.random() * 2;
            double offsetZ = (Math.random() - 0.5) * 3;

            level.sendParticles(
                    new DustParticleOptions(new Vector3f(1.0f, 0.3f, 0.1f), 2.0f),
                    end.x + offsetX, end.y + offsetY, end.z + offsetZ,
                    5, 0.1, 0.1, 0.1, 0.1
            );

            if (i % 3 == 0) {
                level.sendParticles(
                        ParticleTypes.FLAME,
                        end.x + offsetX * 0.5, end.y + offsetY * 0.5, end.z + offsetZ * 0.5,
                        2, 0.05, 0.05, 0.05, 0.02
                );
            }
        }
    }

    private static void createPowerfulExplosion(ServerLevel level, Vec3 center) {
        level.explode(
                null,
                center.x, center.y, center.z,
                15.0F,
                true,
                Level.ExplosionInteraction.BLOCK
        );

        level.sendParticles(net.minecraft.core.particles.ParticleTypes.EXPLOSION_EMITTER,
                center.x, center.y + 1, center.z, 15, 3, 3, 3, 0.3);

        level.sendParticles(net.minecraft.core.particles.ParticleTypes.FLAME,
                center.x, center.y + 1, center.z, 100, 4, 4, 4, 0.2);

        level.playSound(
                null,
                center.x, center.y, center.z,
                SoundEvents.GENERIC_EXPLODE,
                SoundSource.HOSTILE,
                10.0F,
                0.6F
        );

        level.playSound(
                null,
                center.x, center.y, center.z,
                SoundEvents.DRAGON_FIREBALL_EXPLODE,
                SoundSource.HOSTILE,
                8.0F,
                0.7F
        );
    }

    private static void applyLethalDamage(ServerLevel level, Vec3 center) {
        AABB damageArea = new AABB(center, center).inflate(10);
        for (Entity entity : level.getEntities(null, damageArea)) {
            if (entity instanceof LivingEntity living && !living.isInvulnerable()) {
                living.hurt(living.damageSources().generic(), 1000.0F);
                living.setSecondsOnFire(10);
                living.addEffect(new MobEffectInstance(MobEffects.WITHER, 100, 3));
            }
        }
    }
}