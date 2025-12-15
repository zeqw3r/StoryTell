package com.example.storytell.init.shake;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.FloatArgumentType;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.context.CommandContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.server.level.ServerPlayer;

import java.util.Collection;

public class ScreenShakeCommand {

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("screenshake")
                .requires(source -> source.hasPermission(2))
                .then(Commands.argument("targets", EntityArgument.players())
                        .then(Commands.argument("power", FloatArgumentType.floatArg(0.1f, 10.0f))
                                .then(Commands.argument("duration", IntegerArgumentType.integer(1, 2000))
                                        .executes(context -> executeShake(
                                                context,
                                                EntityArgument.getPlayers(context, "targets"),
                                                FloatArgumentType.getFloat(context, "power"),
                                                IntegerArgumentType.getInteger(context, "duration")
                                        ))
                                )
                        )
                ));
    }

    private static int executeShake(CommandContext<CommandSourceStack> context,
                                    Collection<ServerPlayer> targets,
                                    float power,
                                    int duration) {

        for (ServerPlayer player : targets) {
            ScreenShakeHandler.applyScreenShake(player, power, duration);
        }

        return targets.size();
    }
}