// MoveWorldModelPacket.java
package com.example.storytell.init.network;

import com.example.storytell.init.world.WorldModel;
import com.example.storytell.init.world.WorldModelManager;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public class MoveWorldModelPacket {
    private final String id;
    private final float targetX, targetY, targetZ;
    private final int moveDuration;
    private final String easingType;

    public MoveWorldModelPacket(String id, float targetX, float targetY, float targetZ, int moveDuration, String easingType) {
        this.id = id;
        this.targetX = targetX;
        this.targetY = targetY;
        this.targetZ = targetZ;
        this.moveDuration = moveDuration;
        this.easingType = easingType;
    }

    public void encode(FriendlyByteBuf buffer) {
        buffer.writeUtf(id);
        buffer.writeFloat(targetX);
        buffer.writeFloat(targetY);
        buffer.writeFloat(targetZ);
        buffer.writeInt(moveDuration);
        buffer.writeUtf(easingType);
    }

    public static MoveWorldModelPacket decode(FriendlyByteBuf buffer) {
        String id = buffer.readUtf();
        float targetX = buffer.readFloat();
        float targetY = buffer.readFloat();
        float targetZ = buffer.readFloat();
        int moveDuration = buffer.readInt();
        String easingType = buffer.readUtf();
        return new MoveWorldModelPacket(id, targetX, targetY, targetZ, moveDuration, easingType);
    }

    public void handle(Supplier<NetworkEvent.Context> context) {
        context.get().enqueueWork(() -> {
            WorldModel model = WorldModelManager.getModel(id);
            if (model != null) {
                model.setTargetPosition(targetX, targetY, targetZ, moveDuration, easingType);
            }
        });
        context.get().setPacketHandled(true);
    }
}
///particle minecraft:campfire_signal_smoke ~ ~ ~ 2 2 2 0.6 1000 force