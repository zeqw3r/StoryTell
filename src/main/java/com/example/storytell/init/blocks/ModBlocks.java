package com.example.storytell.init.blocks;

import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.material.MapColor;
import net.minecraftforge.registries.RegistryObject;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;

public class ModBlocks {
    public static final DeferredRegister<Block> BLOCKS =
            DeferredRegister.create(ForgeRegistries.BLOCKS, "storytell");

    public static final RegistryObject<Block> UPGRADE_TRANSMITTER_BLOCK =
            BLOCKS.register("upgrade_transmitter", () ->
                    new UpgradeTransmitterBlock(Block.Properties.of()
                            .mapColor(MapColor.METAL)
                            .sound(SoundType.METAL)
                            .strength(2.0f)
                            .requiresCorrectToolForDrops()
                            .noOcclusion())); // важно для правильного рендеринга неполного блока
}