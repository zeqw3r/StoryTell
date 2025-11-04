package com.example.storytell.init.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;
import com.example.storytell.init.star.StarManager;

import java.util.function.Supplier;

public class StarVisibilityPacket {
    private final String starName;
    private final boolean visible;
    private final boolean isReset;
    private final boolean isToggle;

    public StarVisibilityPacket(String starName, boolean visible, boolean isReset, boolean isToggle) {
        this.starName = starName;
        this.visible = visible;
        this.isReset = isReset;
        this.isToggle = isToggle;
    }

    public static void encode(StarVisibilityPacket packet, FriendlyByteBuf buffer) {
        buffer.writeUtf(packet.starName);
        buffer.writeBoolean(packet.visible);
        buffer.writeBoolean(packet.isReset);
        buffer.writeBoolean(packet.isToggle);
    }

    public static StarVisibilityPacket decode(FriendlyByteBuf buffer) {
        return new StarVisibilityPacket(
                buffer.readUtf(),
                buffer.readBoolean(),
                buffer.readBoolean(),
                buffer.readBoolean()
        );
    }

    public static void handle(StarVisibilityPacket packet, Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();
        context.enqueueWork(() -> {
            // Применяем изменения на клиенте
            if (packet.isReset) {
                StarManager.resetStarVisibility(packet.starName);
            } else if (packet.isToggle) {
                StarManager.toggleStarVisibility(packet.starName);
            } else {
                StarManager.setStarVisibility(packet.starName, packet.visible);
            }
        });
        context.setPacketHandled(true);
    }
}