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
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
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

    // Оптимизированные коллекции с WeakReference для предотвращения утечек памяти
    private static final Map<UUID, Set<UUID>> bossSpectatorPlayers = new WeakHashMap<>();
    private static final Map<UUID, Set<UUID>> bossNearbyPlayers = new WeakHashMap<>();
    private static final Map<UUID, Set<UUID>> bossAdventurePlayers = new WeakHashMap<>();
    private static final Set<UUID> activeBosses = Collections.newSetFromMap(new WeakHashMap<>());

    // Кэшированные эффекты для боссов (создаем один раз)
    private static final MobEffectInstance STRENGTH_EFFECT = new MobEffectInstance(
            MobEffects.DAMAGE_BOOST, Integer.MAX_VALUE, 3, false, false);
    private static final MobEffectInstance RESISTANCE_EFFECT = new MobEffectInstance(
            MobEffects.DAMAGE_RESISTANCE, Integer.MAX_VALUE, 0, false, false);
    private static final MobEffectInstance REGENERATION_EFFECT = new MobEffectInstance(
            MobEffects.REGENERATION, Integer.MAX_VALUE, 0, false, false);

    private static final int CHECK_RADIUS = 100;
    private static final int MAX_SPECTATOR_DISTANCE = 50;

    // Кэш для проверки конфига боссов
    private static final Map<ResourceLocation, Boolean> bossConfigCache = new WeakHashMap<>();
    private static long lastConfigCacheClear = System.currentTimeMillis();
    private static final long CONFIG_CACHE_TTL = 30000; // 30 секунд

    @SubscribeEvent
    public static void onEntitySpawn(EntityJoinLevelEvent event) {
        Entity entity = event.getEntity();

        // Быстрая проверка перед детальной проверкой
        if (!(entity instanceof LivingEntity livingEntity) || event.getLevel().isClientSide()) {
            return;
        }

        ResourceLocation entityId = ForgeRegistries.ENTITY_TYPES.getKey(livingEntity.getType());
        if (entityId == null) return;

        // Используем кэшированную проверку босса
        if (!isBossCached(entityId)) {
            return;
        }

        // Пропускаем боссов из цепочки
        if (BossSequenceManager.isSequenceBoss(livingEntity)) {
            return;
        }

        // Применяем кэшированные эффекты
        livingEntity.addEffect(STRENGTH_EFFECT);
        livingEntity.addEffect(RESISTANCE_EFFECT);
        livingEntity.addEffect(REGENERATION_EFFECT);

        // Регистрируем босса
        UUID bossId = livingEntity.getUUID();
        activeBosses.add(bossId);
        bossSpectatorPlayers.put(bossId, new HashSet<>());
        bossNearbyPlayers.put(bossId, new HashSet<>());
        bossAdventurePlayers.put(bossId, new HashSet<>());

        // Сразу находим всех игроков рядом с боссом и переводим в приключение
        updateNearbyPlayers(livingEntity);

        LOGGER.debug("Applied effects to boss: {}", entityId);
    }

    @SubscribeEvent
    public static void onPlayerDeath(LivingDeathEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player) || event.getEntity().level().isClientSide()) {
            return;
        }

        boolean handledBySequence = false;

        // Сначала проверяем боссов цепочки (оптимизированно)
        for (BossSequenceManager.BossSequenceInstance sequence : BossSequenceManager.getActiveSequences().values()) {
            if (sequence.isPlayerInArea(player)) {
                BossSequenceManager.onPlayerDeath(player);
                handledBySequence = true;
                event.setCanceled(true);
                break;
            }
        }

        // Если не обработано цепочкой, проверяем обычных боссов
        if (!handledBySequence) {
            handleRegularBossDeath(player, event);
        }
    }

    private static void handleRegularBossDeath(ServerPlayer player, LivingDeathEvent event) {
        // Используем активных боссов вместо поиска по всем сущностям
        for (UUID bossId : new HashSet<>(activeBosses)) {
            LivingEntity boss = findBossById(bossId);
            if (boss == null || !boss.isAlive()) {
                activeBosses.remove(bossId);
                continue;
            }

            if (boss.distanceTo(player) <= CHECK_RADIUS) {
                ResourceLocation entityId = ForgeRegistries.ENTITY_TYPES.getKey(boss.getType());
                if (entityId != null && isBossCached(entityId)) {
                    processBossDeath(player, event, bossId, boss);
                    break;
                }
            }
        }
    }

    private static void processBossDeath(ServerPlayer player, LivingDeathEvent event, UUID bossId, LivingEntity boss) {
        // Проверяем, что босс еще в наших картах
        if (!bossAdventurePlayers.containsKey(bossId) || !bossSpectatorPlayers.containsKey(bossId)) {
            return;
        }

        // Отменяем стандартную смерть
        event.setCanceled(true);

        // Переводим игрока в режим наблюдателя
        player.setGameMode(GameType.SPECTATOR);
        player.setHealth(1.0f);

        // Удаляем из списка приключения и добавляем в список наблюдателей
        bossAdventurePlayers.get(bossId).remove(player.getUUID());
        bossSpectatorPlayers.get(bossId).add(player.getUUID());

        LOGGER.debug("Player {} set to spectator due to boss {}", player.getScoreboardName(),
                ForgeRegistries.ENTITY_TYPES.getKey(boss.getType()));

        // Проверяем, все ли игроки рядом с боссом мертвы
        checkIfAllPlayersDead(boss);
    }

    @SubscribeEvent
    public static void onBossDeath(LivingDeathEvent event) {
        if (event.getEntity().level().isClientSide()) return;

        LivingEntity boss = (LivingEntity) event.getEntity();
        ResourceLocation entityId = ForgeRegistries.ENTITY_TYPES.getKey(boss.getType());

        if (entityId != null && isBossCached(entityId)) {
            // Пропускаем боссов из цепочки - их обрабатывает BossSequenceManager
            if (BossSequenceManager.isSequenceBoss(boss)) {
                BossSequenceManager.onBossDeath(boss.getUUID(), boss);
                return;
            }

            UUID bossId = boss.getUUID();
            activeBosses.remove(bossId);

            // Возвращаем всех привязанных игроков в выживание
            returnPlayersToSurvival(bossId);

            // Очищаем списки
            bossSpectatorPlayers.remove(bossId);
            bossNearbyPlayers.remove(bossId);
            bossAdventurePlayers.remove(bossId);

            LOGGER.debug("Boss died: {}", entityId);
        }
    }

    // Обновляем список игроков рядом с боссом
    private static void updateNearbyPlayers(LivingEntity boss) {
        UUID bossId = boss.getUUID();

        if (!bossNearbyPlayers.containsKey(bossId) || !bossAdventurePlayers.containsKey(bossId)) {
            return;
        }

        Set<UUID> nearbyPlayers = new HashSet<>();
        Set<UUID> currentAdventurePlayers = bossAdventurePlayers.get(bossId);

        // Оптимизированный поиск игроков
        List<ServerPlayer> playersInRange = boss.level().getEntitiesOfClass(
                ServerPlayer.class,
                boss.getBoundingBox().inflate(CHECK_RADIUS)
        );

        for (ServerPlayer serverPlayer : playersInRange) {
            double distance = serverPlayer.distanceTo(boss);
            UUID playerId = serverPlayer.getUUID();

            if (distance <= CHECK_RADIUS) {
                nearbyPlayers.add(playerId);

                // Переводим живых игроков в режим приключения
                if (serverPlayer.gameMode.getGameModeForPlayer() == GameType.SURVIVAL && serverPlayer.isAlive()) {
                    serverPlayer.setGameMode(GameType.ADVENTURE);
                    currentAdventurePlayers.add(playerId);
                    LOGGER.debug("Player {} set to adventure mode near boss {}",
                            serverPlayer.getScoreboardName(), ForgeRegistries.ENTITY_TYPES.getKey(boss.getType()));
                } else if (serverPlayer.gameMode.getGameModeForPlayer() == GameType.ADVENTURE && serverPlayer.isAlive()) {
                    currentAdventurePlayers.add(playerId);
                }
            } else {
                // Игрок вышел из радиуса - возвращаем в выживание
                if (currentAdventurePlayers.contains(playerId) &&
                        serverPlayer.gameMode.getGameModeForPlayer() == GameType.ADVENTURE) {
                    serverPlayer.setGameMode(GameType.SURVIVAL);
                    currentAdventurePlayers.remove(playerId);
                    LOGGER.debug("Player {} returned to survival - left boss area", serverPlayer.getScoreboardName());
                }
            }
        }

        bossNearbyPlayers.put(bossId, nearbyPlayers);
        bossAdventurePlayers.put(bossId, currentAdventurePlayers);
    }

    // Проверяем, все ли игроки рядом с боссом мертвы
    private static void checkIfAllPlayersDead(LivingEntity boss) {
        UUID bossId = boss.getUUID();

        if (!bossNearbyPlayers.containsKey(bossId) || !bossSpectatorPlayers.containsKey(bossId)) {
            return;
        }

        Set<UUID> nearbyPlayers = bossNearbyPlayers.get(bossId);
        Set<UUID> spectatorPlayers = bossSpectatorPlayers.get(bossId);

        // Оптимизированная проверка
        boolean allPlayersDead = nearbyPlayers.stream()
                .allMatch(playerId -> {
                    ServerPlayer player = getPlayerById(playerId);
                    return player == null || !player.isAlive() || spectatorPlayers.contains(playerId);
                });

        // Если все игроки мертвы, исцеляем босса и воскрешаем игроков
        if (allPlayersDead && !nearbyPlayers.isEmpty()) {
            // Исцеляем босса
            if (boss.getHealth() < boss.getMaxHealth()) {
                boss.setHealth(boss.getMaxHealth());
                LOGGER.info("Boss {} fully healed after all {} nearby players died",
                        ForgeRegistries.ENTITY_TYPES.getKey(boss.getType()), nearbyPlayers.size());
            }

            // Воскрешаем всех игроков на их точках возрождения
            resurrectPlayersAtSpawn(bossId);
        }
    }

    // Воскрешение игроков на их точках возрождения
    private static void resurrectPlayersAtSpawn(UUID bossId) {
        var server = ServerLifecycleHooks.getCurrentServer();
        if (server == null) return;

        if (!bossSpectatorPlayers.containsKey(bossId) || !bossAdventurePlayers.containsKey(bossId)) {
            return;
        }

        Set<UUID> spectatorPlayers = bossSpectatorPlayers.get(bossId);
        Set<UUID> adventurePlayers = bossAdventurePlayers.get(bossId);

        // Создаем копию списка для безопасной итерации
        Set<UUID> spectatorsToResurrect = new HashSet<>(spectatorPlayers);

        for (UUID playerId : spectatorsToResurrect) {
            ServerPlayer player = server.getPlayerList().getPlayer(playerId);
            if (player != null && player.gameMode.getGameModeForPlayer() == GameType.SPECTATOR) {
                resurrectPlayerAtSpawn(player);
                spectatorPlayers.remove(playerId);
                adventurePlayers.remove(playerId);
            }
        }

        LOGGER.info("All players resurrected at spawn points for boss {}", bossId);
    }

    private static void resurrectPlayerAtSpawn(ServerPlayer player) {
        ServerLevel respawnLevel = player.server.getLevel(player.getRespawnDimension());
        BlockPos respawnPos = player.getRespawnPosition();

        if (respawnLevel != null && respawnPos != null) {
            // Находим безопасную позицию для возрождения
            Optional<Vec3> safePos = Player.findRespawnPositionAndUseSpawnBlock(respawnLevel, respawnPos, 0.0F, false, true);
            if (safePos.isPresent()) {
                player.teleportTo(respawnLevel, safePos.get().x(), safePos.get().y(), safePos.get().z(), 0.0F, 0.0F);
            } else {
                // Если не нашли безопасную позицию, используем мировую точку возрождения
                BlockPos worldSpawn = respawnLevel.getSharedSpawnPos();
                player.teleportTo(respawnLevel, worldSpawn.getX() + 0.5, worldSpawn.getY(), worldSpawn.getZ() + 0.5, 0.0F, 0.0F);
            }
        } else {
            // Если нет точки возрождения, используем мировую точку возрождения
            ServerLevel overworld = player.server.getLevel(Level.OVERWORLD);
            BlockPos worldSpawn = overworld.getSharedSpawnPos();
            player.teleportTo(overworld, worldSpawn.getX() + 0.5, worldSpawn.getY(), worldSpawn.getZ() + 0.5, 0.0F, 0.0F);
        }

        // Устанавливаем режим выживания и восстанавливаем
        player.setGameMode(GameType.SURVIVAL);
        player.setHealth(player.getMaxHealth());
        player.getFoodData().setFoodLevel(20);
        player.getFoodData().setSaturation(5.0F);
        player.removeAllEffects();

        LOGGER.info("Resurrected player {} at spawn point and set to survival", player.getScoreboardName());
    }

    // Периодическая проверка для возврата игроков в выживание и обновления списков
    public static void checkSpectatorPlayers() {
        var server = ServerLifecycleHooks.getCurrentServer();
        if (server == null) return;

        // Очищаем кэш конфига если нужно
        if (System.currentTimeMillis() - lastConfigCacheClear > CONFIG_CACHE_TTL) {
            bossConfigCache.clear();
            lastConfigCacheClear = System.currentTimeMillis();
        }

        // Создаем копию для безопасной итерации
        Set<UUID> activeBossesCopy = new HashSet<>(activeBosses);

        for (UUID bossId : activeBossesCopy) {
            LivingEntity boss = findBossById(bossId);

            // Если босс мертв или не найден, возвращаем всех игроков
            if (boss == null || !boss.isAlive()) {
                returnPlayersToSurvival(bossId);
                bossNearbyPlayers.remove(bossId);
                bossAdventurePlayers.remove(bossId);
                activeBosses.remove(bossId);
                continue;
            }

            // Обновляем список игроков рядом с боссом
            updateNearbyPlayers(boss);

            // Проверяем, все ли игроки рядом с боссом мертвы
            checkIfAllPlayersDead(boss);
        }
    }

    // Проверка расстояния спектаторов и телепортация при необходимости
    public static void checkSpectatorDistances() {
        var server = ServerLifecycleHooks.getCurrentServer();
        if (server == null) return;

        for (UUID bossId : activeBosses) {
            LivingEntity boss = findBossById(bossId);
            if (boss == null || !boss.isAlive()) continue;

            Set<UUID> playerUUIDs = bossSpectatorPlayers.get(bossId);
            if (playerUUIDs == null) continue;

            for (UUID playerUUID : playerUUIDs) {
                ServerPlayer player = getPlayerById(playerUUID);
                if (player != null && player.gameMode.getGameModeForPlayer() == GameType.SPECTATOR) {
                    // Проверяем расстояние от игрока до босса
                    double distance = player.distanceTo(boss);
                    if (distance > MAX_SPECTATOR_DISTANCE) {
                        // Телепортируем игрока к боссу
                        player.teleportTo(boss.getX(), boss.getY() + 2, boss.getZ());
                        LOGGER.debug("Teleported spectator player {} back to boss", player.getScoreboardName());
                    }
                }
            }
        }
    }

    private static void returnPlayersToSurvival(UUID bossId) {
        // Возвращаем наблюдателей в выживание (без телепортации)
        if (bossSpectatorPlayers.containsKey(bossId)) {
            Set<UUID> spectatorUUIDs = bossSpectatorPlayers.get(bossId);
            var server = ServerLifecycleHooks.getCurrentServer();
            if (server != null) {
                for (UUID playerUUID : spectatorUUIDs) {
                    ServerPlayer player = server.getPlayerList().getPlayer(playerUUID);
                    if (player != null && player.gameMode.getGameModeForPlayer() == GameType.SPECTATOR) {
                        player.setGameMode(GameType.SURVIVAL);
                        player.setHealth(player.getMaxHealth());
                        player.getFoodData().setFoodLevel(20);
                        LOGGER.debug("Player {} returned to survival after boss death", player.getScoreboardName());
                    }
                }
            }
            bossSpectatorPlayers.remove(bossId);
        }

        // Возвращаем игроков в приключении в выживание
        if (bossAdventurePlayers.containsKey(bossId)) {
            Set<UUID> adventureUUIDs = bossAdventurePlayers.get(bossId);
            var server = ServerLifecycleHooks.getCurrentServer();
            if (server != null) {
                for (UUID playerUUID : adventureUUIDs) {
                    ServerPlayer player = server.getPlayerList().getPlayer(playerUUID);
                    if (player != null && player.gameMode.getGameModeForPlayer() == GameType.ADVENTURE) {
                        player.setGameMode(GameType.SURVIVAL);
                        LOGGER.debug("Player {} returned to survival after boss death", player.getScoreboardName());
                    }
                }
            }
            bossAdventurePlayers.remove(bossId);
        }

        // Удаляем из nearbyPlayers
        bossNearbyPlayers.remove(bossId);
        activeBosses.remove(bossId);
    }

    private static ServerPlayer getPlayerById(UUID playerId) {
        var server = ServerLifecycleHooks.getCurrentServer();
        return server != null ? server.getPlayerList().getPlayer(playerId) : null;
    }

    private static LivingEntity findBossById(UUID bossId) {
        if (bossId == null) return null;

        var server = ServerLifecycleHooks.getCurrentServer();
        if (server == null) return null;

        // Оптимизированный поиск - проверяем только активные уровни
        for (ServerLevel level : server.getAllLevels()) {
            Entity entity = level.getEntity(bossId);
            if (entity instanceof LivingEntity livingEntity) {
                ResourceLocation entityId = ForgeRegistries.ENTITY_TYPES.getKey(livingEntity.getType());
                if (entityId != null && isBossCached(entityId)) {
                    return livingEntity;
                }
            }
        }
        return null;
    }

    // Кэшированная проверка босса
    private static boolean isBossCached(ResourceLocation entityId) {
        return bossConfigCache.computeIfAbsent(entityId, HologramConfig::isBoss);
    }
}