// StarColorAnimationPacket.java
package com.example.storytell.init.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;
import com.example.storytell.init.star.StarManager;

import java.util.function.Supplier;

public class StarColorAnimationPacket {
    private final String starName;
    private final int targetColor;
    private final int durationTicks;

    public StarColorAnimationPacket(String starName, int targetColor, int durationTicks) {
        this.starName = starName;
        this.targetColor = targetColor;
        this.durationTicks = durationTicks;
    }

    public StarColorAnimationPacket(FriendlyByteBuf buf) {
        this.starName = buf.readUtf();
        this.targetColor = buf.readInt();
        this.durationTicks = buf.readInt();
    }

    public void encode(FriendlyByteBuf buf) {
        buf.writeUtf(starName);
        buf.writeInt(targetColor);
        buf.writeInt(durationTicks);
    }

    public static StarColorAnimationPacket decode(FriendlyByteBuf buf) {
        return new StarColorAnimationPacket(buf);
    }

    public boolean handle(Supplier<NetworkEvent.Context> supplier) {
        NetworkEvent.Context context = supplier.get();
        context.enqueueWork(() -> {
            // Запускаем анимацию цвета звезды на клиенте
            var star = StarManager.getStarByName(starName);
            if (star != null) {
                star.startColorAnimation(targetColor, durationTicks);
            }
        });
        return true;
    }
}