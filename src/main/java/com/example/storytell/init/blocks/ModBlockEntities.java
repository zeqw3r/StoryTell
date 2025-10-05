package com.example.storytell.init.blocks;

import net.minecraftforge.registries.RegistryObject;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.resources.ResourceLocation;

public class ModBlockEntities {

    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES =
            DeferredRegister.create(ForgeRegistries.BLOCK_ENTITY_TYPES, "storytell");

    // Правильная регистрация BlockEntity
    public static final RegistryObject<BlockEntityType<UpgradeTransmitterBlockEntity>> UPGRADE_TRANSMITTER_BLOCK_ENTITY_TYPE =
            BLOCK_ENTITIES.register("upgrade_transmitter", () ->
                    BlockEntityType.Builder.of(UpgradeTransmitterBlockEntity::new,
                            ModBlocks.UPGRADE_TRANSMITTER_BLOCK.get()).build(null));
}