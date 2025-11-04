package com.example.storytell.init.entity;

import com.example.storytell.init.HologramConfig;
import com.mojang.brigadier.Command;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.BoolArgumentType;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class RepoSpawnCommand {
    private static final Logger LOGGER = LogManager.getLogger();

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("repospawn")
                .requires(source -> source.hasPermission(2))
                .executes(context -> {
                    boolean currentState = HologramConfig.isRepoSpawnEnabled();
                    context.getSource().sendSuccess(() ->
                            Component.literal("REPO spawn is currently: " + currentState), false);
                    return Command.SINGLE_SUCCESS;
                })
                .then(Commands.literal("set")
                        .then(Commands.argument("enabled", BoolArgumentType.bool())
                                .executes(context -> {
                                    boolean newState = BoolArgumentType.getBool(context, "enabled");
                                    HologramConfig.setRepoSpawnEnabled(newState);
                                    context.getSource().sendSuccess(() ->
                                            Component.literal("REPO spawn set to: " + newState), true);
                                    LOGGER.debug("REPO spawn set to: {}", newState);
                                    return Command.SINGLE_SUCCESS;
                                }))));
    }
}