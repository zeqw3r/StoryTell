package com.example.storytell.init.shake;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public class CameraBreathPacket {
    private final float power;
    private final int duration;
    private final boolean activate;

    public CameraBreathPacket(float power, int duration, boolean activate) {
        this.power = power;
        this.duration = duration;
        this.activate = activate;
    }

    public CameraBreathPacket(FriendlyByteBuf buf) {
        this.power = buf.readFloat();
        this.duration = buf.readInt();
        this.activate = buf.readBoolean();
    }

    public void encode(FriendlyByteBuf buf) {
        buf.writeFloat(power);
        buf.writeInt(duration);
        buf.writeBoolean(activate);
    }

    public boolean handle(Supplier<NetworkEvent.Context> supplier) {
        NetworkEvent.Context context = supplier.get();
        context.enqueueWork(() -> {
            // Обработка на клиенте
            DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> {
                ClientCameraBreathHandler.handleCameraBreath(power, duration, activate);
            });
        });
        return true;
    }

    public float getPower() { return power; }
    public int getDuration() { return duration; }
    public boolean isActivate() { return activate; }
}