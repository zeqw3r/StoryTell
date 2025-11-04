package com.example.storytell.init.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;
import com.example.storytell.init.HologramConfig;

import java.util.function.Supplier;

public class SyncHologramLockPacket {
    private final boolean locked;

    public SyncHologramLockPacket(boolean locked) {
        this.locked = locked;
    }

    public static void encode(SyncHologramLockPacket packet, FriendlyByteBuf buffer) {
        buffer.writeBoolean(packet.locked);
    }

    public static SyncHologramLockPacket decode(FriendlyByteBuf buffer) {
        return new SyncHologramLockPacket(buffer.readBoolean());
    }

    public static void handle(SyncHologramLockPacket packet, Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();
        context.enqueueWork(() -> {
            // Устанавливаем состояние блокировки в клиентском HologramConfig
            HologramConfig.setHologramLockedClient(packet.locked);
        });
        context.setPacketHandled(true);
    }
}