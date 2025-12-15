// PlayerJoinHandler.java
package com.example.storytell.init.star;

import com.example.storytell.init.network.NetworkHandler;
import com.example.storytell.init.network.SyncHologramTexturePacket;
import com.example.storytell.init.network.SyncHologramLockPacket;
import com.example.storytell.init.network.SyncHologramAmbientSoundPacket;
import com.example.storytell.init.network.StarAbsolutePositionPacket;
import com.example.storytell.init.network.StarColorPacket;
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

            // Синхронизируем ВСЕ состояния звезд с новым игроком
            StarManager.syncStarsToPlayer(player);

            // Дополнительная синхронизация для blue_star для надежности
            CustomStar blueStar = StarManager.getStarByName("blue_star");
            if (blueStar != null) {
                // Синхронизируем видимость
                com.example.storytell.init.network.StarVisibilityPacket visibilityPacket =
                        new com.example.storytell.init.network.StarVisibilityPacket(
                                blueStar.getName(), blueStar.isVisible(), false, false
                        );
                NetworkHandler.INSTANCE.send(PacketDistributor.PLAYER.with(() -> player), visibilityPacket);

                // Синхронизируем цвет
                StarColorPacket colorPacket = new StarColorPacket(blueStar.getName(), blueStar.getColor());
                NetworkHandler.INSTANCE.send(PacketDistributor.PLAYER.with(() -> player), colorPacket);

                // Синхронизируем позицию
                StarAbsolutePositionPacket positionPacket = new StarAbsolutePositionPacket(
                        blueStar.getName(),
                        blueStar.getRightAscension(),
                        blueStar.getDeclination(),
                        blueStar.getDistance(),
                        1
                );
                NetworkHandler.INSTANCE.send(PacketDistributor.PLAYER.with(() -> player), positionPacket);

                System.out.println("Extra synchronization for blue_star with player: " + player.getScoreboardName());
            }

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

            System.out.println("Fully synchronized star states with player: " + player.getScoreboardName());
        }
    }
}