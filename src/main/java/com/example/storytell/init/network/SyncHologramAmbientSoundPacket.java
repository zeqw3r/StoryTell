package com.example.storytell.init.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;
import com.example.storytell.init.HologramConfig;

import java.util.function.Supplier;

public class SyncHologramAmbientSoundPacket {
    private final String ambientSound;

    public SyncHologramAmbientSoundPacket(String ambientSound) {
        this.ambientSound = ambientSound;
    }

    public static void encode(SyncHologramAmbientSoundPacket packet, FriendlyByteBuf buffer) {
        buffer.writeUtf(packet.ambientSound);
    }

    public static SyncHologramAmbientSoundPacket decode(FriendlyByteBuf buffer) {
        return new SyncHologramAmbientSoundPacket(buffer.readUtf());
    }

    public static void handle(SyncHologramAmbientSoundPacket packet, Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();
        context.enqueueWork(() -> {
            // Устанавливаем ambient sound в клиентском HologramConfig
            HologramConfig.setHologramAmbientSoundClient(packet.ambientSound);
        });
        context.setPacketHandled(true);
    }
}