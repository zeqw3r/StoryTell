// RedSkyPacket.java
package com.example.storytell.init.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public class RedSkyPacket {
    private final boolean activate;
    private final int duration;

    public RedSkyPacket(boolean activate, int duration) {
        this.activate = activate;
        this.duration = duration;
    }

    public RedSkyPacket(FriendlyByteBuf buf) {
        this.activate = buf.readBoolean();
        this.duration = buf.readInt();
    }

    public void encode(FriendlyByteBuf buf) {
        buf.writeBoolean(activate);
        buf.writeInt(duration);
    }

    public boolean handle(Supplier<NetworkEvent.Context> context) {
        context.get().enqueueWork(() -> {
            // Клиентский код будет здесь
            ClientEventHandlers.handleRedSky(activate, duration);
        });
        return true;
    }
}