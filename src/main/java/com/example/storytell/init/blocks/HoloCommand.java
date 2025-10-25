// HoloCommand.java
package com.example.storytell.init.blocks;

import com.example.storytell.init.HologramConfig;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.BoolArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
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
                                    return count;
                                }
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
                        return count;
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
                                return count;
                            }
                            return 0;
                        }))
                // Команда для управления необходимостью энергии
                .then(Commands.literal("energy")
                        .then(Commands.argument("required", BoolArgumentType.bool())
                                .executes(ctx -> {
                                    boolean required = BoolArgumentType.getBool(ctx, "required");
                                    HologramConfig.setEnergyRequired(required);
                                    return 1;
                                })))
                // Команда для блокировки переключения голограмм
                .then(Commands.literal("lock")
                        .executes(ctx -> {
                            HologramConfig.setHologramLocked(true);
                            return 1;
                        }))
                // Команда для разблокировки переключения голограмм
                .then(Commands.literal("unlock")
                        .executes(ctx -> {
                            HologramConfig.setHologramLocked(false);
                            return 1;
                        }))
        );
    }
}