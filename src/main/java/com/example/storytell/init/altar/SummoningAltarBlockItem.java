// altar/SummoningAltarBlockItem.java
package com.example.storytell.init.altar;

import net.minecraft.network.chat.Component;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class SummoningAltarBlockItem extends BlockItem {

    public SummoningAltarBlockItem(Block block, Properties properties) {
        super(block, properties);
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
        super.appendHoverText(stack, level, tooltip, flag);
        tooltip.add(Component.translatable("tooltip.storytell.altar.description"));
        tooltip.add(Component.translatable("tooltip.storytell.altar.unbreakable").withStyle(net.minecraft.ChatFormatting.DARK_RED));
        tooltip.add(Component.translatable("tooltip.storytell.altar.usage").withStyle(net.minecraft.ChatFormatting.GRAY));
    }
}