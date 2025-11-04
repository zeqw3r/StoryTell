// BossSequenceEventHandler.java
package com.example.storytell.init.boss;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.core.BlockPos;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = "storytell", bus = Mod.EventBusSubscriber.Bus.FORGE)
public class BossSequenceEventHandler {

    @SubscribeEvent
    public static void onPlayerDeath(LivingDeathEvent event) {
        if (event.getEntity() instanceof ServerPlayer && !event.getEntity().level().isClientSide()) {
            ServerPlayer player = (ServerPlayer) event.getEntity();

            // Убираем мгновенное возрождение - теперь игроки остаются в режиме наблюдателя
            // до тех пор, пока все игроки не умрут или цепочка не будет завершена
            BossSequenceManager.onPlayerDeath(player);
        }
    }

    // Убираем метод checkIfAllPlayersDead, так как он больше не нужен
}