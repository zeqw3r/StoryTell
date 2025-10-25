// NetworkHandler.java
package com.example.storytell.init.network;

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
    }
}