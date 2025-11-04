package com.example.storytell.init.boss;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.GameType;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.server.ServerLifecycleHooks;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class BossSequenceManager {
    private static final List<String> BOSS_SEQUENCE = new ArrayList<>();
    private static final Map<UUID, BossSequenceInstance> activeSequences = new ConcurrentHashMap<>();

    // Оптимизированные коллекции
    private static final Map<UUID, Set<UUID>> sequenceSpectatorPlayers = new ConcurrentHashMap<>();
    private static final Map<UUID, Set<UUID>> sequenceAdventurePlayers = new ConcurrentHashMap<>();

    // Кэшированные эффекты
    private static final net.minecraft.world.effect.MobEffectInstance STRENGTH_EFFECT =
            new net.minecraft.world.effect.MobEffectInstance(net.minecraft.world.effect.MobEffects.DAMAGE_BOOST, Integer.MAX_VALUE, 3, false, false);
    private static final net.minecraft.world.effect.MobEffectInstance RESISTANCE_EFFECT =
            new net.minecraft.world.effect.MobEffectInstance(net.minecraft.world.effect.MobEffects.DAMAGE_RESISTANCE, Integer.MAX_VALUE, 0, false, false);
    private static final net.minecraft.world.effect.MobEffectInstance REGENERATION_EFFECT =
            new net.minecraft.world.effect.MobEffectInstance(net.minecraft.world.effect.MobEffects.REGENERATION, Integer.MAX_VALUE, 0, false, false);

    private static final int CHECK_RADIUS = 100;
    private static final int MAX_SPECTATOR_DISTANCE = 50;
    public static final String SEQUENCE_BOSS_TAG = "storytell_sequence_boss";

    // Задержка в тиках вместо миллисекунд
    private static final int RESPAWN_DELAY_TICKS = 200;

    static {
        BOSS_SEQUENCE.add("cataclysm:scylla");
        BOSS_SEQUENCE.add("cataclysm:maledictus");
        BOSS_SEQUENCE.add("cataclysm:ancient_remnant");
        BOSS_SEQUENCE.add("cataclysm:ender_guardian");
        BOSS_SEQUENCE.add("cataclysm:the_harbinger");
        BOSS_SEQUENCE.add("cataclysm:ignis");
        BOSS_SEQUENCE.add("cataclysm:netherite_monstrosity");
    }

    public static Map<UUID, BossSequenceInstance> getActiveSequences() {
        return Collections.unmodifiableMap(activeSequences);
    }

    public static void startBossSequence(ServerLevel level, BlockPos pos) {
        UUID sequenceId = UUID.randomUUID();
        BossSequenceInstance sequence = new BossSequenceInstance(sequenceId, level, pos, new ArrayList<>(BOSS_SEQUENCE));
        activeSequences.put(sequenceId, sequence);

        sequenceSpectatorPlayers.put(sequenceId, Collections.newSetFromMap(new ConcurrentHashMap<>()));
        sequenceAdventurePlayers.put(sequenceId, Collections.newSetFromMap(new ConcurrentHashMap<>()));

        sequence.start();
    }

    public static void onBossDeath(UUID bossId, LivingEntity boss) {
        if (!boss.getTags().contains(SEQUENCE_BOSS_TAG)) {
            return;
        }

        for (BossSequenceInstance sequence : activeSequences.values()) {
            if (sequence.hasBoss(bossId)) {
                sequence.onBossDeath(bossId, boss);
                break;
            }
        }
    }

    public static void onPlayerDeath(ServerPlayer player) {
        // Оптимизированная проверка активных последовательностей
        for (BossSequenceInstance sequence : activeSequences.values()) {
            if (sequence.isPlayerInArea(player)) {
                handlePlayerDeathInSequence(sequence.getSequenceId(), player);
                break;
            }
        }
    }

    private static void handlePlayerDeathInSequence(UUID sequenceId, ServerPlayer player) {
        // Отменяем стандартную смерть (будет сделано в событии)
        // Переводим игрока в режим наблюдателя
        player.setGameMode(GameType.SPECTATOR);
        player.setHealth(1.0f);

        // Добавляем в список наблюдателей
        if (sequenceSpectatorPlayers.containsKey(sequenceId)) {
            sequenceSpectatorPlayers.get(sequenceId).add(player.getUUID());
        }
        if (sequenceAdventurePlayers.containsKey(sequenceId)) {
            sequenceAdventurePlayers.get(sequenceId).remove(player.getUUID());
        }

        // Проверяем, все ли игроки в последовательности мертвы
        checkIfAllPlayersDeadInSequence(sequenceId);
    }

    private static void checkIfAllPlayersDeadInSequence(UUID sequenceId) {
        if (!sequenceAdventurePlayers.containsKey(sequenceId) || !sequenceSpectatorPlayers.containsKey(sequenceId)) return;

        Set<UUID> adventurePlayers = sequenceAdventurePlayers.get(sequenceId);

        // Оптимизированная проверка живых игроков
        boolean allPlayersDead = adventurePlayers.stream()
                .noneMatch(playerId -> {
                    ServerPlayer player = getPlayerById(playerId);
                    return player != null && player.isAlive() && player.gameMode.getGameModeForPlayer() != GameType.SPECTATOR;
                });

        if (allPlayersDead && (!adventurePlayers.isEmpty() || !sequenceSpectatorPlayers.get(sequenceId).isEmpty())) {
            BossSequenceInstance sequence = activeSequences.get(sequenceId);
            if (sequence != null) {
                sequence.resetSequence();
            }
        }
    }

    public static void updateSequencePlayers() {
        for (BossSequenceInstance sequence : activeSequences.values()) {
            UUID sequenceId = sequence.getSequenceId();
            updateNearbyPlayersInSequence(sequenceId, sequence);
            checkSpectatorDistancesInSequence(sequenceId, sequence);
        }
    }

    private static void updateNearbyPlayersInSequence(UUID sequenceId, BossSequenceInstance sequence) {
        if (!sequenceAdventurePlayers.containsKey(sequenceId)) return;

        Set<UUID> currentAdventurePlayers = sequenceAdventurePlayers.get(sequenceId);
        LivingEntity currentBoss = sequence.getCurrentBoss();

        if (currentBoss == null) return;

        // Оптимизированный поиск игроков в радиусе
        List<ServerPlayer> playersInRange = currentBoss.level().getEntitiesOfClass(
                ServerPlayer.class,
                currentBoss.getBoundingBox().inflate(CHECK_RADIUS)
        );

        for (ServerPlayer serverPlayer : playersInRange) {
            double distance = serverPlayer.distanceTo(currentBoss);
            UUID playerId = serverPlayer.getUUID();

            if (distance <= CHECK_RADIUS) {
                // Переводим живых игроков в режим приключения
                if (serverPlayer.gameMode.getGameModeForPlayer() == GameType.SURVIVAL &&
                        serverPlayer.isAlive() &&
                        !sequenceSpectatorPlayers.get(sequenceId).contains(playerId)) {
                    serverPlayer.setGameMode(GameType.ADVENTURE);
                    currentAdventurePlayers.add(playerId);
                }

                // Добавляем в приключение, если игрок уже в приключении и жив
                if (serverPlayer.gameMode.getGameModeForPlayer() == GameType.ADVENTURE &&
                        serverPlayer.isAlive()) {
                    currentAdventurePlayers.add(playerId);
                }
            } else {
                // Игрок вышел из радиуса - возвращаем в выживание
                if (currentAdventurePlayers.contains(playerId) &&
                        serverPlayer.gameMode.getGameModeForPlayer() == GameType.ADVENTURE &&
                        !sequenceSpectatorPlayers.get(sequenceId).contains(playerId)) {
                    serverPlayer.setGameMode(GameType.SURVIVAL);
                    currentAdventurePlayers.remove(playerId);
                }
            }
        }
    }

    private static void checkSpectatorDistancesInSequence(UUID sequenceId, BossSequenceInstance sequence) {
        LivingEntity currentBoss = sequence.getCurrentBoss();
        if (currentBoss == null) return;

        Set<UUID> spectatorPlayers = sequenceSpectatorPlayers.get(sequenceId);
        if (spectatorPlayers == null) return;

        for (UUID playerId : spectatorPlayers) {
            ServerPlayer player = getPlayerById(playerId);
            if (player != null && player.gameMode.getGameModeForPlayer() == GameType.SPECTATOR) {
                double distance = player.distanceTo(currentBoss);
                if (distance > MAX_SPECTATOR_DISTANCE) {
                    player.teleportTo(currentBoss.getX(), currentBoss.getY() + 2, currentBoss.getZ());
                }
            }
        }
    }

    public static void returnPlayersToSurvival(UUID sequenceId) {
        // Возвращаем наблюдателей в выживание
        if (sequenceSpectatorPlayers.containsKey(sequenceId)) {
            Set<UUID> spectatorPlayers = sequenceSpectatorPlayers.get(sequenceId);
            for (UUID playerId : spectatorPlayers) {
                ServerPlayer player = getPlayerById(playerId);
                if (player != null) {
                    player.setGameMode(GameType.SURVIVAL);
                    player.setHealth(player.getMaxHealth());
                    player.getFoodData().setFoodLevel(20);
                    player.removeAllEffects();
                }
            }
            sequenceSpectatorPlayers.remove(sequenceId);
        }

        // Возвращаем игроков в приключении в выживание
        if (sequenceAdventurePlayers.containsKey(sequenceId)) {
            Set<UUID> adventurePlayers = sequenceAdventurePlayers.get(sequenceId);
            for (UUID playerId : adventurePlayers) {
                ServerPlayer player = getPlayerById(playerId);
                if (player != null && player.gameMode.getGameModeForPlayer() == GameType.ADVENTURE) {
                    player.setGameMode(GameType.SURVIVAL);
                }
            }
            sequenceAdventurePlayers.remove(sequenceId);
        }
    }

    private static ServerPlayer getPlayerById(UUID playerId) {
        var server = ServerLifecycleHooks.getCurrentServer();
        return server != null ? server.getPlayerList().getPlayer(playerId) : null;
    }

    // Остальные методы без изменений
    public static void addBossToSequence(String bossId) {
        if (!BOSS_SEQUENCE.contains(bossId)) {
            BOSS_SEQUENCE.add(bossId);
        }
    }

    public static void removeBossFromSequence(String bossId) {
        BOSS_SEQUENCE.remove(bossId);
    }

    public static List<String> getBossSequence() {
        return new ArrayList<>(BOSS_SEQUENCE);
    }

    public static void clearBossSequence() {
        BOSS_SEQUENCE.clear();
    }

    public static boolean isSequenceBoss(LivingEntity entity) {
        return entity.getTags().contains(SEQUENCE_BOSS_TAG);
    }

    public static class BossSequenceInstance {
        private final UUID sequenceId;
        private final ServerLevel level;
        private final BlockPos spawnPos;
        private final List<String> bossQueue;
        private int currentBossIndex = -1;
        private final Set<UUID> spawnedBosses = Collections.newSetFromMap(new ConcurrentHashMap<>());
        private boolean isActive = true;
        private boolean sequenceCompleted = false;
        private LivingEntity currentBoss = null;
        private int respawnTimer = -1;

        public BossSequenceInstance(UUID sequenceId, ServerLevel level, BlockPos spawnPos, List<String> bossQueue) {
            this.sequenceId = sequenceId;
            this.level = level;
            this.spawnPos = spawnPos;
            this.bossQueue = bossQueue;
        }

        public UUID getSequenceId() {
            return sequenceId;
        }

        public void start() {
            spawnNextBoss();
        }

        public void tick() {
            if (respawnTimer > 0) {
                respawnTimer--;
                if (respawnTimer == 0) {
                    spawnNextBoss();
                }
            }
        }

        private void spawnNextBoss() {
            respawnTimer = -1;
            currentBossIndex++;

            if (!isActive || currentBossIndex >= bossQueue.size()) {
                sequenceCompleted = true;
                resurrectDeadPlayersAtSpawnPos();
                returnPlayersToSurvival(sequenceId);
                activeSequences.remove(sequenceId);
                return;
            }

            String bossId = bossQueue.get(currentBossIndex);
            ResourceLocation entityId = new ResourceLocation(bossId);

            if (ForgeRegistries.ENTITY_TYPES.containsKey(entityId)) {
                Entity entity = ForgeRegistries.ENTITY_TYPES.getValue(entityId).create(level);

                if (entity instanceof LivingEntity) {
                    LivingEntity boss = (LivingEntity) entity;
                    boss.setPos(spawnPos.getX(), spawnPos.getY(), spawnPos.getZ());

                    applyBossEffects(boss);
                    boss.addTag(SEQUENCE_BOSS_TAG);
                    level.addFreshEntity(boss);
                    spawnedBosses.add(boss.getUUID());
                    currentBoss = boss;
                }
            } else {
                // Пропускаем неизвестного босса
                scheduleNextBoss();
            }
        }

        private void applyBossEffects(LivingEntity boss) {
            boss.addEffect(STRENGTH_EFFECT);
            boss.addEffect(RESISTANCE_EFFECT);
            boss.addEffect(REGENERATION_EFFECT);
        }

        public void onBossDeath(UUID bossId, LivingEntity boss) {
            if (spawnedBosses.contains(bossId)) {
                spawnedBosses.remove(bossId);
                currentBoss = null;
                scheduleNextBoss();
            }
        }

        private void scheduleNextBoss() {
            respawnTimer = RESPAWN_DELAY_TICKS;
        }

        public void resetSequence() {
            // Удаляем всех активных боссов
            for (UUID bossId : spawnedBosses) {
                Entity boss = level.getEntity(bossId);
                if (boss != null) {
                    boss.remove(Entity.RemovalReason.DISCARDED);
                }
            }
            spawnedBosses.clear();
            currentBoss = null;

            // Сбрасываем прогресс
            currentBossIndex = -1;
            sequenceCompleted = false;
            respawnTimer = -1;

            // Воскрешаем всех игроков на их точках спавна
            resurrectAllPlayersAtTheirSpawn();

            // Начинаем заново
            start();
        }

        private void resurrectAllPlayersAtTheirSpawn() {
            if (!sequenceSpectatorPlayers.containsKey(sequenceId)) return;

            Set<UUID> spectatorPlayers = sequenceSpectatorPlayers.get(sequenceId);

            for (UUID playerId : spectatorPlayers) {
                ServerPlayer player = getPlayerById(playerId);
                if (player != null) {
                    resurrectPlayerAtPersonalSpawn(player);
                }
            }
            spectatorPlayers.clear();
        }

        private void resurrectDeadPlayersAtSpawnPos() {
            if (!sequenceSpectatorPlayers.containsKey(sequenceId)) return;

            Set<UUID> spectatorPlayers = sequenceSpectatorPlayers.get(sequenceId);

            for (UUID playerId : spectatorPlayers) {
                ServerPlayer player = getPlayerById(playerId);
                if (player != null) {
                    // Телепортируем игрока на точку спавна последовательности (как в оригинале)
                    player.teleportTo(level, spawnPos.getX() + 0.5, spawnPos.getY() + 1, spawnPos.getZ() + 0.5, 0.0F, 0.0F);
                    player.setGameMode(GameType.SURVIVAL);
                    player.setHealth(player.getMaxHealth());
                    player.getFoodData().setFoodLevel(20);
                    player.removeAllEffects();
                }
            }
            spectatorPlayers.clear();
        }

        private void resurrectPlayerAtPersonalSpawn(ServerPlayer player) {
            ServerLevel respawnLevel = player.server.getLevel(player.getRespawnDimension());
            BlockPos respawnPos = player.getRespawnPosition();

            if (respawnLevel != null && respawnPos != null) {
                Optional<Vec3> safePos = Player.findRespawnPositionAndUseSpawnBlock(respawnLevel, respawnPos, 0.0F, false, true);
                if (safePos.isPresent()) {
                    player.teleportTo(respawnLevel, safePos.get().x(), safePos.get().y(), safePos.get().z(), 0.0F, 0.0F);
                } else {
                    BlockPos worldSpawn = respawnLevel.getSharedSpawnPos();
                    player.teleportTo(respawnLevel, worldSpawn.getX() + 0.5, worldSpawn.getY(), worldSpawn.getZ() + 0.5, 0.0F, 0.0F);
                }
            } else {
                ServerLevel overworld = player.server.getLevel(net.minecraft.world.level.Level.OVERWORLD);
                BlockPos worldSpawn = overworld.getSharedSpawnPos();
                player.teleportTo(overworld, worldSpawn.getX() + 0.5, worldSpawn.getY(), worldSpawn.getZ() + 0.5, 0.0F, 0.0F);
            }

            player.setGameMode(GameType.SURVIVAL);
            player.setHealth(player.getMaxHealth());
            player.getFoodData().setFoodLevel(20);
            player.removeAllEffects();
        }

        public boolean hasBoss(UUID bossId) {
            return spawnedBosses.contains(bossId);
        }

        public boolean isInArea(ServerLevel checkLevel, BlockPos center) {
            return checkLevel == level && spawnPos.closerThan(center, 100);
        }

        public boolean isPlayerInArea(ServerPlayer player) {
            return player.level() == level && player.blockPosition().closerThan(spawnPos, CHECK_RADIUS);
        }

        public LivingEntity getCurrentBoss() {
            return currentBoss;
        }
    }
}