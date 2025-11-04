package com.example.storytell.init.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;
import com.example.storytell.init.HologramConfig;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.Entity;
import com.example.storytell.init.blocks.HologramEntity;

import java.util.function.Supplier;

public class SyncHologramTexturePacket {
    private final String texture;

    public SyncHologramTexturePacket(String texture) {
        this.texture = texture;
    }

    public static void encode(SyncHologramTexturePacket packet, FriendlyByteBuf buffer) {
        buffer.writeUtf(packet.texture);
    }

    public static SyncHologramTexturePacket decode(FriendlyByteBuf buffer) {
        return new SyncHologramTexturePacket(buffer.readUtf());
    }

    public static void handle(SyncHologramTexturePacket packet, Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();
        context.enqueueWork(() -> {
            // Устанавливаем текстуру в клиентском HologramConfig
            HologramConfig.setHologramTextureClient(packet.texture);

            // Обновляем все существующие голограммы в клиентском мире
            if (Minecraft.getInstance().level != null) {
                for (Entity entity : Minecraft.getInstance().level.entitiesForRendering()) {
                    if (entity instanceof HologramEntity) {
                        ((HologramEntity) entity).setTextureFromConfig();
                    }
                }
            }
        });
        context.setPacketHandled(true);
    }
}