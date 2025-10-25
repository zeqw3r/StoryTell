// AddWorldModelPacket.java
package com.example.storytell.init.network;

import com.example.storytell.init.world.WorldModel;
import com.example.storytell.init.world.WorldModelManager;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public class AddWorldModelPacket {
    private final String id;
    private final ResourceLocation modelLocation;
    private final float x, y, z;
    private final float size;
    private final int color;

    public AddWorldModelPacket(String id, ResourceLocation modelLocation, float x, float y, float z, float size, int color) {
        this.id = id;
        this.modelLocation = modelLocation;
        this.x = x;
        this.y = y;
        this.z = z;
        this.size = size;
        this.color = color;
    }

    public void encode(FriendlyByteBuf buffer) {
        buffer.writeUtf(id);
        buffer.writeResourceLocation(modelLocation);
        buffer.writeFloat(x);
        buffer.writeFloat(y);
        buffer.writeFloat(z);
        buffer.writeFloat(size);
        buffer.writeInt(color);
    }

    public static AddWorldModelPacket decode(FriendlyByteBuf buffer) {
        String id = buffer.readUtf();
        ResourceLocation modelLocation = buffer.readResourceLocation();
        float x = buffer.readFloat();
        float y = buffer.readFloat();
        float z = buffer.readFloat();
        float size = buffer.readFloat();
        int color = buffer.readInt();
        return new AddWorldModelPacket(id, modelLocation, x, y, z, size, color);
    }

    public void handle(Supplier<NetworkEvent.Context> context) {
        context.get().enqueueWork(() -> {
            WorldModel model = new WorldModel(id, modelLocation, x, y, z, size, color);
            WorldModelManager.addModel(model);
        });
        context.get().setPacketHandled(true);
    }
}