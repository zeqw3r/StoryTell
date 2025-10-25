// SetRotationSpeedPacket.java - новый пакет для синхронизации вращения
package com.example.storytell.init.network;

import com.example.storytell.init.world.WorldModel;
import com.example.storytell.init.world.WorldModelManager;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public class SetRotationSpeedPacket {
    private final String id;
    private final float speedX, speedY, speedZ;

    public SetRotationSpeedPacket(String id, float speedX, float speedY, float speedZ) {
        this.id = id;
        this.speedX = speedX;
        this.speedY = speedY;
        this.speedZ = speedZ;
    }

    public void encode(FriendlyByteBuf buffer) {
        buffer.writeUtf(id);
        buffer.writeFloat(speedX);
        buffer.writeFloat(speedY);
        buffer.writeFloat(speedZ);
    }

    public static SetRotationSpeedPacket decode(FriendlyByteBuf buffer) {
        String id = buffer.readUtf();
        float speedX = buffer.readFloat();
        float speedY = buffer.readFloat();
        float speedZ = buffer.readFloat();
        return new SetRotationSpeedPacket(id, speedX, speedY, speedZ);
    }

    public void handle(Supplier<NetworkEvent.Context> context) {
        context.get().enqueueWork(() -> {
            WorldModel model = WorldModelManager.getModel(id);
            if (model != null) {
                model.setRotationSpeeds(speedX, speedY, speedZ);
            }
        });
        context.get().setPacketHandled(true);
    }
}