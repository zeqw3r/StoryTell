// HoloCommand.java
package com.example.storytell.init.blocks;

import com.example.storytell.init.HologramConfig;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.BoolArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraftforge.network.PacketDistributor;
import com.example.storytell.init.network.*;

public class HoloCommand {

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        // Команда /sethologram <text> - устанавливает текст со стандартной текстурой
        dispatcher.register(Commands.literal("sethologram")
                .requires(source -> source.hasPermission(2))
                .then(Commands.argument("text", StringArgumentType.string())
                        .executes(ctx -> {
                            String text = StringArgumentType.getString(ctx, "text");

                            // Обновляем конфиг
                            HologramConfig.setHologramTexture("storytell:textures/entity/default_hologram.png");
                            HologramConfig.setHologramText(text);

                            // Синхронизируем с клиентами
                            syncConfigWithClients();

                            ctx.getSource().sendSuccess(() ->
                                    Component.literal("Hologram updated with text: " + text), true);
                            return 1;
                        })));

        // Основная команда /hologram
        dispatcher.register(Commands.literal("hologram")
                .requires(source -> source.hasPermission(2))
                .executes(ctx -> {
                    if (ctx.getSource().getLevel() instanceof ServerLevel) {
                        ServerLevel serverLevel = (ServerLevel) ctx.getSource().getLevel();
                        int count = 0;
                        for (Object e : serverLevel.getAllEntities()) {
                            if (e instanceof HologramEntity) count++;
                        }
                        ctx.getSource().sendSuccess(() ->
                                Component.literal("Active holograms: "), true);
                        return count;
                    }
                    return 0;
                })
                .then(Commands.literal("removeAll")
                        .executes(ctx -> {
                            if (ctx.getSource().getLevel() instanceof ServerLevel) {
                                ServerLevel serverLevel = (ServerLevel) ctx.getSource().getLevel();
                                int count = 0;
                                for (Object e : serverLevel.getAllEntities()) {
                                    if (e instanceof HologramEntity) {
                                        ((HologramEntity) e).discard();
                                        count++;
                                    }
                                }
                                ctx.getSource().sendSuccess(() ->
                                        Component.literal("Removed " + " holograms"), true);
                                return count;
                            }
                            return 0;
                        }))
                .then(Commands.literal("energy")
                        .then(Commands.argument("required", BoolArgumentType.bool())
                                .executes(ctx -> {
                                    boolean required = BoolArgumentType.getBool(ctx, "required");
                                    HologramConfig.setEnergyRequired(required);
                                    ctx.getSource().sendSuccess(() ->
                                            Component.literal("Energy requirement set to: " + required), true);
                                    return 1;
                                })))
                .then(Commands.literal("lock")
                        .executes(ctx -> {
                            HologramConfig.setHologramLocked(true);
                            syncConfigWithClients();
                            ctx.getSource().sendSuccess(() ->
                                    Component.literal("Holograms locked"), true);
                            return 1;
                        }))
                .then(Commands.literal("unlock")
                        .executes(ctx -> {
                            HologramConfig.setHologramLocked(false);
                            syncConfigWithClients();
                            ctx.getSource().sendSuccess(() ->
                                    Component.literal("Holograms unlocked"), true);
                            return 1;
                        }))
        );
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
}