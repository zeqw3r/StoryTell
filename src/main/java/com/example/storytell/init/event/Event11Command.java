// Event11Command.java
package com.example.storytell.init.event;

import com.example.storytell.init.boss.BossSequenceManager;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.DoubleArgumentType;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;

public class Event11Command {

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("event11")
                .requires(source -> source.hasPermission(2))
                .then(Commands.argument("x", DoubleArgumentType.doubleArg())
                        .then(Commands.argument("y", DoubleArgumentType.doubleArg())
                                .then(Commands.argument("z", DoubleArgumentType.doubleArg())
                                        .executes(context -> {
                                            double x = DoubleArgumentType.getDouble(context, "x");
                                            double y = DoubleArgumentType.getDouble(context, "y");
                                            double z = DoubleArgumentType.getDouble(context, "z");
                                            ServerLevel level = context.getSource().getLevel();
                                            BlockPos pos = new BlockPos((int) x, (int) y, (int) z);

                                            return executeEvent11(context.getSource(), level, pos);
                                        })))));
    }

    private static int executeEvent11(CommandSourceStack source, ServerLevel level, BlockPos pos) {
        try {
            BossSequenceManager.startBossSequence(level, pos);
            source.sendSuccess(() -> Component.literal("Event11 started at " + pos.getX() + " " + pos.getY() + " " + pos.getZ()), true);
            return 1;
        } catch (Exception e) {
            source.sendFailure(Component.literal("Error starting event11: " + e.getMessage()));
            e.printStackTrace();
            return 0;
        }
    }
}
