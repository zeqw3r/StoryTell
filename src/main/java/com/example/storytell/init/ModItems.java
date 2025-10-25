// ModItems.java
package com.example.storytell.init;

import com.example.storytell.init.blocks.ModBlocks;
import com.example.storytell.init.blocks.UpgradeTransmitterBlockItem;
import com.example.storytell.init.item.RobotShard;
import com.example.storytell.init.tablet.NewsTabletItem;
import net.minecraft.world.item.Item;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public class ModItems {
    public static final DeferredRegister<Item> ITEMS =
            DeferredRegister.create(ForgeRegistries.ITEMS, "storytell");

    public static final RegistryObject<Item> ROBOT_SHARD = ITEMS.register("robot_shard",
            () -> new RobotShard());

    public static final RegistryObject<Item> UPGRADE_TRANSMITTER_BLOCK_ITEM =
            ITEMS.register("upgrade_transmitter", () ->
                    new UpgradeTransmitterBlockItem(ModBlocks.UPGRADE_TRANSMITTER_BLOCK.get(),
                            new Item.Properties()));
    public static final RegistryObject<Item> NEWS_TABLET = ITEMS.register("news_tablet",
            () -> new NewsTabletItem(new Item.Properties().stacksTo(1)));
}