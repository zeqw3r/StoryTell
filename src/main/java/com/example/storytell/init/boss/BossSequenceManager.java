package com.example.storytell.init.boss;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffect;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.server.ServerLifecycleHooks;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class BossSequenceManager {
    private static final List<String> BOSS_SEQUENCE = new ArrayList<>();
    private static final Map<UUID, BossSequenceInstance> activeSequences = new ConcurrentHashMap<>();
    public static final String SEQUENCE_BOSS_TAG = "storytell_sequence_boss";
    private static final int RESPAWN_DELAY_TICKS = 100;

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

    public static void updateSequenceTicks() {
        for (BossSequenceInstance sequence : activeSequences.values()) {
            sequence.tick();
        }
    }

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
            System.out.println("[BossSequence] Starting boss sequence with " + bossQueue.size() + " bosses");
            spawnNextBoss();
        }

        public void tick() {
            if (respawnTimer > 0) {
                respawnTimer--;
                System.out.println("[BossSequence] Respawn timer: " + respawnTimer + " for sequence " + sequenceId);
                if (respawnTimer == 0) {
                    System.out.println("[BossSequence] Spawning next boss now!");
                    spawnNextBoss();
                }
            }
        }

        private void spawnNextBoss() {
            respawnTimer = -1;
            currentBossIndex++;

            System.out.println("[BossSequence] spawnNextBoss called, currentBossIndex: " + currentBossIndex + ", bossQueue size: " + bossQueue.size());

            if (!isActive || currentBossIndex >= bossQueue.size()) {
                sequenceCompleted = true;
                System.out.println("[BossSequence] Sequence completed!");

                // Проигрываем звук при завершении цепочки
                if (level.getServer() != null) {
                    String command = "playsound minecraft:entity.firework_rocket.large_blast_far master @a 0 1000 0 99999999999999 1 1";
                    level.getServer().getCommands().performPrefixedCommand(
                            level.getServer().createCommandSourceStack(),
                            command
                    );
                    System.out.println("[BossSequence] Victory sound played!");
                }

                activeSequences.remove(sequenceId);
                return;
            }

            String bossId = bossQueue.get(currentBossIndex);
            ResourceLocation entityId = new ResourceLocation(bossId);

            System.out.println("[BossSequence] Trying to spawn boss: " + bossId);

            if (ForgeRegistries.ENTITY_TYPES.containsKey(entityId)) {
                try {
                    Entity entity = ForgeRegistries.ENTITY_TYPES.getValue(entityId).create(level);

                    if (entity instanceof LivingEntity) {
                        LivingEntity boss = (LivingEntity) entity;
                        boss.setPos(spawnPos.getX() + 0.5, spawnPos.getY(), spawnPos.getZ() + 0.5);

                        // Применяем эффекты ДО добавления в мир
                        applyBossEffects(boss);
                        boss.addTag(SEQUENCE_BOSS_TAG);
                        level.addFreshEntity(boss);
                        spawnedBosses.add(boss.getUUID());
                        currentBoss = boss;

                        System.out.println("[BossSequence] Successfully spawned boss: " + bossId + " at " +
                                spawnPos.getX() + ", " + spawnPos.getY() + ", " + spawnPos.getZ());
                    }
                } catch (Exception e) {
                    System.out.println("[BossSequence] Error spawning boss " + bossId + ": " + e.getMessage());
                    scheduleNextBoss();
                }
            } else {
                System.out.println("[BossSequence] Unknown boss entity: " + bossId);
                scheduleNextBoss();
            }
        }

        private void applyBossEffects(LivingEntity boss) {
            // Применяем эффекты из конфига
            List<com.example.storytell.init.HologramConfig.BossEffectConfig> effects = com.example.storytell.init.HologramConfig.getBossEffects();
            for (com.example.storytell.init.HologramConfig.BossEffectConfig effectConfig : effects) {
                try {
                    ResourceLocation effectId = new ResourceLocation(effectConfig.effect);
                    MobEffect effect = ForgeRegistries.MOB_EFFECTS.getValue(effectId);
                    if (effect != null) {
                        int duration = effectConfig.duration;
                        if (duration <= 0) duration = Integer.MAX_VALUE; // Бесконечная длительность

                        MobEffectInstance effectInstance = new MobEffectInstance(
                                effect,
                                duration,
                                effectConfig.amplifier,
                                effectConfig.ambient,
                                effectConfig.showParticles
                        );
                        boss.addEffect(effectInstance);
                    } else {
                    }
                } catch (Exception e) {
                }
            }
        }

        public void onBossDeath(UUID bossId, LivingEntity boss) {
            if (spawnedBosses.contains(bossId)) {
                spawnedBosses.remove(bossId);
                currentBoss = null;
                System.out.println("[BossSequence] Boss died, scheduling next boss");
                scheduleNextBoss();
            }
        }

        private void scheduleNextBoss() {
            respawnTimer = RESPAWN_DELAY_TICKS;
            System.out.println("[BossSequence] Scheduled next boss in " + respawnTimer + " ticks");
        }

        public boolean hasBoss(UUID bossId) {
            return spawnedBosses.contains(bossId);
        }

        public LivingEntity getCurrentBoss() {
            return currentBoss;
        }
    }
}