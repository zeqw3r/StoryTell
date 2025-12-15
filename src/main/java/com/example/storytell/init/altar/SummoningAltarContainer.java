package com.example.storytell.init.altar;

import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.core.BlockPos;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;

public class SummoningAltarContainer extends AbstractContainerMenu {
    private final BlockPos pos;

    public SummoningAltarContainer(int containerId, Inventory playerInventory, BlockPos pos) {
        super(ModAltarContainers.SUMMONING_ALTAR_CONTAINER.get(), containerId);
        this.pos = pos;
    }

    public SummoningAltarContainer(int containerId, Inventory playerInventory, SummoningAltarBlockEntity blockEntity) {
        this(containerId, playerInventory, blockEntity.getBlockPos());
    }

    public BlockPos getBlockPos() {
        return pos;
    }

    @Override
    public ItemStack quickMoveStack(Player pPlayer, int pIndex) {
        return null;
    }

    @Override
    public boolean stillValid(Player player) {
        return player.distanceToSqr(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5) <= 64.0;
    }
}