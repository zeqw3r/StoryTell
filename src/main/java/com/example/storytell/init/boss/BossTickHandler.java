// BossTickHandler.java
package com.example.storytell.init.boss;

import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = "storytell", bus = Mod.EventBusSubscriber.Bus.FORGE)
public class BossTickHandler {
    private static int tickCounter = 0;
    private static int distanceCheckCounter = 0;

    @SubscribeEvent
    public static void onServerTick(TickEvent.ServerTickEvent event) {
        if (event.phase == TickEvent.Phase.END) {
            tickCounter++;
            distanceCheckCounter++;

            // Проверяем каждые 2 секунды (40 тиков) для более быстрого реагирования
            if (tickCounter >= 40) {
                BossEventHandler.checkSpectatorPlayers();
                tickCounter = 0;
            }

            // Проверяем расстояние каждые 20 тиков (1 секунда)
            if (distanceCheckCounter >= 20) {
                BossEventHandler.checkSpectatorDistances();
                distanceCheckCounter = 0;
            }
        }
    }
}