// REPOSpawnManager.java
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

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@Mod.EventBusSubscriber(modid = "storytell")
public class REPOSpawnManager {
    private static final Map<UUID, Long> playerCooldowns = new ConcurrentHashMap<>();
    private static final long SPAWN_COOLDOWN = 5 * 60 * 1000; // 5 минут в миллисекундах
    private static final Random random = new Random();
    private static UUID activeRepoUUID = null;

    @SubscribeEvent
    public static void onServerTick(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;

        // ПРОВЕРКА: Если спавн REPO выключен в конфиге - пропускаем
        if (!HologramConfig.isRepoSpawnEnabled()) {
            return;
        }

        // Проверяем спавн каждые 10 секунд для производительности
        if (event.getServer().getTickCount() % 200 != 0) return;

        // Проверяем, существует ли активный REPO
        boolean repoExistsInWorld = false;
        for (ServerLevel level : event.getServer().getAllLevels()) {
            for (net.minecraft.world.entity.Entity entity : level.getAllEntities()) {
                if (entity instanceof REPO) {
                    repoExistsInWorld = true;
                    if (activeRepoUUID == null) {
                        activeRepoUUID = entity.getUUID();
                    }
                    break;
                }
            }
            if (repoExistsInWorld) break;
        }

        // Если REPO существует, не спавним новый
        if (repoExistsInWorld) {
            return;
        }

        // Если активный REPO был удален, сбрасываем UUID
        if (activeRepoUUID != null) {
            boolean repoExists = false;
            for (ServerLevel level : event.getServer().getAllLevels()) {
                if (level.getEntity(activeRepoUUID) != null) {
                    repoExists = true;
                    break;
                }
            }
            if (!repoExists) {
                activeRepoUUID = null;
            } else {
                return;
            }
        }

        // Ищем подходящий уровень для спавна
        for (ServerLevel level : event.getServer().getAllLevels()) {
            if (level.players().isEmpty()) continue;

            // Получаем список всех игроков, для которых можно спавнить REPO
            List<ServerPlayer> eligiblePlayers = getEligiblePlayers(level);

            if (eligiblePlayers.isEmpty()) continue;

            // ВЫБИРАЕМ СЛУЧАЙНОГО ИГРОКА из доступных
            ServerPlayer targetPlayer = eligiblePlayers.get(random.nextInt(eligiblePlayers.size()));
            trySpawnREPO(level, targetPlayer);
            break; // Спавним только одного REPO
        }
    }

    private static List<ServerPlayer> getEligiblePlayers(ServerLevel level) {
        List<ServerPlayer> eligiblePlayers = new ArrayList<>();
        long currentTime = System.currentTimeMillis();

        for (ServerPlayer player : level.players()) {
            UUID playerId = player.getUUID();

            // Проверяем кулдаун для игрока
            Long lastSpawnTime = playerCooldowns.get(playerId);
            if (lastSpawnTime != null && (currentTime - lastSpawnTime) < SPAWN_COOLDOWN) {
                continue; // Кулдаун не прошел
            }

            // Проверяем, что игрок не в безопасной зоне
            if (player.isSleeping() || player.getRespawnPosition() != null) {
                continue;
            }

            // Игрок подходит для спавна
            eligiblePlayers.add(player);
        }

        return eligiblePlayers;
    }

    private static void trySpawnREPO(ServerLevel level, ServerPlayer targetPlayer) {
        Vec3 spawnPos = findSpawnPosition(level, targetPlayer);
        if (spawnPos == null) {
            System.out.println("Не удалось найти позицию для спавна REPO");
            return;
        }

        REPO repo = ModEntities.REPO.get().create(level);
        if (repo == null) {
            System.out.println("Не удалось создать entity REPO");
            return;
        }

        repo.setPos(spawnPos.x, spawnPos.y, spawnPos.z);
        repo.setTargetPlayer(targetPlayer.getUUID());

        level.addFreshEntity(repo);
        spawnSpawnParticles(level, spawnPos);

        activeRepoUUID = repo.getUUID();

        // Устанавливаем кулдаун ДЛЯ ВСЕХ ИГРОКОВ, чтобы следующий REPO появился через 5 минут
        // но не блокируем конкретного игрока
        updateAllPlayerCooldowns();

        System.out.println("REPO заспавнен рядом со случайным игроком: " + targetPlayer.getName().getString() +
                " на позиции: " + spawnPos.x + ", " + spawnPos.y + ", " + spawnPos.z);
    }

    private static void updateAllPlayerCooldowns() {
        long currentTime = System.currentTimeMillis();
        // Устанавливаем время последнего спавна как текущее время
        // Это обеспечит кулдаун 5 минут до следующего спавна
        // Мы не привязываем кулдаун к конкретному игроку
        playerCooldowns.put(UUID.randomUUID(), currentTime); // Просто отмечаем факт спавна
    }

    private static void spawnSpawnParticles(ServerLevel level, Vec3 pos) {
        double x = pos.x;
        double y = pos.y + 1.0;
        double z = pos.z;

        for (int i = 0; i < 30; i++) {
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
        Random random = new Random();

        for (int attempt = 0; attempt < 20; attempt++) {
            double angle = random.nextDouble() * 2 * Math.PI;
            double distance = 20 + random.nextDouble() * 80; // 20-100 блоков

            double x = playerPos.x + Math.cos(angle) * distance;
            double z = playerPos.z + Math.sin(angle) * distance;

            int surfaceY = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, (int) x, (int) z);

            if (surfaceY > level.getMinBuildHeight()) {
                BlockPos spawnPos = new BlockPos((int) x, surfaceY, (int) z);
                BlockPos abovePos = spawnPos.above();

                if (level.isEmptyBlock(spawnPos) && level.isEmptyBlock(abovePos)) {
                    return new Vec3(x, surfaceY + 0.5, z);
                }
            }
        }

        System.out.println("Не удалось найти валидную позицию для спавна после 20 попыток");
        return null;
    }

    public static void setActiveRepo(UUID repoUUID) {
        activeRepoUUID = repoUUID;
    }

    public static void ensureSingleREPO(ServerLevel level) {
        List<REPO> allRepos = new ArrayList<>();

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