// CutsceneEventHandler.java
package com.example.storytell.init.cutscene;

import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderGuiEvent;
import net.minecraftforge.client.event.RenderGuiOverlayEvent;
import net.minecraftforge.client.gui.overlay.VanillaGuiOverlay;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = "storytell", value = Dist.CLIENT)
public class CutsceneEventHandler {

    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase == TickEvent.Phase.END) {
            CutsceneManager.getInstance().tick();
        }
    }

    @SubscribeEvent
    public static void onRenderGuiOverlay(RenderGuiOverlayEvent.Pre event) {
        // Блокируем рендер всех элементов интерфейса во время катсцены
        if (CutsceneManager.getInstance().isCutscenePlaying()) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public static void onRenderGui(RenderGuiEvent.Post event) {
        // Рендерим катсцену после блокировки всех остальных элементов
        if (CutsceneManager.getInstance().isCutscenePlaying()) {
            CutsceneManager.getInstance().render(event.getGuiGraphics());
        }
    }
}