package com.example.storytell.init.blocks;

import com.example.storytell.init.ModEntities;
import com.example.storytell.init.altar.ModAltarContainers;
import com.example.storytell.init.altar.client.SummoningAltarScreen;
import net.minecraft.client.gui.screens.MenuScreens;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.EntityRenderersEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;

@Mod.EventBusSubscriber(modid = "storytell", value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.MOD)
public class ModClientSetup {

    @SubscribeEvent
    public static void registerRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerEntityRenderer(ModEntities.HOLOGRAM_ENTITY.get(), HologramRenderer::new);
        event.registerBlockEntityRenderer(ModBlockEntities.UPGRADE_TRANSMITTER_BLOCK_ENTITY_TYPE.get(), UpgradeTransmitterRenderer::new);
    }

    @SubscribeEvent
    public static void onClientSetup(FMLClientSetupEvent event) {
        // Регистрируем экран для контейнера алтаря (старый способ для Forge до 1.18)
        event.enqueueWork(() -> {
            MenuScreens.register(ModAltarContainers.SUMMONING_ALTAR_CONTAINER.get(), SummoningAltarScreen::new);
        });
    }
}