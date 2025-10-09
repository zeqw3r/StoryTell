// ClientSetup.java
package com.example.storytell.init.renderer;

import com.example.storytell.StoryTell;
import com.example.storytell.init.renderer.RepoModel;
import com.example.storytell.init.renderer.REPORenderer;
import com.example.storytell.init.ModEntities;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.EntityRenderersEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;

@Mod.EventBusSubscriber(modid = StoryTell.MODID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public class ClientSetup {

    @SubscribeEvent
    public static void onClientSetup(FMLClientSetupEvent event) {
        // Здесь можно разместить код, который должен выполниться на клиенте при запуске
    }

    @SubscribeEvent
    public static void registerRenderers(EntityRenderersEvent.RegisterRenderers event) {
        // Регистрируем рендерер для REPO
        event.registerEntityRenderer(ModEntities.REPO.get(), REPORenderer::new);
    }

    @SubscribeEvent
    public static void registerLayerDefinitions(EntityRenderersEvent.RegisterLayerDefinitions event) {
        // Регистрируем модель REPO
        event.registerLayerDefinition(RepoModel.LAYER_LOCATION, RepoModel::createBodyLayer);
    }
}