// BossCommands.java
package com.example.storytell.init.boss;

import com.example.storytell.init.HologramConfig;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;

import java.util.List;

public class BossCommands {

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        // Команда /bosslist - показать список боссов
        dispatcher.register(Commands.literal("bosslist")
                .requires(source -> source.hasPermission(2))
                .executes(ctx -> {
                    List<String> bosses = HologramConfig.getBossList();
                    ctx.getSource().sendSuccess(() ->
                            Component.literal("Registered bosses (" + bosses.size() + "): " + String.join(", ", bosses)), false);
                    return bosses.size();
                }));

        // Команда /addboss <boss_id>
        dispatcher.register(Commands.literal("addboss")
                .requires(source -> source.hasPermission(2))
                .then(Commands.argument("boss_id", StringArgumentType.string())
                        .executes(ctx -> {
                            String bossId = StringArgumentType.getString(ctx, "boss_id");
                            boolean success = HologramConfig.addBoss(bossId);

                            if (success) {
                                ctx.getSource().sendSuccess(() ->
                                        Component.literal("Added boss: " + bossId), true);
                            } else {
                                ctx.getSource().sendFailure(Component.literal("Failed to add boss. Maybe it already exists or ID is invalid."));
                            }
                            return success ? 1 : 0;
                        })));

        // Команда /removeboss <boss_id>
        dispatcher.register(Commands.literal("removeboss")
                .requires(source -> source.hasPermission(2))
                .then(Commands.argument("boss_id", StringArgumentType.string())
                        .executes(ctx -> {
                            String bossId = StringArgumentType.getString(ctx, "boss_id");
                            boolean success = HologramConfig.removeBoss(bossId);

                            if (success) {
                                ctx.getSource().sendSuccess(() ->
                                        Component.literal("Removed boss: " + bossId), true);
                            } else {
                                ctx.getSource().sendFailure(Component.literal("Boss not found: " + bossId));
                            }
                            return success ? 1 : 0;
                        })));
    }
}