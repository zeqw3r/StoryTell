// BossEventHandler.java
package com.example.storytell.init.boss;

import com.example.storytell.init.HologramConfig;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.GameType;
import net.minecraft.server.level.ServerLevel;
import net.minecraftforge.event.entity.EntityJoinLevelEvent;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.server.ServerLifecycleHooks;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.*;

@Mod.EventBusSubscriber(modid = "storytell", bus = Mod.EventBusSubscriber.Bus.FORGE)
public class BossEventHandler {
    private static final Logger LOGGER = LogManager.getLogger();

    // Храним информацию о боссах и игроках в спектаторе
    private static final Map<UUID, Set<UUID>> bossSpectatorPlayers = new HashMap<>();
    // Храним информацию о всех игроках, которые были рядом с боссом
    private static final Map<UUID, Set<UUID>> bossNearbyPlayers = new HashMap<>();
    private static final int CHECK_RADIUS = 100;
    private static final int MAX_SPECTATOR_DISTANCE = 300;

    @SubscribeEvent
    public static void onEntitySpawn(EntityJoinLevelEvent event) {
        Entity entity = event.getEntity();

        // Проверяем, что это живое существо и не на клиентской стороне
        if (entity instanceof LivingEntity livingEntity && !event.getLevel().isClientSide()) {
            ResourceLocation entityId = ForgeRegistries.ENTITY_TYPES.getKey(livingEntity.getType());

            if (entityId != null && HologramConfig.isBoss(entityId)) {
                // Даем эффект силы 4 уровня (уровень 3 = 4-й уровень)
                MobEffectInstance strengthEffect = new MobEffectInstance(
                        MobEffects.DAMAGE_BOOST,
                        Integer.MAX_VALUE,
                        3,
                        false,
                        false
                );

                // Даем эффект сопротивления 1 уровня
                MobEffectInstance resistanceEffect = new MobEffectInstance(
                        MobEffects.DAMAGE_RESISTANCE,
                        Integer.MAX_VALUE,
                        0, // Уровень 0 = 1 уровень сопротивления
                        false,
                        false
                );

                // Даем эффект регенерации 1 уровня
                MobEffectInstance regenerationEffect = new MobEffectInstance(
                        MobEffects.REGENERATION,
                        Integer.MAX_VALUE,
                        0, // Уровень 0 = 1 уровень регенерации
                        false,
                        false
                );

                livingEntity.addEffect(strengthEffect);
                livingEntity.addEffect(resistanceEffect);
                livingEntity.addEffect(regenerationEffect);

                // Регистрируем босса
                UUID bossId = livingEntity.getUUID();
                bossSpectatorPlayers.put(bossId, new HashSet<>());
                bossNearbyPlayers.put(bossId, new HashSet<>());

                // Сразу находим всех игроков рядом с боссом
                updateNearbyPlayers(livingEntity);

                LOGGER.info("Applied effects to boss: {}", entityId);
            }
        }
    }

    @SubscribeEvent
    public static void onPlayerDeath(LivingDeathEvent event) {
        if (event.getEntity() instanceof ServerPlayer player && !event.getEntity().level().isClientSide()) {
            // Проверяем всех боссов в радиусе 100 блоков
            for (LivingEntity entity : player.level().getEntitiesOfClass(LivingEntity.class,
                    player.getBoundingBox().inflate(CHECK_RADIUS))) {

                ResourceLocation entityId = ForgeRegistries.ENTITY_TYPES.getKey(entity.getType());
                if (entityId != null && HologramConfig.isBoss(entityId) && entity.isAlive()) {
                    // Переводим игрока в режим наблюдателя
                    player.setGameMode(GameType.SPECTATOR);

                    // Добавляем игрока в список привязанных к этому боссу
                    UUID bossId = entity.getUUID();
                    bossSpectatorPlayers.computeIfAbsent(bossId, k -> new HashSet<>())
                            .add(player.getUUID());

                    // Обновляем список игроков рядом с боссом
                    updateNearbyPlayers(entity);

                    LOGGER.info("Player {} set to spectator due to boss {}", player.getScoreboardName(), entityId);

                    // Проверяем, все ли игроки рядом с боссом мертвы
                    checkIfAllPlayersDead(entity);
                    break;
                }
            }
        }
    }

    @SubscribeEvent
    public static void onBossDeath(LivingDeathEvent event) {
        if (!event.getEntity().level().isClientSide()) {
            LivingEntity boss = (LivingEntity) event.getEntity();
            ResourceLocation entityId = ForgeRegistries.ENTITY_TYPES.getKey(boss.getType());

            if (entityId != null && HologramConfig.isBoss(entityId)) {
                UUID bossId = boss.getUUID();

                // Возвращаем всех привязанных игроков в выживание
                returnPlayersToSurvival(bossId);

                // Очищаем списки
                bossSpectatorPlayers.remove(bossId);
                bossNearbyPlayers.remove(bossId);

                LOGGER.info("Boss died: {}", entityId);
            }
        }
    }

    // Обновляем список игроков рядом с боссом
    private static void updateNearbyPlayers(LivingEntity boss) {
        UUID bossId = boss.getUUID();
        Set<UUID> nearbyPlayers = new HashSet<>();

        // Находим всех игроков в радиусе 100 блоков от босса
        for (Player player : boss.level().players()) {
            if (player instanceof ServerPlayer serverPlayer &&
                    serverPlayer.distanceTo(boss) <= CHECK_RADIUS) {
                nearbyPlayers.add(serverPlayer.getUUID());
            }
        }

        bossNearbyPlayers.put(bossId, nearbyPlayers);
    }

    // Проверяем, все ли игроки рядом с боссом мертвы
    private static void checkIfAllPlayersDead(LivingEntity boss) {
        UUID bossId = boss.getUUID();

        if (!bossNearbyPlayers.containsKey(bossId) || !bossSpectatorPlayers.containsKey(bossId)) {
            return;
        }

        Set<UUID> nearbyPlayers = bossNearbyPlayers.get(bossId);
        Set<UUID> spectatorPlayers = bossSpectatorPlayers.get(bossId);

        // Проверяем, все ли игроки из списка nearbyPlayers находятся в spectatorPlayers
        boolean allPlayersDead = true;
        for (UUID playerId : nearbyPlayers) {
            if (!spectatorPlayers.contains(playerId)) {
                // Игрок еще жив (не в спектаторе)
                allPlayersDead = false;
                break;
            }
        }

        // Если все игроки мертвы, исцеляем босса
        if (allPlayersDead && nearbyPlayers.size() > 0) {
            if (boss.getHealth() < boss.getMaxHealth()) {
                boss.setHealth(boss.getMaxHealth());
                LOGGER.info("Boss {} fully healed after all {} nearby players died",
                        ForgeRegistries.ENTITY_TYPES.getKey(boss.getType()), nearbyPlayers.size());
            }

            // ВАЖНОЕ ИСПРАВЛЕНИЕ: Возвращаем всех игроков в выживание когда все умерли
            returnPlayersToSurvival(bossId);
            bossSpectatorPlayers.remove(bossId);
            bossNearbyPlayers.remove(bossId);
        }
    }

    // Периодическая проверка для возврата игроков в выживание и обновления списков
    public static void checkSpectatorPlayers() {
        var server = ServerLifecycleHooks.getCurrentServer();
        if (server == null) return;

        // Создаем копию для безопасной итерации
        Map<UUID, Set<UUID>> copy = new HashMap<>(bossSpectatorPlayers);

        for (Map.Entry<UUID, Set<UUID>> entry : copy.entrySet()) {
            UUID bossId = entry.getKey();
            Set<UUID> playerUUIDs = new HashSet<>(entry.getValue());

            // Находим босса по UUID
            LivingEntity boss = findBossById(bossId);

            // Если босс мертв или не найден, возвращаем всех игроков
            if (boss == null || !boss.isAlive()) {
                returnPlayersToSurvival(bossId);
                bossNearbyPlayers.remove(bossId);
                continue;
            }

            // Обновляем список игроков рядом с боссом
            updateNearbyPlayers(boss);

            // Проверяем, все ли игроки рядом с боссом мертвы (возвращает в выживание если все умерли)
            checkIfAllPlayersDead(boss);

            // Если списки были очищены в checkIfAllPlayersDead, пропускаем дальнейшую обработку
            if (!bossSpectatorPlayers.containsKey(bossId)) {
                continue;
            }

            // Проверяем условия для возврата в выживание
            if (shouldReturnAllPlayersToSurvival(boss)) {
                returnPlayersToSurvival(bossId);
                bossNearbyPlayers.remove(bossId);
            } else {
                // Проверяем отдельных игроков, которых можно вернуть
                checkIndividualPlayers(boss, bossId, playerUUIDs);
            }
        }
    }

    // Проверка расстояния спектаторов и телепортация при необходимости
    public static void checkSpectatorDistances() {
        var server = ServerLifecycleHooks.getCurrentServer();
        if (server == null) return;

        // Создаем копию для безопасной итерации
        Map<UUID, Set<UUID>> copy = new HashMap<>(bossSpectatorPlayers);

        for (Map.Entry<UUID, Set<UUID>> entry : copy.entrySet()) {
            UUID bossId = entry.getKey();
            Set<UUID> playerUUIDs = new HashSet<>(entry.getValue());

            // Находим босса по UUID
            LivingEntity boss = findBossById(bossId);
            if (boss == null || !boss.isAlive()) continue;

            for (UUID playerUUID : playerUUIDs) {
                ServerPlayer player = server.getPlayerList().getPlayer(playerUUID);
                if (player != null && player.gameMode.getGameModeForPlayer() == GameType.SPECTATOR) {
                    // Проверяем расстояние от игрока до босса
                    double distance = player.distanceTo(boss);
                    if (distance > MAX_SPECTATOR_DISTANCE) {
                        // Телепортируем игрока к боссу
                        player.teleportTo(boss.getX(), boss.getY() + 2, boss.getZ());
                        LOGGER.info("Teleported spectator player {} back to boss", player.getScoreboardName());
                    }
                }
            }
        }
    }

    private static void returnPlayersToSurvival(UUID bossId) {
        if (!bossSpectatorPlayers.containsKey(bossId)) return;

        Set<UUID> playerUUIDs = bossSpectatorPlayers.get(bossId);
        var server = ServerLifecycleHooks.getCurrentServer();
        if (server == null) return;

        for (UUID playerUUID : playerUUIDs) {
            ServerPlayer player = server.getPlayerList().getPlayer(playerUUID);
            if (player != null && player.gameMode.getGameModeForPlayer() == GameType.SPECTATOR) {
                player.setGameMode(GameType.SURVIVAL);
                LOGGER.info("Player {} returned to survival", player.getScoreboardName());
            }
        }

        bossSpectatorPlayers.remove(bossId);
    }

    private static void checkIndividualPlayers(LivingEntity boss, UUID bossId, Set<UUID> playerUUIDs) {
        var server = ServerLifecycleHooks.getCurrentServer();
        if (server == null) return;

        for (UUID playerUUID : playerUUIDs) {
            ServerPlayer player = server.getPlayerList().getPlayer(playerUUID);
            if (player != null &&
                    player.gameMode.getGameModeForPlayer() == GameType.SPECTATOR &&
                    shouldPlayerReturnToSurvival(player, boss)) {

                player.setGameMode(GameType.SURVIVAL);
                bossSpectatorPlayers.get(bossId).remove(playerUUID);
                LOGGER.info("Player {} returned to survival - no nearby survival players",
                        player.getScoreboardName());
            }
        }

        // Если список игроков пуст, удаляем запись о боссе
        if (bossSpectatorPlayers.containsKey(bossId) && bossSpectatorPlayers.get(bossId).isEmpty()) {
            bossSpectatorPlayers.remove(bossId);
            bossNearbyPlayers.remove(bossId);
        }
    }

    private static boolean shouldReturnAllPlayersToSurvival(LivingEntity boss) {
        // Если босс мертв, все игроки должны вернуться в выживание
        if (!boss.isAlive()) {
            return true;
        }

        // Проверяем, есть ли ЛЮБЫЕ игроки в выживании в радиусе 100 блоков от босса
        boolean hasSurvivalPlayers = false;
        for (Player player : boss.level().players()) {
            if (player instanceof ServerPlayer serverPlayer &&
                    serverPlayer.gameMode.getGameModeForPlayer() == GameType.SURVIVAL &&
                    serverPlayer.distanceTo(boss) <= CHECK_RADIUS) {
                hasSurvivalPlayers = true;
                break;
            }
        }

        // Если нет игроков в выживании рядом с боссом, возвращаем всех
        return !hasSurvivalPlayers;
    }

    private static boolean shouldPlayerReturnToSurvival(ServerPlayer spectator, LivingEntity boss) {
        // Если босс мертв, игрок должен вернуться в выживание
        if (!boss.isAlive()) {
            return true;
        }

        // Проверяем, есть ли игроки в выживании в радиусе 100 блоков от босса
        // ИСКЛЮЧАЯ самого проверяемого игрока (который в спектаторе)
        for (Player player : boss.level().players()) {
            if (player instanceof ServerPlayer serverPlayer &&
                    serverPlayer != spectator &&
                    serverPlayer.gameMode.getGameModeForPlayer() == GameType.SURVIVAL &&
                    serverPlayer.distanceTo(boss) <= CHECK_RADIUS) {
                return false; // Есть игроки в выживании рядом с боссом
            }
        }

        return true; // Нет игроков в выживании рядом с боссом
    }

    private static boolean hasNearbyLivingBosses(Player player) {
        for (LivingEntity entity : player.level().getEntitiesOfClass(LivingEntity.class,
                player.getBoundingBox().inflate(CHECK_RADIUS))) {

            ResourceLocation entityId = ForgeRegistries.ENTITY_TYPES.getKey(entity.getType());
            if (entityId != null && HologramConfig.isBoss(entityId) && entity.isAlive()) {
                return true;
            }
        }
        return false;
    }

    private static LivingEntity findBossById(UUID bossId) {
        // Ищем босса по UUID среди всех уровней сервера
        if (bossId == null) return null;

        // Используем ServerLifecycleHooks для получения сервера
        var server = ServerLifecycleHooks.getCurrentServer();
        if (server == null) return null;

        for (ServerLevel level : server.getAllLevels()) {
            Entity entity = level.getEntity(bossId);
            if (entity instanceof LivingEntity livingEntity) {
                ResourceLocation entityId = ForgeRegistries.ENTITY_TYPES.getKey(livingEntity.getType());
                if (entityId != null && HologramConfig.isBoss(entityId)) {
                    return livingEntity;
                }
            }
        }
        return null;
    }
}