package com.example.storytell.init.shake;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.AreaEffectCloud;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.server.ServerLifecycleHooks;

import java.util.*;

@Mod.EventBusSubscriber
public class ScreenShakeHandler {

    private static final Map<UUID, ShakeData> ACTIVE_SHAKES = new HashMap<>();
    private static final Random RANDOM = new Random();

    public static void applyScreenShake(ServerPlayer player, float power, int duration) {
        ACTIVE_SHAKES.put(player.getUUID(), new ShakeData(power, duration, player.getYRot(), player.getXRot()));
    }

    @SubscribeEvent
    public static void onServerTick(TickEvent.ServerTickEvent event) {
        if (event.phase == TickEvent.Phase.END) {
            processScreenShakes();
        }
    }

    private static void processScreenShakes() {
        Iterator<Map.Entry<UUID, ShakeData>> iterator = ACTIVE_SHAKES.entrySet().iterator();

        while (iterator.hasNext()) {
            Map.Entry<UUID, ShakeData> entry = iterator.next();
            UUID playerId = entry.getKey();
            ShakeData data = entry.getValue();

            ServerPlayer player = getPlayerByUUID(playerId);
            if (player == null) {
                iterator.remove();
                continue;
            }

            if (data.timer <= 0) {
                iterator.remove();
                continue;
            }

            executeShakeLogic(player, data);
            data.timer--;
        }
    }

    private static void executeShakeLogic(ServerPlayer player, ShakeData data) {
        // Вычисляем прогресс тряски (от 1 до 0)
        float progress = data.timer / (float)data.duration;

        // Экспоненциальное затухание для более плавного окончания
        float decay = (float)Math.pow(progress, 1.5f);

        // Вычисляем тряску с затуханием
        float trauma = data.power * decay;

        // Генерируем плавные случайные колебания с помощью шума Перлина
        float shakeYaw = calculateShakeOffset(trauma, data.timer, 0);
        float shakePitch = calculateShakeOffset(trauma, data.timer, 1000);

        // Применяем тряску к исходному повороту
        float newYaw = data.initialYaw + shakeYaw;
        float newPitch = data.initialPitch + shakePitch;

        // Ограничиваем pitch в допустимых пределах
        newPitch = Math.max(-90.0f, Math.min(90.0f, newPitch));

        applyShakeEffect(player, newYaw, newPitch);
    }

    private static float calculateShakeOffset(float trauma, int timer, int seedOffset) {
        // Используем упрощенный шум Перлина для плавных колебаний
        float time = timer * 0.1f;

        // Генерируем несколько октав шума для более естественного вида
        float noise = perlinNoise(time + seedOffset, seedOffset) * 0.5f +
                perlinNoise(time * 2f + seedOffset, seedOffset + 100) * 0.25f +
                perlinNoise(time * 4f + seedOffset, seedOffset + 200) * 0.125f;

        // Умножаем на травму для регулировки интенсивности
        return noise * trauma * 25f; // Множитель для усиления эффекта
    }

    private static float perlinNoise(float x, int seed) {
        // Упрощенная реализация шума Перлина
        Random noiseRandom = new Random(seed);
        int x0 = (int)Math.floor(x);
        int x1 = x0 + 1;

        float dx = x - x0;

        // Интерполяция между двумя случайными значениями
        float g0 = noiseRandom.nextFloat() * 2 - 1;
        float g1 = noiseRandom.nextFloat() * 2 - 1;

        // Кубическая интерполяция для плавности
        dx = smoothStep(dx);

        return lerp(g0, g1, dx);
    }

    private static float smoothStep(float x) {
        return x * x * (3 - 2 * x);
    }

    private static float lerp(float a, float b, float t) {
        return a + (b - a) * t;
    }

    private static void applyShakeEffect(ServerPlayer player, float yaw, float pitch) {
        // Создаем AreaEffectCloud для применения поворота
        AreaEffectCloud cloud = new AreaEffectCloud(player.level(),
                player.getX(), player.getY(), player.getZ());
        cloud.setRadius(0.1f);
        cloud.setWaitTime(0);
        cloud.setDuration(2);
        cloud.setFixedColor(0xFF0000);

        cloud.setYRot(yaw);
        cloud.setXRot(pitch);

        player.level().addFreshEntity(cloud);

        // Телепортируем игрока для применения поворота
        player.teleportTo(
                player.getServer().overworld(),
                player.getX(), player.getY(), player.getZ(),
                yaw, pitch
        );

        cloud.discard();
        player.connection.resetPosition();
    }

    private static ServerPlayer getPlayerByUUID(UUID uuid) {
        try {
            return ServerLifecycleHooks.getCurrentServer().getPlayerList().getPlayer(uuid);
        } catch (Exception e) {
            return null;
        }
    }

    private static class ShakeData {
        public float power;
        public int duration;
        public int timer;
        public float initialYaw;
        public float initialPitch;

        public ShakeData(float power, int duration, float initialYaw, float initialPitch) {
            this.power = power;
            this.duration = duration;
            this.timer = duration;
            this.initialYaw = initialYaw;
            this.initialPitch = initialPitch;
        }
    }
}