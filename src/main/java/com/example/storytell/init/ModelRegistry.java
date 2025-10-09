// ModelRegistry.java
package com.example.storytell.init;

import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ModelEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = "storytell", bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public class ModelRegistry {

    @SubscribeEvent
    public static void onModelRegistry(ModelEvent.RegisterAdditional event) {
        // Register our custom models
        event.register(new ResourceLocation("storytell:star/blue_star"));
        System.out.println("Registered custom star model: storytell:star/blue_star");
    }
}