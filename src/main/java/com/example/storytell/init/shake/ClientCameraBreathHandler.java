package com.example.storytell.init.shake;

import net.minecraft.client.Minecraft;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ViewportEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = "storytell", bus = Mod.EventBusSubscriber.Bus.FORGE, value = Dist.CLIENT)
public class ClientCameraBreathHandler {
    private static float targetPower = 0f;
    private static int duration = 0;
    private static int timer = 0;
    private static float originalFOV = 70f;
    private static boolean isActive = false;
    private static long startTime = 0;

    public static void handleCameraBreath(float power, int newDuration, boolean activate) {
        if (activate) {
            targetPower = power;
            duration = newDuration;
            timer = newDuration;
            originalFOV = Minecraft.getInstance().options.fov().get();
            isActive = true;
            startTime = System.currentTimeMillis();
        } else {
            isActive = false;
            timer = 0;
        }
    }

    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase == TickEvent.Phase.END && isActive) {
            if (timer > 0) {
                timer--;
            } else {
                isActive = false;
            }
        }
    }

    @SubscribeEvent
    public static void onFOVUpdate(ViewportEvent.ComputeFov event) {
        if (!isActive || timer <= 0) return;

        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) return;

        // Вычисляем прогресс эффекта (от 0 до 1)
        float progress = 1.0f - (timer / (float)duration);

        // Определяем фазы эффекта
        float fovChange;

        if (progress < 0.9f) {
            // Фаза 1: плавное уменьшение FOV (90% времени)
            // Используем квадратичную функцию для плавного ускорения/замедления
            float phaseProgress = progress / 0.9f; // Нормализуем до 0-1 для первой фазы
            fovChange = easeInOutQuad(phaseProgress) * targetPower * 25f;
        } else {
            // Фаза 2: плавное возвращение к исходному FOV (последние 10% времени)
            float phaseProgress = (progress - 0.9f) / 0.1f; // Нормализуем до 0-1 для второй фазы
            fovChange = (1f - easeInOutQuad(phaseProgress)) * targetPower * 25f;
        }

        // Применяем изменение FOV (уменьшение FOV = приближение)
        float newFOV = originalFOV - fovChange;

        // Ограничиваем FOV в разумных пределах
        newFOV = Math.max(30f, Math.min(110f, newFOV));

        event.setFOV(newFOV);
    }

    // Функция плавного ускорения и замедления
    private static float easeInOutQuad(float x) {
        return x < 0.5f ? 2 * x * x : 1 - (float)Math.pow(-2 * x + 2, 2) / 2;
    }

    // Функция плавного начала
    private static float easeOutQuad(float x) {
        return 1 - (1 - x) * (1 - x);
    }

    // Функция плавного окончания
    private static float easeInQuad(float x) {
        return x * x;
    }

    public static boolean isActive() {
        return isActive;
    }

    public static float getCurrentProgress() {
        if (!isActive || duration == 0) return 0f;
        return 1.0f - (timer / (float)duration);
    }
}