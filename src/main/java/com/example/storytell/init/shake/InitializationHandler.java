package com.example.storytell.init.shake;

import net.minecraftforge.event.server.ServerStartingEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber
public class InitializationHandler {

    @SubscribeEvent
    public static void onServerStarting(ServerStartingEvent event) {
        // Теперь инициализация не требуется, так как мы убрали систему scoreboard
        // Оставляем класс для возможных будущих инициализаций
    }
}