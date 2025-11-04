// PlayerJoinHandler.java
package com.example.storytell.init.star;

import com.example.storytell.init.network.NetworkHandler;
import com.example.storytell.init.network.SyncHologramTexturePacket;
import com.example.storytell.init.network.SyncHologramLockPacket;
import com.example.storytell.init.network.SyncHologramAmbientSoundPacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.network.PacketDistributor;

@Mod.EventBusSubscriber(modid = "storytell")
public class PlayerJoinHandler {

    @SubscribeEvent
    public static void onPlayerLogin(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer) {
            ServerPlayer player = (ServerPlayer) event.getEntity();

            // Синхронизируем состояние звезд с новым игроком
            StarManager.syncStarsToPlayer(player);

            // Синхронизируем настройки голограммы с новым игроком
            String currentTexture = com.example.storytell.init.HologramConfig.getHologramTexture().toString();
            boolean currentLockState = com.example.storytell.init.HologramConfig.isHologramLocked();
            String currentAmbientSound = com.example.storytell.init.HologramConfig.getHologramAmbientSoundLocation();

            SyncHologramTexturePacket texturePacket = new SyncHologramTexturePacket(currentTexture);
            SyncHologramLockPacket lockPacket = new SyncHologramLockPacket(currentLockState);
            SyncHologramAmbientSoundPacket soundPacket = new SyncHologramAmbientSoundPacket(currentAmbientSound);

            NetworkHandler.INSTANCE.send(PacketDistributor.PLAYER.with(() -> player), texturePacket);
            NetworkHandler.INSTANCE.send(PacketDistributor.PLAYER.with(() -> player), lockPacket);
            NetworkHandler.INSTANCE.send(PacketDistributor.PLAYER.with(() -> player), soundPacket);
        }
    }
}