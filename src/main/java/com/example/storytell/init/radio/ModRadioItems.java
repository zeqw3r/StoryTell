// ModRadioItems.java
package com.example.storytell.init.radio;

import net.minecraft.world.item.Item;
import net.minecraftforge.registries.RegistryObject;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;

public class ModRadioItems {
    public static final DeferredRegister<Item> ITEMS =
            DeferredRegister.create(ForgeRegistries.ITEMS, "storytell");

    // Используем кастомный RadioBlockItem вместо стандартного BlockItem
    public static final RegistryObject<Item> RADIO_BLOCK_ITEM =
            ITEMS.register("radio", () ->
                    new RadioBlockItem(ModRadioBlocks.RADIO_BLOCK.get(),
                            new Item.Properties()));
}