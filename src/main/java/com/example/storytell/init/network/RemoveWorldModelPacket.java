// RemoveWorldModelPacket.java
package com.example.storytell.init.network;

import com.example.storytell.init.world.WorldModelManager;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public class RemoveWorldModelPacket {
    private final String id;

    public RemoveWorldModelPacket(String id) {
        this.id = id;
    }

    public void encode(FriendlyByteBuf buffer) {
        buffer.writeUtf(id);
    }

    public static RemoveWorldModelPacket decode(FriendlyByteBuf buffer) {
        String id = buffer.readUtf();
        return new RemoveWorldModelPacket(id);
    }

    public void handle(Supplier<NetworkEvent.Context> context) {
        context.get().enqueueWork(() -> {
            WorldModelManager.removeModel(id);
        });
        context.get().setPacketHandled(true);
    }
}