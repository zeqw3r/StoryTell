// EvokerLootFix.java
package com.example.storytell.init.fix;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.storage.loot.LootPool;
import net.minecraft.world.level.storage.loot.entries.LootItem;
import net.minecraft.world.level.storage.loot.functions.SetItemCountFunction;
import net.minecraft.world.level.storage.loot.predicates.LootItemRandomChanceCondition;
import net.minecraft.world.level.storage.loot.providers.number.UniformGenerator;
import net.minecraftforge.event.LootTableLoadEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = "storytell", bus = Mod.EventBusSubscriber.Bus.FORGE)
public class EvokerLootFix {

    private static final ResourceLocation EVOKER_LOOT_TABLE =
            new ResourceLocation("minecraft", "entities/evoker");

    @SubscribeEvent
    public static void onLootTableLoad(LootTableLoadEvent event) {
        if (!event.getName().equals(EVOKER_LOOT_TABLE)) {
            return;
        }

        // Remove the original pool containing the totem
        event.getTable().removePool("main");

        // Create a new pool with only emerald (no totem)
        // Using the same parameters as vanilla evoker loot
        LootPool.Builder newPoolBuilder = LootPool.lootPool()
                .name("main")
                .setRolls(net.minecraft.world.level.storage.loot.providers.number.ConstantValue.exactly(1.0F))
                .add(LootItem.lootTableItem(Items.EMERALD)
                        .setWeight(1)
                        .apply(SetItemCountFunction.setCount(UniformGenerator.between(0.0F, 1.0F))))
                .when(LootItemRandomChanceCondition.randomChance(0.5F)); // 50% chance like vanilla

        event.getTable().addPool(newPoolBuilder.build());
    }
}