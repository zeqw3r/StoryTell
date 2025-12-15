// ServerEventHandler.java
package com.example.storytell.init.star;

import net.minecraftforge.event.server.ServerAboutToStartEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = "storytell")
public class ServerEventHandler {

    @SubscribeEvent
    public static void onServerAboutToStart(ServerAboutToStartEvent event) {
        // Гарантируем инициализацию звезд при запуске сервера
        if (!StarManager.isInitialized()) {
            System.out.println("Server starting - initializing StarManager...");
            StarManager.init();
        }
    }
}