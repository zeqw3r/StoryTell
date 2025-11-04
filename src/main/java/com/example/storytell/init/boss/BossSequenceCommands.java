// BossSequenceCommands.java
package com.example.storytell.init.boss;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;

import java.util.List;

public class BossSequenceCommands {

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        // Команда /bosssequencelist - показать список боссов в последовательности
        dispatcher.register(Commands.literal("bosssequencelist")
                .requires(source -> source.hasPermission(2))
                .executes(ctx -> {
                    List<String> bosses = BossSequenceManager.getBossSequence();
                    ctx.getSource().sendSuccess(() ->
                            Component.literal("Boss sequence (" + bosses.size() + "): " + String.join(" → ", bosses)), false);
                    return bosses.size();
                }));

        // Команда /addbosssequence <boss_id>
        dispatcher.register(Commands.literal("addbosssequence")
                .requires(source -> source.hasPermission(2))
                .then(Commands.argument("boss_id", StringArgumentType.string())
                        .executes(ctx -> {
                            String bossId = StringArgumentType.getString(ctx, "boss_id");
                            BossSequenceManager.addBossToSequence(bossId);
                            ctx.getSource().sendSuccess(() ->
                                    Component.literal("Added boss to sequence: " + bossId), true);
                            return 1;
                        })));

        // Команда /removebosssequence <boss_id>
        dispatcher.register(Commands.literal("removebosssequence")
                .requires(source -> source.hasPermission(2))
                .then(Commands.argument("boss_id", StringArgumentType.string())
                        .executes(ctx -> {
                            String bossId = StringArgumentType.getString(ctx, "boss_id");
                            BossSequenceManager.removeBossFromSequence(bossId);
                            ctx.getSource().sendSuccess(() ->
                                    Component.literal("Removed boss from sequence: " + bossId), true);
                            return 1;
                        })));

        // Команда /clearbosssequence - очистить всю последовательность
        dispatcher.register(Commands.literal("clearbosssequence")
                .requires(source -> source.hasPermission(2))
                .executes(ctx -> {
                    BossSequenceManager.clearBossSequence();
                    ctx.getSource().sendSuccess(() ->
                            Component.literal("Cleared boss sequence"), true);
                    return 1;
                }));
    }
}