package com.example.storytell.init.radio;

import net.minecraftforge.registries.RegistryObject;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraft.world.level.block.entity.BlockEntityType;

public class ModRadioBlockEntities {

    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES =
            DeferredRegister.create(ForgeRegistries.BLOCK_ENTITY_TYPES, "storytell");

    public static final RegistryObject<BlockEntityType<RadioBlockEntity>> RADIO_BLOCK_ENTITY_TYPE =
            BLOCK_ENTITIES.register("radio", () ->
                    BlockEntityType.Builder.of(RadioBlockEntity::new,
                            ModRadioBlocks.RADIO_BLOCK.get()).build(null));
}