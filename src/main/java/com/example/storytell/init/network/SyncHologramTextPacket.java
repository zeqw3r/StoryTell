package com.example.storytell.init.network;

import com.example.storytell.init.HologramConfig;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public class SyncHologramTextPacket {
    private final String text;

    public SyncHologramTextPacket(String text) {
        this.text = text;
    }

    // Используем этот конструктор для декодирования
    public SyncHologramTextPacket(FriendlyByteBuf buf) {
        this.text = buf.readUtf(32767);
    }

    // Метод для кодирования
    public static void encode(SyncHologramTextPacket packet, FriendlyByteBuf buf) {
        buf.writeUtf(packet.text);
    }

    // Метод для декодирования
    public static SyncHologramTextPacket decode(FriendlyByteBuf buf) {
        return new SyncHologramTextPacket(buf);
    }

    // Статический метод handle (исправлено)
    public static void handle(SyncHologramTextPacket packet, Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();
        context.enqueueWork(() -> {
            // Устанавливаем текст на клиенте
            HologramConfig.setHologramTextClient(packet.text);
        });
        context.setPacketHandled(true);
    }
}