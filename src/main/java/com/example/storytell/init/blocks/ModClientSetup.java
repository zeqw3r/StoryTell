// ModClientSetup.java
package com.example.storytell.init.blocks;

import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.EntityRenderersEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;

@Mod.EventBusSubscriber(modid = "storytell", value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.MOD)
public class ModClientSetup {

    @SubscribeEvent
    public static void registerRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerEntityRenderer(ModEntities.HOLOGRAM_ENTITY_TYPE.get(), HologramRenderer::new);
        event.registerBlockEntityRenderer(ModBlockEntities.UPGRADE_TRANSMITTER_BLOCK_ENTITY_TYPE.get(), UpgradeTransmitterRenderer::new);
    }

    @SubscribeEvent
    public static void onClientSetup(FMLClientSetupEvent event) {
        // Дополнительная клиентская настройка если нужно
    }
}