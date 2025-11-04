package com.example.storytell.init.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;
import com.example.storytell.init.star.StarManager;

import java.util.function.Supplier;

public class StarSmoothMovePacket {
    private final String starName;
    private final float targetX, targetY, targetZ;
    private final int duration;
    private final String easingType;

    public StarSmoothMovePacket(String starName, float targetX, float targetY, float targetZ, int duration, String easingType) {
        this.starName = starName;
        this.targetX = targetX;
        this.targetY = targetY;
        this.targetZ = targetZ;
        this.duration = duration;
        this.easingType = easingType;
    }

    public static void encode(StarSmoothMovePacket packet, FriendlyByteBuf buffer) {
        buffer.writeUtf(packet.starName);
        buffer.writeFloat(packet.targetX);
        buffer.writeFloat(packet.targetY);
        buffer.writeFloat(packet.targetZ);
        buffer.writeInt(packet.duration);
        buffer.writeUtf(packet.easingType);
    }

    public static StarSmoothMovePacket decode(FriendlyByteBuf buffer) {
        return new StarSmoothMovePacket(
                buffer.readUtf(),
                buffer.readFloat(),
                buffer.readFloat(),
                buffer.readFloat(),
                buffer.readInt(),
                buffer.readUtf()
        );
    }

    public static void handle(StarSmoothMovePacket packet, Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();
        context.enqueueWork(() -> {
            StarManager.applySmoothMovement(packet.starName, packet.targetX, packet.targetY, packet.targetZ,
                    packet.duration, packet.easingType);
        });
        context.setPacketHandled(true);
    }
}