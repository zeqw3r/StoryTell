// CutsceneStartPacket.java
package com.example.storytell.init.cutscene;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public class CutsceneStartPacket {
    private final String folderName;
    private final String targetSelector;

    public CutsceneStartPacket(String folderName, String targetSelector) {
        this.folderName = folderName;
        this.targetSelector = targetSelector;
    }

    public CutsceneStartPacket(FriendlyByteBuf buf) {
        this.folderName = buf.readUtf();
        this.targetSelector = buf.readUtf();
    }

    public void toBytes(FriendlyByteBuf buf) {
        buf.writeUtf(folderName);
        buf.writeUtf(targetSelector);
    }

    public boolean handle(Supplier<NetworkEvent.Context> supplier) {
        NetworkEvent.Context context = supplier.get();
        context.enqueueWork(() -> {
            System.out.println("CutsceneStartPacket: Starting cutscene " + folderName + " for " + targetSelector);

            // Запускаем катсцену на клиенте
            CutsceneManager.getInstance().startCutscene(folderName);
        });
        return true;
    }
}