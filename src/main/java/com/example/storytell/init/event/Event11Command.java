package com.example.storytell.init.event;

import com.example.storytell.init.boss.BossSequenceManager;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.DoubleArgumentType;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.coordinates.BlockPosArgument;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;

public class Event11Command {

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("event11")
                .requires(source -> source.hasPermission(2))
                .executes(context -> {
                    ServerLevel level = context.getSource().getLevel();
                    BlockPos pos = context.getSource().getPlayerOrException().blockPosition();
                    return executeEvent11(context.getSource(), level, pos);
                })
                .then(Commands.argument("pos", BlockPosArgument.blockPos())
                        .executes(context -> {
                            ServerLevel level = context.getSource().getLevel();
                            BlockPos pos = BlockPosArgument.getLoadedBlockPos(context, "pos");
                            return executeEvent11(context.getSource(), level, pos);
                        })));
    }

    private static int executeEvent11(CommandSourceStack source, ServerLevel level, BlockPos pos) {
        try {
            BossSequenceManager.startBossSequence(level, pos);
            source.sendSuccess(() -> Component.literal("Босс цепочка запущена на координатах " + pos.getX() + " " + pos.getY() + " " + pos.getZ()), true);
            return 1;
        } catch (Exception e) {
            source.sendFailure(Component.literal("Ошибка запуска цепочки боссов: " + e.getMessage()));
            return 0;
        }
    }
}
