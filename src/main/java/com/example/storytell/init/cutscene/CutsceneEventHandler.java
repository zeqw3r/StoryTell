package com.example.storytell.init.cutscene;

import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderGuiEvent;
import net.minecraftforge.client.event.RenderGuiOverlayEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = "storytell", value = Dist.CLIENT)
public class CutsceneEventHandler {
    private static boolean wasCutscenePlaying = false;

    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase == TickEvent.Phase.END) {
            CutsceneManager.getInstance().tick();
        }
    }

    @SubscribeEvent
    public static void onRenderGuiOverlay(RenderGuiOverlayEvent.Pre event) {
        // Кэшируем состояние для оптимизации
        boolean isCutscenePlaying = CutsceneManager.getInstance().isCutscenePlaying();

        if (isCutscenePlaying) {
            event.setCanceled(true);
        }

        wasCutscenePlaying = isCutscenePlaying;
    }

    @SubscribeEvent
    public static void onRenderGui(RenderGuiEvent.Post event) {
        // Используем кэшированное состояние чтобы избежать повторного вызова
        if (wasCutscenePlaying) {
            CutsceneManager.getInstance().render(event.getGuiGraphics());
        }
    }
}