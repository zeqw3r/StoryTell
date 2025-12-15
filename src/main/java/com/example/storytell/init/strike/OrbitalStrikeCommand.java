// OrbitalStrikeCommand.java
package com.example.storytell.init.strike;

import com.mojang.brigadier.CommandDispatcher;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.Vec3;

import java.util.Collection;

public class OrbitalStrikeCommand {
    private static final int MAX_WORLD_HEIGHT = 312;

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("orbitalstrike")
                .then(Commands.argument("targets", EntityArgument.players())
                        .executes(context -> {
                            CommandSourceStack source = context.getSource();
                            Collection<ServerPlayer> targets = EntityArgument.getPlayers(context, "targets");

                            for (ServerPlayer target : targets) {
                                startOrbitalStrikeSequence(target);
                            }
                            return 1;
                        })
                )
                .executes(context -> {
                    CommandSourceStack source = context.getSource();
                    if (source.getEntity() instanceof ServerPlayer player) {
                        startOrbitalStrikeSequence(player);
                        return 1;
                    }
                    return 0;
                })
        );
    }

    private static void startOrbitalStrikeSequence(ServerPlayer target) {
        // 5 повторений звуков и эффектов с задержкой 5 секунд
        for (int repetition = 0; repetition < 5; repetition++) {
            final int currentRep = repetition;
            new Thread(() -> {
                try {
                    Thread.sleep(5000 * (currentRep + 1)); // Задержка 5, 10, 15, 20, 25 секунд

                    target.getServer().execute(() -> {
                        applyPreparationEffects(target, currentRep + 1);
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

                target.getServer().execute(() -> {
                    performLaserStrike(target);
                });
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }).start();
    }

    private static void applyPreparationEffects(ServerPlayer target, int repetition) {
        target.level().playSound(
                null,
                target.getX(), target.getY(), target.getZ(),
                SoundEvents.RESPAWN_ANCHOR_CHARGE,
                SoundSource.HOSTILE,
                1.0F,
                0.5F
        );

        target.level().playSound(
                null,
                target.getX(), target.getY(), target.getZ(),
                SoundEvents.BEACON_ACTIVATE,
                SoundSource.HOSTILE,
                1.0F,
                1.0F
        );

        target.addEffect(new MobEffectInstance(MobEffects.CONFUSION, 100, 2));
        target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 100, 3));

        // Выполняем команду screenshake для цели
        String command = "screenshake " + target.getGameProfile().getName() + " 1.1 60";
        target.getServer().getCommands().performPrefixedCommand(
                target.getServer().createCommandSourceStack().withSuppressedOutput(),
                command
        );
    }

    private static void performLaserStrike(ServerPlayer target) {
        // Создаем визуальный луч и разрушаем блоки
        createLaserBeam(target.serverLevel(), target.position());

        // Наносим урон
        applyLethalDamage(target);

        // МОЩНЫЙ ВЗРЫВ с огнём и разрушением блоков
        createPowerfulExplosion(target.serverLevel(), target.position());
    }

    private static void applyLethalDamage(ServerPlayer target) {
        target.hurt(target.damageSources().generic(), 1000.0F);
        target.setSecondsOnFire(5);
        target.addEffect(new MobEffectInstance(MobEffects.WITHER, 100, 1));
    }

    private static void createLaserBeam(ServerLevel level, Vec3 targetPos) {
        Vec3 skyPos = new Vec3(targetPos.x, MAX_WORLD_HEIGHT, targetPos.z);

        // Разрушаем блоки на пути лазера
        destroyBlocksAlongLaser(level, skyPos, targetPos);

        // Создаем визуальный эффект лазера
        LaserBeamRenderer.createTextureBeam(level, skyPos, targetPos);

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
            }
        }
    }

    private static void createPowerfulExplosion(ServerLevel level, Vec3 center) {
        level.explode(
                null,
                center.x, center.y, center.z,
                3.0F,
                false,
                Level.ExplosionInteraction.BLOCK
        );

        level.sendParticles(net.minecraft.core.particles.ParticleTypes.EXPLOSION_EMITTER,
                center.x, center.y + 1, center.z, 10, 3, 3, 3, 0.3);

        level.sendParticles(net.minecraft.core.particles.ParticleTypes.FLAME,
                center.x, center.y + 1, center.z, 50, 4, 4, 4, 0.2);

        level.playSound(
                null,
                center.x, center.y, center.z,
                SoundEvents.GENERIC_EXPLODE,
                SoundSource.HOSTILE,
                8.0F,
                0.6F
        );
    }
}