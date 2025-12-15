// altar/network/SummoningAltarSelectPacket.java
package com.example.storytell.init.altar.network;

import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public class SummoningAltarSelectPacket {
    private final BlockPos blockPos;
    private final String selectedEntity;

    public SummoningAltarSelectPacket(BlockPos blockPos, String selectedEntity) {
        this.blockPos = blockPos;
        this.selectedEntity = selectedEntity;
    }

    public static void encode(SummoningAltarSelectPacket msg, FriendlyByteBuf buf) {
        buf.writeBlockPos(msg.blockPos);
        buf.writeUtf(msg.selectedEntity);
    }

    public static SummoningAltarSelectPacket decode(FriendlyByteBuf buf) {
        return new SummoningAltarSelectPacket(buf.readBlockPos(), buf.readUtf());
    }

    public static void handle(SummoningAltarSelectPacket msg, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            var player = ctx.get().getSender();
            if (player != null) {
                var level = player.level();
                var blockEntity = level.getBlockEntity(msg.blockPos);
                if (blockEntity instanceof com.example.storytell.init.altar.SummoningAltarBlockEntity) {
                    ((com.example.storytell.init.altar.SummoningAltarBlockEntity) blockEntity)
                            .setSelectedEntity(msg.selectedEntity);
                }
            }
        });
        ctx.get().setPacketHandled(true);
    }
}