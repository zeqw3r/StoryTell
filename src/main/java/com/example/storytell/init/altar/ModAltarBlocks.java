// altar/ModAltarBlocks.java
package com.example.storytell.init.altar;

import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraftforge.registries.RegistryObject;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;

public class ModAltarBlocks {
    public static final DeferredRegister<Block> BLOCKS =
            DeferredRegister.create(ForgeRegistries.BLOCKS, "storytell");

    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES =
            DeferredRegister.create(ForgeRegistries.BLOCK_ENTITY_TYPES, "storytell");

    public static final RegistryObject<Block> SUMMONING_ALTAR_BLOCK =
            BLOCKS.register("summoning_altar", () ->
                    new SummoningAltarBlock(Block.Properties.of()
                            .mapColor(MapColor.STONE)
                            .sound(SoundType.STONE)
                            .strength(-1.0F, Float.MAX_VALUE)
                            .requiresCorrectToolForDrops()
                            .noOcclusion()
                            .lightLevel(state -> 5)
                            .emissiveRendering((state, level, pos) -> true)));

    // ВАЖНО: Используем ленивую инициализацию для BlockEntityType
    public static final RegistryObject<BlockEntityType<SummoningAltarBlockEntity>> SUMMONING_ALTAR_BLOCK_ENTITY_TYPE =
            BLOCK_ENTITIES.register("summoning_altar", () ->
                    BlockEntityType.Builder.of(SummoningAltarBlockEntity::new, SUMMONING_ALTAR_BLOCK.get()).build(null));
}