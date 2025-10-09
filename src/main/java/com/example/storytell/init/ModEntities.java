// ModEntities.java
package com.example.storytell.init;

import com.example.storytell.StoryTell;
import com.example.storytell.init.entity.REPO;
import com.example.storytell.init.blocks.HologramEntity;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraftforge.registries.RegistryObject;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;

public class ModEntities {
    public static final DeferredRegister<EntityType<?>> ENTITY_TYPES =
            DeferredRegister.create(ForgeRegistries.ENTITY_TYPES, StoryTell.MODID);

    // Регистрация сущности REPO
    public static final RegistryObject<EntityType<REPO>> REPO = ENTITY_TYPES.register("repo",
            () -> EntityType.Builder.of(REPO::new, MobCategory.MONSTER)
                    .sized(1.0f, 2.0f)
                    .clientTrackingRange(8)
                    .build(new ResourceLocation(StoryTell.MODID, "repo").toString()));

    // Регистрация сущности HologramEntity
    public static final RegistryObject<EntityType<HologramEntity>> HOLOGRAM_ENTITY =
            ENTITY_TYPES.register("hologram_entity",
                    () -> EntityType.Builder.<HologramEntity>of(HologramEntity::new, MobCategory.MISC)
                            .sized(0.5F, 0.5F)
                            .build(new ResourceLocation(StoryTell.MODID, "hologram_entity").toString()));

    public static void register() {
        // Метод оставлен для обратной совместимости
    }
}