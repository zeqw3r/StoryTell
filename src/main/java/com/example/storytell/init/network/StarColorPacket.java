// StarColorPacket.java
package com.example.storytell.init.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;
import com.example.storytell.init.star.StarManager;

import java.util.function.Supplier;

public class StarColorPacket {
    private final String starName;
    private final int color;

    public StarColorPacket(String starName, int color) {
        this.starName = starName;
        this.color = color;
    }

    public StarColorPacket(FriendlyByteBuf buf) {
        this.starName = buf.readUtf();
        this.color = buf.readInt();
    }

    public void encode(FriendlyByteBuf buf) {
        buf.writeUtf(starName);
        buf.writeInt(color);
    }

    public static StarColorPacket decode(FriendlyByteBuf buf) {
        return new StarColorPacket(buf);
    }

    public boolean handle(Supplier<NetworkEvent.Context> supplier) {
        NetworkEvent.Context context = supplier.get();
        context.enqueueWork(() -> {
            // Обновляем цвет звезды на клиенте
            var star = StarManager.getStarByName(starName);
            if (star != null) {
                star.setColor(color);
            }
        });
        return true;
    }
}