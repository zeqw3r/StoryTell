// HoloCommand.java
package com.example.storytell.init.blocks;

import com.example.storytell.init.HologramConfig;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.Entity;
import net.minecraft.server.level.ServerLevel;

public class HoloCommand {



    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        // Команда /sethologram <texture>
        dispatcher.register(Commands.literal("sethologram")
                .requires(source -> source.hasPermission(2))
                .then(Commands.argument("texture", StringArgumentType.string())
                        .executes(ctx -> {
                            String texture = StringArgumentType.getString(ctx, "texture");

                            // Сохраняем текстуру в конфиг
                            boolean success = HologramConfig.setHologramTexture(texture);

                            if (success) {
                                // Обновляем текстуру во всех голограммах
                                if (ctx.getSource().getLevel() instanceof ServerLevel) {
                                    ServerLevel serverLevel = (ServerLevel) ctx.getSource().getLevel();
                                    int count = 0;
                                    for (Entity e : serverLevel.getAllEntities()) {
                                        if (e instanceof HologramEntity) {
                                            ((HologramEntity) e).setTextureFromConfig();
                                            count++;
                                        }
                                    }
                                    final int finalCount = count;
                                    ctx.getSource().sendSuccess(() ->
                                            Component.literal("Updated texture for " + finalCount + " holograms to: " + texture), true);
                                    return finalCount;
                                }
                            } else {
                                ctx.getSource().sendFailure(Component.literal("Failed to set hologram texture. Invalid texture path: " + texture));
                            }
                            return 0;
                        })));

        // Команда /hologram
        dispatcher.register(Commands.literal("hologram")
                .requires(source -> source.hasPermission(2))
                .executes(ctx -> {
                    if (ctx.getSource().getLevel() instanceof ServerLevel) {
                        ServerLevel serverLevel = (ServerLevel) ctx.getSource().getLevel();
                        int count = 0;
                        for (Entity e : serverLevel.getAllEntities()) {
                            if (e instanceof HologramEntity) {
                                count++;
                            }
                        }
                        final int finalCount = count;
                        ctx.getSource().sendSuccess(() ->
                                Component.literal("Found " + finalCount + " holograms in this dimension"), false);
                        return finalCount;
                    }
                    return 0;
                })
                .then(Commands.literal("removeAll")
                        .executes(ctx -> {
                            if (ctx.getSource().getLevel() instanceof ServerLevel) {
                                ServerLevel serverLevel = (ServerLevel) ctx.getSource().getLevel();
                                int count = 0;
                                for (Entity e : serverLevel.getAllEntities()) {
                                    if (e instanceof HologramEntity) {
                                        e.discard();
                                        count++;
                                    }
                                }
                                final int finalCount = count;
                                ctx.getSource().sendSuccess(() ->
                                        Component.literal("Removed " + finalCount + " holograms"), true);
                                return finalCount;
                            }
                            return 0;
                        }))
        );
    }
}