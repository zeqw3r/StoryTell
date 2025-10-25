// UpgradeTransmitterCommandPacket.java
package com.example.storytell.init.blocks;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public class UpgradeTransmitterCommandPacket {
    private final BlockPos blockPos;
    private final String command;

    public UpgradeTransmitterCommandPacket(BlockPos blockPos, String command) {
        this.blockPos = blockPos;
        this.command = command;
    }

    public static void encode(UpgradeTransmitterCommandPacket msg, FriendlyByteBuf buf) {
        buf.writeBlockPos(msg.blockPos);
        buf.writeUtf(msg.command, 100); // Максимальная длина 100 символов
    }

    public static UpgradeTransmitterCommandPacket decode(FriendlyByteBuf buf) {
        return new UpgradeTransmitterCommandPacket(buf.readBlockPos(), buf.readUtf(100));
    }

    public static void handle(UpgradeTransmitterCommandPacket msg, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            ServerPlayer player = ctx.get().getSender();
            if (player != null) {
                Level level = player.level();
                if (level instanceof ServerLevel serverLevel) {
                    if (serverLevel.getBlockEntity(msg.blockPos) instanceof UpgradeTransmitterBlockEntity blockEntity) {
                        blockEntity.processCommand(msg.command);
                    }
                }
            }
        });
        ctx.get().setPacketHandled(true);
    }
}