package com.example.storytell.init.cutscene;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.function.Supplier;

public class CutsceneStartPacket {
    private static final Logger LOGGER = LogManager.getLogger();

    private final String folderName;
    private final String targetSelector;

    public CutsceneStartPacket(String folderName, String targetSelector) {
        this.folderName = folderName;
        this.targetSelector = targetSelector;
    }

    public CutsceneStartPacket(FriendlyByteBuf buf) {
        this.folderName = buf.readUtf(50); // Ограничение длины для безопасности
        this.targetSelector = buf.readUtf(50);
    }

    public void toBytes(FriendlyByteBuf buf) {
        buf.writeUtf(folderName, 50);
        buf.writeUtf(targetSelector, 50);
    }

    public boolean handle(Supplier<NetworkEvent.Context> supplier) {
        NetworkEvent.Context context = supplier.get();
        context.enqueueWork(() -> {
            // Прямой запуск без лишних проверок
            CutsceneManager.getInstance().startCutscene(folderName);
        });
        return true;
    }
}