package com.example.storytell.init.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;
import com.example.storytell.init.star.StarManager;
import com.example.storytell.init.star.CustomStar;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Supplier;

public class SyncAllStarsPacket {
    private final Map<String, Boolean> starVisibilityMap;

    public SyncAllStarsPacket(Map<String, Boolean> starVisibilityMap) {
        this.starVisibilityMap = starVisibilityMap;
    }

    public static void encode(SyncAllStarsPacket packet, FriendlyByteBuf buffer) {
        buffer.writeInt(packet.starVisibilityMap.size());
        for (Map.Entry<String, Boolean> entry : packet.starVisibilityMap.entrySet()) {
            buffer.writeUtf(entry.getKey());
            buffer.writeBoolean(entry.getValue());
        }
    }

    public static SyncAllStarsPacket decode(FriendlyByteBuf buffer) {
        int size = buffer.readInt();
        Map<String, Boolean> visibilityMap = new HashMap<>();
        for (int i = 0; i < size; i++) {
            String starName = buffer.readUtf();
            boolean visible = buffer.readBoolean();
            visibilityMap.put(starName, visible);
        }
        return new SyncAllStarsPacket(visibilityMap);
    }

    public static void handle(SyncAllStarsPacket packet, Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();
        context.enqueueWork(() -> {
            // Синхронизируем видимость всех звезд на клиенте
            for (Map.Entry<String, Boolean> entry : packet.starVisibilityMap.entrySet()) {
                CustomStar star = StarManager.getStarByName(entry.getKey());
                if (star != null) {
                    star.setVisible(entry.getValue());
                }
            }
        });
        context.setPacketHandled(true);
    }
}