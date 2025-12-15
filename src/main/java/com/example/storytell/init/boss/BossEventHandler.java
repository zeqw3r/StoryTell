package com.example.storytell.init.boss;

import com.example.storytell.init.HologramConfig;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.event.entity.EntityJoinLevelEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.registries.ForgeRegistries;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.*;

@Mod.EventBusSubscriber(modid = "storytell", bus = Mod.EventBusSubscriber.Bus.FORGE)
public class BossEventHandler {
    private static final Logger LOGGER = LogManager.getLogger();

    // Множество для отслеживания обработанных боссов
    private static final Set<UUID> processedBosses = Collections.newSetFromMap(new WeakHashMap<>());

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

        // Пропускаем уже обработанных боссов
        if (processedBosses.contains(livingEntity.getUUID())) {
            return;
        }

        // Пропускаем боссов из цепочки
        if (BossSequenceManager.isSequenceBoss(livingEntity)) {
            return;
        }

        // Используем кэшированную проверку босса
        if (!isBossCached(entityId)) {
            return;
        }

        // Применяем эффекты к обычным боссам
        applyBossEffects(livingEntity);

        // Регистрируем босса как обработанного
        processedBosses.add(livingEntity.getUUID());

        LOGGER.info("[BossEffectsHandler] Applied effects to regular boss: {} (UUID: {})", entityId, livingEntity.getUUID());
    }

    private static void applyBossEffects(LivingEntity boss) {
        // Применяем эффекты из конфига
        List<HologramConfig.BossEffectConfig> effects = HologramConfig.getBossEffects();
        for (HologramConfig.BossEffectConfig effectConfig : effects) {
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

                    LOGGER.debug("[BossEffectsHandler] Applied effect {} (level {}) to boss: {}",
                            effectId, effectConfig.amplifier + 1,
                            ForgeRegistries.ENTITY_TYPES.getKey(boss.getType()));
                } else {
                    LOGGER.warn("[BossEffectsHandler] Unknown effect: {}", effectConfig.effect);
                }
            } catch (Exception e) {
                LOGGER.error("[BossEffectsHandler] Error applying effect {}: {}", effectConfig.effect, e.getMessage());
            }
        }

        LOGGER.info("[BossEffectsHandler] Applied {} effects to boss: {}",
                effects.size(), ForgeRegistries.ENTITY_TYPES.getKey(boss.getType()));
    }

    // Периодическая очистка кэша конфига
    public static void cleanupCache() {
        if (System.currentTimeMillis() - lastConfigCacheClear > CONFIG_CACHE_TTL) {
            bossConfigCache.clear();
            lastConfigCacheClear = System.currentTimeMillis();
            LOGGER.debug("[BossEffectsHandler] Config cache cleared");
        }
    }

    // Кэшированная проверка босса
    private static boolean isBossCached(ResourceLocation entityId) {
        return bossConfigCache.computeIfAbsent(entityId, HologramConfig::isBoss);
    }
}