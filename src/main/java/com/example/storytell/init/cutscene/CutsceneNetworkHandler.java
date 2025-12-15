package com.example.storytell.init.cutscene;

import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.simple.SimpleChannel;

public class CutsceneNetworkHandler {
    private static final String PROTOCOL_VERSION = "1";
    public static final SimpleChannel INSTANCE = NetworkRegistry.newSimpleChannel(
            new ResourceLocation("storytell", "cutscene"),
            () -> PROTOCOL_VERSION,
            PROTOCOL_VERSION::equals,
            PROTOCOL_VERSION::equals
    );

    private static int packetId = 0;

    public static void register() {
        INSTANCE.registerMessage(packetId++, CutsceneStartPacket.class,
                CutsceneStartPacket::toBytes, CutsceneStartPacket::new, CutsceneStartPacket::handle);

        System.out.println("Cutscene network handler registered successfully");
    }
}