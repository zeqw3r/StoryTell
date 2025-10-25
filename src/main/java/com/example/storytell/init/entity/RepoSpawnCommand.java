// RepoSpawnCommand.java
package com.example.storytell.init.entity;

import com.example.storytell.init.HologramConfig;
import com.mojang.brigadier.Command;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.BoolArgumentType;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;

public class RepoSpawnCommand {
    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("repospawn")
                .requires(source -> source.hasPermission(2)) // Только для операторов (permission level 2)
                .executes(context -> {
                    boolean currentState = HologramConfig.isRepoSpawnEnabled();
                    return Command.SINGLE_SUCCESS;
                })
                .then(Commands.literal("set")
                        .then(Commands.argument("enabled", BoolArgumentType.bool())
                                .executes(context -> {
                                    boolean newState = BoolArgumentType.getBool(context, "enabled");
                                    HologramConfig.setRepoSpawnEnabled(newState);
                                    return Command.SINGLE_SUCCESS;
                                }))));
    }
}