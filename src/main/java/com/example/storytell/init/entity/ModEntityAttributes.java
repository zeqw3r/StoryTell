// ModEntityAttributes.java
package com.example.storytell.init.entity;

import com.example.storytell.init.ModEntities;
import com.example.storytell.init.entity.REPO;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.Monster;
import net.minecraftforge.event.entity.EntityAttributeCreationEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = "storytell", bus = Mod.EventBusSubscriber.Bus.MOD)
public class ModEntityAttributes {

    @SubscribeEvent
    public static void registerAttributes(EntityAttributeCreationEvent event) {
        event.put(ModEntities.REPO.get(),
                Monster.createMonsterAttributes()
                        .add(Attributes.MAX_HEALTH, 1.0D)
                        .add(Attributes.MOVEMENT_SPEED, 0.0D)
                        .add(Attributes.KNOCKBACK_RESISTANCE, 1.0D)
                        .add(Attributes.ATTACK_DAMAGE, 0.0D)
                        .add(Attributes.FOLLOW_RANGE, 200.0D) // Updated to 200 blocks
                        .build());
    }
}