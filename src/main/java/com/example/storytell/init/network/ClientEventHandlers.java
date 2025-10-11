// ClientEventHandlers.java
package com.example.storytell.init.network;

import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = "storytell", bus = Mod.EventBusSubscriber.Bus.FORGE, value = Dist.CLIENT)
public class ClientEventHandlers {
    private static int redSkyDuration = 0;
    private static long redSkyStartTime = 0;

    public static void handleRedSky(boolean activate, int duration) {
        if (activate) {
            redSkyDuration = duration * 20; // Конвертируем секунды в тики
            redSkyStartTime = System.currentTimeMillis();
        } else {
            redSkyDuration = 0;
        }
    }

    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase == TickEvent.Phase.END && redSkyDuration > 0) {
            redSkyDuration--;

            if (redSkyDuration <= 0) {
                redSkyDuration = 0;
            }
        }
    }

    public static boolean isRedSkyActive() {
        return redSkyDuration > 0;
    }

    public static float getRedSkyIntensity() {
        if (redSkyDuration <= 0) return 0.0f;
        long currentTime = System.currentTimeMillis();
        float progress = (currentTime - redSkyStartTime) / 1000.0f;

        // Плавное появление и исчезание
        if (progress < 2.0f) {
            return progress / 2.0f; // Плавное появление за 2 секунды
        } else if (progress > 58.0f) {
            return Math.max(0.0f, (60.0f - progress) / 2.0f); // Плавное исчезание за 2 секунды
        }
        return 1.0f;
    }
}