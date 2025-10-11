// NetworkHandler.java
package com.example.storytell.init.network;

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
    }

}