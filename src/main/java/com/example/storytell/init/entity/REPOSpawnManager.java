// REPOSpawnManager.java
package com.example.storytell.init.entity;

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
    private static final Map<UUID, Long> lastSpawnTimes = new ConcurrentHashMap<>();
    private static final long SPAWN_COOLDOWN = 5 * 60 * 1; // 5 минут в миллисекундах
    private static final Random random = new Random();
    private static UUID activeRepoUUID = null; // UUID активного REPO

    @SubscribeEvent
    public static void onServerTick(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;

        // Проверяем спавн каждые 10 секунд для производительности
        if (event.getServer().getTickCount() % 200 != 0) return;

        // ДОПОЛНИТЕЛЬНАЯ ПРОВЕРКА: убеждаемся, что в мире нет других REPO
        boolean repoExistsInWorld = false;
        for (ServerLevel level : event.getServer().getAllLevels()) {
            // Исправленный способ перебора entities без приведения типа
            for (net.minecraft.world.entity.Entity entity : level.getAllEntities()) {
                if (entity instanceof REPO) {
                    repoExistsInWorld = true;
                    // Обновляем activeRepoUUID, если нашли REPO
                    if (activeRepoUUID == null) {
                        activeRepoUUID = entity.getUUID();
                    }
                    break;
                }
            }
            if (repoExistsInWorld) break;
        }

        // Если нашли REPO в мире, но activeRepoUUID не установлен - устанавливаем
        if (repoExistsInWorld && activeRepoUUID == null) {
            for (ServerLevel level : event.getServer().getAllLevels()) {
                for (net.minecraft.world.entity.Entity entity : level.getAllEntities()) {
                    if (entity instanceof REPO) {
                        activeRepoUUID = entity.getUUID();
                        repoExistsInWorld = true;
                        break;
                    }
                }
                if (activeRepoUUID != null) break;
            }
        }

        // Если REPO уже существует в мире, не спавним новый
        if (repoExistsInWorld) {
            return;
        }

        // Проверяем, существует ли еще активный REPO по UUID
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
                System.out.println("Активный REPO удален из менеджера");
            } else {
                // REPO уже существует, не спавним новый
                return;
            }
        }

        for (ServerLevel level : event.getServer().getAllLevels()) {
            if (level.players().isEmpty()) continue;

            // Получаем список игроков, которые могут видеть REPO
            List<ServerPlayer> potentialTargets = new ArrayList<>();
            for (ServerPlayer player : level.players()) {
                if (canSpawnForPlayer(player)) {
                    potentialTargets.add(player);
                }
            }

            if (potentialTargets.isEmpty()) continue;

            // Выбираем случайного игрока
            ServerPlayer targetPlayer = potentialTargets.get(random.nextInt(potentialTargets.size()));
            trySpawnREPO(level, targetPlayer);
            break; // Спавним только одного REPO
        }
    }

    private static boolean canSpawnForPlayer(ServerPlayer player) {
        UUID playerId = player.getUUID();
        long currentTime = System.currentTimeMillis();

        // Проверяем кулдаун
        Long lastSpawn = lastSpawnTimes.get(playerId);
        if (lastSpawn != null && (currentTime - lastSpawn) < SPAWN_COOLDOWN) {
            return false;
        }

        // Проверяем, что игрок не в безопасной зоне (кровать, дом и т.д.)
        return !player.isSleeping() && player.getRespawnPosition() == null;
    }

    private static void trySpawnREPO(ServerLevel level, ServerPlayer targetPlayer) {
        // Генерируем позицию в радиусе 20-100 блоков от игрока
        Vec3 spawnPos = findSpawnPosition(level, targetPlayer);
        if (spawnPos == null) {
            System.out.println("Не удалось найти позицию для спавна REPO");
            return;
        }

        // Создаем REPO
        REPO repo = ModEntities.REPO.get().create(level);
        if (repo == null) {
            System.out.println("Не удалось создать entity REPO");
            return;
        }

        // Устанавливаем позицию и целевого игрока
        repo.setPos(spawnPos.x, spawnPos.y, spawnPos.z);
        repo.setTargetPlayer(targetPlayer.getUUID());

        // Спавним в мире
        level.addFreshEntity(repo);

        // Создаем частицы при появлении
        spawnSpawnParticles(level, spawnPos);

        // Устанавливаем как активного REPO
        activeRepoUUID = repo.getUUID();

        // Обновляем время последнего спавна
        lastSpawnTimes.put(targetPlayer.getUUID(), System.currentTimeMillis());

        System.out.println("REPO заспавнен рядом с игроком: " + targetPlayer.getName().getString() +
                " на позиции: " + spawnPos.x + ", " + spawnPos.y + ", " + spawnPos.z);
    }

    // Новый метод: создание частиц при появлении REPO
    private static void spawnSpawnParticles(ServerLevel level, Vec3 pos) {
        // Используем частицы портала (фиолетовые, как у эндермена)
        double x = pos.x;
        double y = pos.y + 1.0; // Немного выше позиции спавна
        double z = pos.z;

        // Создаем много частиц для эффектного появления
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

            // Находим высоту поверхности
            int surfaceY = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, (int) x, (int) z);

            if (surfaceY > level.getMinBuildHeight()) {
                // Проверяем, что позиция валидна (не внутри блока)
                BlockPos spawnPos = new BlockPos((int) x, surfaceY, (int) z);
                BlockPos abovePos = spawnPos.above();

                if (level.isEmptyBlock(spawnPos) && level.isEmptyBlock(abovePos)) {
                    // Ставим на 0.5 выше поверхности для надежности
                    return new Vec3(x, surfaceY + 0.5, z);
                }
            }
        }

        System.out.println("Не удалось найти валидную позицию для спавна после 20 попыток");
        return null;
    }

    public static void updateSpawnTime(UUID playerId) {
        lastSpawnTimes.put(playerId, System.currentTimeMillis());
    }

    public static void setActiveRepo(UUID repoUUID) {
        activeRepoUUID = repoUUID;
    }

    // Новый метод для принудительной проверки и удаления лишних REPO
    public static void ensureSingleREPO(ServerLevel level) {
        List<REPO> allRepos = new ArrayList<>();

        // Собираем всех REPO в мире
        for (net.minecraft.world.entity.Entity entity : level.getAllEntities()) {
            if (entity instanceof REPO) {
                allRepos.add((REPO) entity);
            }
        }

        // Если найдено больше одного REPO, оставляем только первого
        if (allRepos.size() > 1) {
            System.out.println("Найдено " + allRepos.size() + " REPO, удаляем лишние");
            for (int i = 1; i < allRepos.size(); i++) {
                allRepos.get(i).discard();
            }
            // Устанавливаем активным оставшегося REPO
            if (!allRepos.isEmpty()) {
                activeRepoUUID = allRepos.get(0).getUUID();
            }
        }
    }
}