package com.example.storytell.init;

import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;
import com.example.storytell.init.blocks.UpgradeTransmitterBlock;

public class BlocksStory {
    public static final DeferredRegister<Block> BLOCKS = DeferredRegister.create(ForgeRegistries.BLOCKS, "storytell");
    public static final DeferredRegister<Item> ITEMS = DeferredRegister.create(ForgeRegistries.ITEMS, "storytell");

    public static final RegistryObject<Block> UPGRADE_TRANSMITTER = BLOCKS.register("upgrade_transmitter",
            () -> new UpgradeTransmitterBlock(BlockBehaviour.Properties.copy(Blocks.IRON_BLOCK).noOcclusion()));

    public static final RegistryObject<Item> UPGRADE_TRANSMITTER_ITEM = ITEMS.register("upgrade_transmitter",
            () -> new BlockItem(UPGRADE_TRANSMITTER.get(), new Item.Properties()));
}
