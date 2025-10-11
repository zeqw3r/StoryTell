// StarMoveCommand.java
package com.example.storytell.init.star;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.FloatArgumentType;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;

public class StarMoveCommand {

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("star_move")
                .requires(source -> source.hasPermission(2))
                .then(Commands.argument("starName", StringArgumentType.string())
                        .then(Commands.argument("offsetX", FloatArgumentType.floatArg())
                                .then(Commands.argument("offsetY", FloatArgumentType.floatArg())
                                        .then(Commands.argument("offsetZ", FloatArgumentType.floatArg())
                                                .then(Commands.argument("duration", IntegerArgumentType.integer(1))
                                                        .executes(context -> executeStarMove(
                                                                context.getSource(),
                                                                StringArgumentType.getString(context, "starName"),
                                                                FloatArgumentType.getFloat(context, "offsetX"),
                                                                FloatArgumentType.getFloat(context, "offsetY"),
                                                                FloatArgumentType.getFloat(context, "offsetZ"),
                                                                IntegerArgumentType.getInteger(context, "duration")
                                                        ))
                                                )
                                        )
                                )
                        )
                ));
    }

    private static int executeStarMove(CommandSourceStack source, String starName, float offsetX, float offsetY, float offsetZ, int duration) {
        StarManager.applyStarOffset(starName, offsetX, offsetY, offsetZ, duration);
        source.sendSuccess(() -> Component.literal("Applied movement to star " + starName +
                " with offset (" + offsetX + ", " + offsetY + ", " + offsetZ + ") for " + duration + " ticks"), true);
        return 1;
    }
}