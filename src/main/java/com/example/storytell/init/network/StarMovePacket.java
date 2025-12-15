package com.example.storytell.init.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;
import com.example.storytell.init.star.StarManager;

import java.util.function.Supplier;

public class StarMovePacket {
    private final String starName;
    private final float offsetX, offsetY, offsetZ;
    private final int duration;

    public StarMovePacket(String starName, float offsetX, float offsetY, float offsetZ, int duration) {
        this.starName = starName;
        this.offsetX = offsetX;
        this.offsetY = offsetY;
        this.offsetZ = offsetZ;
        this.duration = duration;
    }

    public static void encode(StarMovePacket packet, FriendlyByteBuf buffer) {
        buffer.writeUtf(packet.starName);
        buffer.writeFloat(packet.offsetX);
        buffer.writeFloat(packet.offsetY);
        buffer.writeFloat(packet.offsetZ);
        buffer.writeInt(packet.duration);
    }

    public static StarMovePacket decode(FriendlyByteBuf buffer) {
        return new StarMovePacket(
                buffer.readUtf(),
                buffer.readFloat(),
                buffer.readFloat(),
                buffer.readFloat(),
                buffer.readInt()
        );
    }

    public static void handle(StarMovePacket packet, Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();
        context.enqueueWork(() -> {
            // Проверяем, что мы на клиенте
            if (context.getDirection().getReceptionSide().isClient()) {
                StarManager.applyStarOffset(packet.starName, packet.offsetX, packet.offsetY, packet.offsetZ, packet.duration);
            }
        });
        context.setPacketHandled(true);
    }
}