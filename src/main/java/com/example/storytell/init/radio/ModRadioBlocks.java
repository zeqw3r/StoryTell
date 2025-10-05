// ModRadioBlocks.java
package com.example.storytell.init.radio;

import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.material.MapColor;
import net.minecraftforge.registries.RegistryObject;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;

public class ModRadioBlocks {
    public static final DeferredRegister<Block> BLOCKS =
            DeferredRegister.create(ForgeRegistries.BLOCKS, "storytell");

    public static final RegistryObject<Block> RADIO_BLOCK =
            BLOCKS.register("radio", () ->
                    new RadioBlock(Block.Properties.of()
                            .mapColor(MapColor.WOOD)
                            .sound(SoundType.WOOD)
                            .strength(1.5f)
                            .noOcclusion()
                            .dynamicShape()));
}