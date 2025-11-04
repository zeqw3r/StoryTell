package com.example.storytell.init.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;
import com.example.storytell.init.star.StarManager;

import java.util.function.Supplier;

public class StarModifierPacket {
    private final String starName;
    private final String modifierType;
    private final float x, y, z;
    private final int duration;

    public StarModifierPacket(String starName, String modifierType, float x, float y, float z, int duration) {
        this.starName = starName;
        this.modifierType = modifierType;
        this.x = x;
        this.y = y;
        this.z = z;
        this.duration = duration;
    }

    public static void encode(StarModifierPacket packet, FriendlyByteBuf buffer) {
        buffer.writeUtf(packet.starName);
        buffer.writeUtf(packet.modifierType);
        buffer.writeFloat(packet.x);
        buffer.writeFloat(packet.y);
        buffer.writeFloat(packet.z);
        buffer.writeInt(packet.duration);
    }

    public static StarModifierPacket decode(FriendlyByteBuf buffer) {
        return new StarModifierPacket(
                buffer.readUtf(),
                buffer.readUtf(),
                buffer.readFloat(),
                buffer.readFloat(),
                buffer.readFloat(),
                buffer.readInt()
        );
    }

    public static void handle(StarModifierPacket packet, Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();
        context.enqueueWork(() -> {
            StarManager.applyStarModifier(packet.starName, packet.modifierType, packet.x, packet.y, packet.z, packet.duration);
        });
        context.setPacketHandled(true);
    }
}