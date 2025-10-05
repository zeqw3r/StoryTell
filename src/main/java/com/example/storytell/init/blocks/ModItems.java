package com.example.storytell.init.blocks;

import net.minecraft.world.item.Item;
import net.minecraftforge.registries.RegistryObject;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;

public class ModItems {
    public static final DeferredRegister<Item> ITEMS =
            DeferredRegister.create(ForgeRegistries.ITEMS, "storytell");

    // Используем кастомный UpgradeTransmitterBlockItem вместо стандартного BlockItem
    public static final RegistryObject<Item> UPGRADE_TRANSMITTER_BLOCK_ITEM =
            ITEMS.register("upgrade_transmitter", () ->
                    new UpgradeTransmitterBlockItem(ModBlocks.UPGRADE_TRANSMITTER_BLOCK.get(),
                            new Item.Properties()));
}