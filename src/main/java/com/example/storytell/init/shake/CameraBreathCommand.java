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

public class CameraBreathCommand {

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("camerabreath")
                .requires(source -> source.hasPermission(2))
                .then(Commands.argument("targets", EntityArgument.players())
                        .then(Commands.argument("power", FloatArgumentType.floatArg(0.1f, 5.0f))
                                .then(Commands.argument("duration", IntegerArgumentType.integer(1, 1000))
                                        .executes(context -> executeBreath(
                                                context,
                                                EntityArgument.getPlayers(context, "targets"),
                                                FloatArgumentType.getFloat(context, "power"),
                                                IntegerArgumentType.getInteger(context, "duration")
                                        ))
                                )
                        )
                ));
    }

    private static int executeBreath(CommandContext<CommandSourceStack> context,
                                     Collection<ServerPlayer> targets,
                                     float power,
                                     int duration) {

        for (ServerPlayer player : targets) {
            CameraBreathHandler.applyCameraBreath(player, power, duration);
        }

        context.getSource().sendSuccess(() ->
                        net.minecraft.network.chat.Component.literal("Применен эффект дыхания камеры к " + targets.size() + " игрокам"),
                true
        );

        return targets.size();
    }
}