// Event7Command.java
package com.example.storytell.init.event;

import com.example.storytell.init.HologramConfig;
import com.example.storytell.init.ModSounds;
import com.mojang.brigadier.CommandDispatcher;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraftforge.network.PacketDistributor;
import com.example.storytell.init.network.*;

public class Event7Command {

    private static final String EVENT7_TEXT = "Система восстановлена. Приносим извинения за неудобства.";

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("event7")
                .requires(source -> source.hasPermission(2))
                .executes(context -> executeEvent7(context.getSource())));
    }

    private static int executeEvent7(CommandSourceStack source) {
        if (source.getLevel() instanceof ServerLevel) {
            ServerLevel serverLevel = (ServerLevel) source.getLevel();

            // Обновляем конфиг
            HologramConfig.setHologramTexture("storytell:textures/entity/default_hologram.png");
            HologramConfig.setHologramText(EVENT7_TEXT);
            HologramConfig.setHologramAmbientSound("storytell:hologram_ambient");
            HologramConfig.setHologramLocked(false);

            // Синхронизируем с клиентами
            syncConfigWithClients();

            // Воспроизводим звук через 1 секунду
            scheduleDelayedTask(serverLevel.getServer(), 20, () -> {
                serverLevel.playSound(null, 0, 64, 0,
                        ModSounds.HOLOGRAM_APPEAR.get(), SoundSource.BLOCKS, 0.5F, 1.0F);
            });

            source.sendSuccess(() ->
                    Component.literal("Event7 executed: Holograms unlocked, standard texture with message"), true);
            return 1;
        } else {
            source.sendFailure(Component.literal("Must be executed on server"));
            return 0;
        }
    }

    private static void syncConfigWithClients() {
        SyncHologramTexturePacket texturePacket = new SyncHologramTexturePacket(HologramConfig.getHologramTexture().toString());
        SyncHologramTextPacket textPacket = new SyncHologramTextPacket(HologramConfig.getHologramText());
        SyncHologramAmbientSoundPacket soundPacket = new SyncHologramAmbientSoundPacket(HologramConfig.getHologramAmbientSoundLocation());
        SyncHologramLockPacket lockPacket = new SyncHologramLockPacket(HologramConfig.isHologramLocked());

        NetworkHandler.INSTANCE.send(PacketDistributor.ALL.noArg(), texturePacket);
        NetworkHandler.INSTANCE.send(PacketDistributor.ALL.noArg(), textPacket);
        NetworkHandler.INSTANCE.send(PacketDistributor.ALL.noArg(), soundPacket);
        NetworkHandler.INSTANCE.send(PacketDistributor.ALL.noArg(), lockPacket);
    }

    private static void scheduleDelayedTask(net.minecraft.server.MinecraftServer server, int delayTicks, Runnable task) {
        new Thread(() -> {
            try {
                Thread.sleep(delayTicks * 50L);
                server.execute(task);
            } catch (InterruptedException e) {
                // Игнорируем прерывание
            }
        }).start();
    }
}