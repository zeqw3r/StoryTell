package com.example.storytell.init.boss;

import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = "storytell", bus = Mod.EventBusSubscriber.Bus.FORGE)
public class BossTickHandler {
    private static int tickCounter = 0;
    private static int sequenceCheckCounter = 0;

    @SubscribeEvent
    public static void onServerTick(TickEvent.ServerTickEvent event) {
        if (event.phase == TickEvent.Phase.END) {
            tickCounter++;
            sequenceCheckCounter++;

            // Проверяем обычных боссов каждые 4 секунды (вместо 2)
            if (tickCounter >= 80) {
                BossEventHandler.checkSpectatorPlayers();
                tickCounter = 0;
            }

            // Проверяем последовательности боссов каждые 2 секунды (вместо 1)
            if (sequenceCheckCounter >= 80) {
                BossSequenceManager.updateSequencePlayers();
                BossEventHandler.checkSpectatorDistances();

                // Обновляем таймеры последовательностей
                for (BossSequenceManager.BossSequenceInstance sequence : BossSequenceManager.getActiveSequences().values()) {
                    sequence.tick();
                }

                sequenceCheckCounter = 0;
            }
        }
    }
}