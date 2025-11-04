// SyncHologramTextPacket.java
package com.example.storytell.init.network;

import com.example.storytell.init.HologramConfig;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public class SyncHologramTextPacket {
    private final String text;

    public SyncHologramTextPacket(String text) {
        this.text = text;
    }

    public SyncHologramTextPacket(FriendlyByteBuf buf) {
        this.text = buf.readUtf(32767);
    }

    public void toBytes(FriendlyByteBuf buf) {
        buf.writeUtf(text);
    }

    public boolean handle(Supplier<NetworkEvent.Context> supplier) {
        NetworkEvent.Context context = supplier.get();
        context.enqueueWork(() -> {
            // Устанавливаем текст на клиенте
            HologramConfig.setHologramTextClient(text);
        });
        return true;
    }
}