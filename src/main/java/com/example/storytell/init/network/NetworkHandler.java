// NetworkHandler.java
package com.example.storytell.init.network;

import com.example.storytell.init.altar.network.SummoningAltarSelectPacket;
import com.example.storytell.init.blocks.UpgradeTransmitterCommandPacket;
import com.example.storytell.init.shake.CameraBreathPacket;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.simple.SimpleChannel;

public class NetworkHandler {
    private static final String PROTOCOL_VERSION = "1";
    public static final SimpleChannel INSTANCE = NetworkRegistry.newSimpleChannel(
            new ResourceLocation("storytell", "main"),
            () -> PROTOCOL_VERSION,
            PROTOCOL_VERSION::equals,
            PROTOCOL_VERSION::equals
    );

    public static void register() {
        int id = 0;
        INSTANCE.registerMessage(id++, RedSkyPacket.class, RedSkyPacket::encode, RedSkyPacket::new, RedSkyPacket::handle);
        INSTANCE.registerMessage(id++, CameraBreathPacket.class, CameraBreathPacket::encode, CameraBreathPacket::new, CameraBreathPacket::handle);
        INSTANCE.registerMessage(id++, AddWorldModelPacket.class, AddWorldModelPacket::encode, AddWorldModelPacket::decode, AddWorldModelPacket::handle);
        INSTANCE.registerMessage(id++, RemoveWorldModelPacket.class, RemoveWorldModelPacket::encode, RemoveWorldModelPacket::decode, RemoveWorldModelPacket::handle);
        INSTANCE.registerMessage(id++, MoveWorldModelPacket.class, MoveWorldModelPacket::encode, MoveWorldModelPacket::decode, MoveWorldModelPacket::handle);
        INSTANCE.registerMessage(id++, SetRotationSpeedPacket.class, SetRotationSpeedPacket::encode, SetRotationSpeedPacket::decode, SetRotationSpeedPacket::handle);
        INSTANCE.registerMessage(id++, UpgradeTransmitterCommandPacket.class, UpgradeTransmitterCommandPacket::encode, UpgradeTransmitterCommandPacket::decode, UpgradeTransmitterCommandPacket::handle);

        // Новые пакеты для синхронизации звезд
        INSTANCE.registerMessage(id++, StarVisibilityPacket.class, StarVisibilityPacket::encode, StarVisibilityPacket::decode, StarVisibilityPacket::handle);
        INSTANCE.registerMessage(id++, StarMovePacket.class, StarMovePacket::encode, StarMovePacket::decode, StarMovePacket::handle);
        INSTANCE.registerMessage(id++, StarSmoothMovePacket.class, StarSmoothMovePacket::encode, StarSmoothMovePacket::decode, StarSmoothMovePacket::handle);
        INSTANCE.registerMessage(id++, StarModifierPacket.class, StarModifierPacket::encode, StarModifierPacket::decode, StarModifierPacket::handle);
        INSTANCE.registerMessage(id++, SyncAllStarsPacket.class, SyncAllStarsPacket::encode, SyncAllStarsPacket::decode, SyncAllStarsPacket::handle);
        INSTANCE.registerMessage(id++, StarColorPacket.class, StarColorPacket::encode, StarColorPacket::decode, StarColorPacket::handle);
        INSTANCE.registerMessage(id++, StarColorAnimationPacket.class, StarColorAnimationPacket::encode, StarColorAnimationPacket::decode, StarColorAnimationPacket::handle); // Добавлен пакет анимации цвета

        // Пакеты для синхронизации голограмм
        INSTANCE.registerMessage(id++, SyncHologramTextPacket.class, SyncHologramTextPacket::encode, SyncHologramTextPacket::decode, SyncHologramTextPacket::handle);
        INSTANCE.registerMessage(id++, SyncHologramTexturePacket.class, SyncHologramTexturePacket::encode, SyncHologramTexturePacket::decode, SyncHologramTexturePacket::handle);
        INSTANCE.registerMessage(id++, SyncHologramLockPacket.class, SyncHologramLockPacket::encode, SyncHologramLockPacket::decode, SyncHologramLockPacket::handle);
        INSTANCE.registerMessage(id++, SyncHologramAmbientSoundPacket.class, SyncHologramAmbientSoundPacket::encode, SyncHologramAmbientSoundPacket::decode, SyncHologramAmbientSoundPacket::handle);
        INSTANCE.registerMessage(id++, SyncAllStarsPacket.class, SyncAllStarsPacket::encode, SyncAllStarsPacket::decode, SyncAllStarsPacket::handle);
        INSTANCE.registerMessage(id++, SummoningAltarSelectPacket.class,
                SummoningAltarSelectPacket::encode, SummoningAltarSelectPacket::decode,
                SummoningAltarSelectPacket::handle);
        INSTANCE.registerMessage(id++, StarAbsolutePositionPacket.class, StarAbsolutePositionPacket::encode, StarAbsolutePositionPacket::decode, StarAbsolutePositionPacket::handle);
    }
}