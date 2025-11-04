package com.example.storytell.init.entity;

import com.example.storytell.init.HologramConfig;
import com.example.storytell.init.ModEntities;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@Mod.EventBusSubscriber(modid = "storytell")
public class REPOSpawnManager {
    private static final Logger LOGGER = LogManager.getLogger();

    private static final Map<UUID, Long> playerCooldowns = new ConcurrentHashMap<>();
    private static final long SPAWN_COOLDOWN = 5 * 60 * 1000;
    private static final Random random = new Random();
    private static UUID activeRepoUUID = null;

    // Оптимизация: кэш для проверки существования REPO
    private static Long lastRepoSweepTime = null;
    private static final long REPO_SWEEP_INTERVAL = 10000; // 10 секунд
    private static Boolean repoExistsCache = null;

    @SubscribeEvent
    public static void onServerTick(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;

        if (!HologramConfig.isRepoSpawnEnabled()) {
            return;
        }

        // Увеличиваем интервал проверки для производительности
        if (event.getServer().getTickCount() % 400 != 0) return; // Каждые 20 секунд вместо 10

        // Используем кэшированную проверку существования REPO
        if (!shouldSpawnNewRepo(event.getServer())) {
            return;
        }

        // Ищем подходящий уровень для спавна
        for (ServerLevel level : event.getServer().getAllLevels()) {
            if (level.players().isEmpty()) continue;

            List<ServerPlayer> eligiblePlayers = getEligiblePlayers(level);
            if (eligiblePlayers.isEmpty()) continue;

            ServerPlayer targetPlayer = eligiblePlayers.get(random.nextInt(eligiblePlayers.size()));
            trySpawnREPO(level, targetPlayer);
            break;
        }
    }

    private static boolean shouldSpawnNewRepo(net.minecraft.server.MinecraftServer server) {
        long currentTime = System.currentTimeMillis();

        // Используем кэш для проверки существования REPO
        if (lastRepoSweepTime == null || currentTime - lastRepoSweepTime > REPO_SWEEP_INTERVAL) {
            repoExistsCache = checkRepoExistsInWorld(server);
            lastRepoSweepTime = currentTime;
        }

        return !repoExistsCache && activeRepoUUID == null;
    }

    private static boolean checkRepoExistsInWorld(net.minecraft.server.MinecraftServer server) {
        // Оптимизированный поиск REPO - проверяем только активные уровни
        for (ServerLevel level : server.getAllLevels()) {
            // Быстрая проверка по активному UUID
            if (activeRepoUUID != null && level.getEntity(activeRepoUUID) instanceof REPO) {
                return true;
            }

            // Проверяем только если в уровне есть игроки
            if (!level.players().isEmpty()) {
                // Используем более эффективный поиск по классу
                // УБРАНА ПРОВЕРКА isEmpty() - просто перебираем entities
                for (net.minecraft.world.entity.Entity entity : level.getAllEntities()) {
                    if (entity instanceof REPO) {
                        activeRepoUUID = entity.getUUID();
                        return true;
                    }
                }
            }
        }
        return false;
    }

    private static List<ServerPlayer> getEligiblePlayers(ServerLevel level) {
        List<ServerPlayer> eligiblePlayers = new ArrayList<>();
        long currentTime = System.currentTimeMillis();

        // Оптимизированная проверка игроков
        for (ServerPlayer player : level.players()) {
            if (isPlayerEligible(player, currentTime)) {
                eligiblePlayers.add(player);
            }
        }

        return eligiblePlayers;
    }

    private static boolean isPlayerEligible(ServerPlayer player, long currentTime) {
        UUID playerId = player.getUUID();

        // Проверяем кулдаун
        Long lastSpawnTime = playerCooldowns.get(playerId);
        if (lastSpawnTime != null && (currentTime - lastSpawnTime) < SPAWN_COOLDOWN) {
            return false;
        }

        // Проверяем условия спавна
        return !player.isSleeping() && player.getRespawnPosition() == null;
    }

    private static void trySpawnREPO(ServerLevel level, ServerPlayer targetPlayer) {
        Vec3 spawnPos = findSpawnPosition(level, targetPlayer);
        if (spawnPos == null) {
            LOGGER.debug("Не удалось найти позицию для спавна REPO");
            return;
        }

        REPO repo = ModEntities.REPO.get().create(level);
        if (repo == null) {
            LOGGER.debug("Не удалось создать entity REPO");
            return;
        }

        repo.setPos(spawnPos.x, spawnPos.y, spawnPos.z);
        repo.setTargetPlayer(targetPlayer.getUUID());

        level.addFreshEntity(repo);
        spawnSpawnParticles(level, spawnPos);

        activeRepoUUID = repo.getUUID();
        updateAllPlayerCooldowns();

        LOGGER.debug("REPO заспавнен рядом с игроком: {} на позиции: {}, {}, {}",
                targetPlayer.getName().getString(), spawnPos.x, spawnPos.y, spawnPos.z);
    }

    private static void updateAllPlayerCooldowns() {
        long currentTime = System.currentTimeMillis();
        // Очищаем старые записи кулдауна для экономии памяти
        cleanupOldCooldowns(currentTime);
        playerCooldowns.put(UUID.randomUUID(), currentTime);
    }

    private static void cleanupOldCooldowns(long currentTime) {
        // Удаляем старые записи кулдауна (старше 10 минут)
        playerCooldowns.entrySet().removeIf(entry ->
                (currentTime - entry.getValue()) > (SPAWN_COOLDOWN * 2)
        );
    }

    private static void spawnSpawnParticles(ServerLevel level, Vec3 pos) {
        double x = pos.x;
        double y = pos.y + 1.0;
        double z = pos.z;

        // Оптимизированное создание частиц
        for (int i = 0; i < 15; i++) { // Уменьшено с 30 до 15
            double offsetX = (random.nextDouble() - 0.5) * 2.0;
            double offsetY = random.nextDouble() * 2.0;
            double offsetZ = (random.nextDouble() - 0.5) * 2.0;

            level.sendParticles(net.minecraft.core.particles.ParticleTypes.PORTAL,
                    x + offsetX, y + offsetY, z + offsetZ,
                    1, 0, 0, 0, 0.1);
        }
    }

    private static Vec3 findSpawnPosition(ServerLevel level, ServerPlayer player) {
        Vec3 playerPos = player.position();

        // Оптимизированный поиск позиции с кэшированием высоты
        for (int attempt = 0; attempt < 15; attempt++) { // Уменьшено с 20 до 15 попыток
            double angle = random.nextDouble() * 2 * Math.PI;
            double distance = 20 + random.nextDouble() * 80;

            double x = playerPos.x + Math.cos(angle) * distance;
            double z = playerPos.z + Math.sin(angle) * distance;

            BlockPos spawnPos = findValidSpawnPosition(level, (int) x, (int) z);
            if (spawnPos != null) {
                return new Vec3(spawnPos.getX() + 0.5, spawnPos.getY() + 0.5, spawnPos.getZ() + 0.5);
            }
        }

        LOGGER.debug("Не удалось найти валидную позицию для спавна после 15 попыток");
        return null;
    }

    private static BlockPos findValidSpawnPosition(ServerLevel level, int x, int z) {
        int surfaceY = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z);

        if (surfaceY > level.getMinBuildHeight()) {
            BlockPos spawnPos = new BlockPos(x, surfaceY, z);
            BlockPos abovePos = spawnPos.above();

            if (level.isEmptyBlock(spawnPos) && level.isEmptyBlock(abovePos)) {
                return spawnPos;
            }
        }
        return null;
    }

    public static void setActiveRepo(UUID repoUUID) {
        activeRepoUUID = repoUUID;
        // Инвалидируем кэш при изменении активного REPO
        repoExistsCache = null;
        lastRepoSweepTime = null;
    }

    public static void ensureSingleREPO(ServerLevel level) {
        List<REPO> allRepos = new ArrayList<>();

        // Оптимизированный поиск REPO
        for (net.minecraft.world.entity.Entity entity : level.getAllEntities()) {
            if (entity instanceof REPO) {
                allRepos.add((REPO) entity);
            }
        }

        if (allRepos.size() > 1) {
            for (int i = 1; i < allRepos.size(); i++) {
                allRepos.get(i).discard();
            }
            if (!allRepos.isEmpty()) {
                activeRepoUUID = allRepos.get(0).getUUID();
            }
        }
    }
}