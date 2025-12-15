package com.example.storytell.init.shake;

import com.example.storytell.StoryTell;
import com.example.storytell.init.util.CustomPerlinNoiseGenerator;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.AreaEffectCloud;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.server.ServerLifecycleHooks;
import org.joml.Vector2f;

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

        // Вычисляем тряску с затуханием
        float trauma = progress * data.power;

        float maximumAngularShake = 2f * data.power;

        Vector2f new_rot = new Vector2f(
                maximumAngularShake * (calculateShakeOffset(data.power, data.timer, data.seed, 0, 0.5f) * trauma),
                maximumAngularShake * (calculateShakeOffset(data.power, data.timer, data.seed, 100, 0.5f) * trauma));

        float shakeYaw = new_rot.x;
        float shakePitch = new_rot.y;

        // Применяем тряску к исходному повороту
        float newYaw = data.initialYaw + shakeYaw;
        float newPitch = data.initialPitch + shakePitch;

        // Ограничиваем pitch в допустимых пределах
        newPitch = Math.max(-90.0f, Math.min(90.0f, newPitch));

        applyShakeEffect(player, newYaw, newPitch);
    }

    private static float calculateShakeOffset(float size, int timer, double seed, int seedOffset, float smooth_factor) {
        // Используем упрощенный шум Перлина для плавных колебаний
        float time = timer * smooth_factor;
        CustomPerlinNoiseGenerator generator = new CustomPerlinNoiseGenerator();

        // Умножаем на травму для регулировки интенсивности
        return (float) generator.noise(time + seedOffset, seed + seedOffset, size);
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
        public double seed;

        public ShakeData(float power, int duration, float initialYaw, float initialPitch) {
            this.power = power;
            this.duration = duration;
            this.timer = duration;
            this.initialYaw = initialYaw;
            this.initialPitch = initialPitch;
            this.seed = RANDOM.nextDouble();
        }
    }
}