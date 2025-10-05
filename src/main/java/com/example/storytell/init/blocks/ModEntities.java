// ModEntities.java
package com.example.storytell.init.blocks;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public class ModEntities {

    public static final DeferredRegister<EntityType<?>> ENTITIES = DeferredRegister.create(ForgeRegistries.ENTITY_TYPES, "storytell");

    public static final RegistryObject<EntityType<HologramEntity>> HOLOGRAM_ENTITY_TYPE =
            ENTITIES.register("hologram_entity", () ->
                    EntityType.Builder.<HologramEntity>of(HologramEntity::new, MobCategory.MISC)
                            .sized(0.5F, 0.5F)
                            .build(new ResourceLocation("storytell", "hologram_entity").toString())
            );

    public static void register() {
        IEventBus bus = FMLJavaModLoadingContext.get().getModEventBus();
        ENTITIES.register(bus);
    }
}