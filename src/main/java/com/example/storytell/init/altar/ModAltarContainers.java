package com.example.storytell.init.altar;

import net.minecraft.world.inventory.MenuType;
import net.minecraftforge.common.extensions.IForgeMenuType;
import net.minecraftforge.registries.RegistryObject;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.common.extensions.IForgeMenuType;

public class ModAltarContainers {
    public static final DeferredRegister<MenuType<?>> CONTAINERS =
            DeferredRegister.create(ForgeRegistries.MENU_TYPES, "storytell");

    public static final RegistryObject<MenuType<SummoningAltarContainer>> SUMMONING_ALTAR_CONTAINER =
            CONTAINERS.register("summoning_altar", () ->
                    IForgeMenuType.create((windowId, inv, data) -> {
                        net.minecraft.core.BlockPos pos = data.readBlockPos();
                        return new SummoningAltarContainer(windowId, inv, pos);
                    }));
}