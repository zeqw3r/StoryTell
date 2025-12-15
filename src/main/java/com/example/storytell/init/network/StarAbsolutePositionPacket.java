// StarAbsolutePositionPacket.java
package com.example.storytell.init.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;
import com.example.storytell.init.star.StarManager;

import java.util.function.Supplier;

public class StarAbsolutePositionPacket {
    private final String starName;
    private final float rightAscension;
    private final float declination;
    private final float distance;
    private final int duration;

    public StarAbsolutePositionPacket(String starName, float rightAscension, float declination, float distance, int duration) {
        this.starName = starName;
        this.rightAscension = rightAscension;
        this.declination = declination;
        this.distance = distance;
        this.duration = duration;
    }

    public static void encode(StarAbsolutePositionPacket packet, FriendlyByteBuf buffer) {
        buffer.writeUtf(packet.starName);
        buffer.writeFloat(packet.rightAscension);
        buffer.writeFloat(packet.declination);
        buffer.writeFloat(packet.distance);
        buffer.writeInt(packet.duration);
    }

    public static StarAbsolutePositionPacket decode(FriendlyByteBuf buffer) {
        return new StarAbsolutePositionPacket(
                buffer.readUtf(),
                buffer.readFloat(),
                buffer.readFloat(),
                buffer.readFloat(),
                buffer.readInt()
        );
    }

    public static void handle(StarAbsolutePositionPacket packet, Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();
        context.enqueueWork(() -> {
            // Проверяем, что мы на клиенте
            if (context.getDirection().getReceptionSide().isClient()) {
                // Применяем абсолютное позиционирование на клиенте
                StarManager.applyAbsolutePosition(packet.starName, packet.rightAscension, packet.declination, packet.distance, packet.duration);
                System.out.println("Client: Received star position update for " + packet.starName +
                        " - RA: " + packet.rightAscension +
                        ", Dec: " + packet.declination +
                        ", Dist: " + packet.distance);
            }
        });
        context.setPacketHandled(true);
    }
}