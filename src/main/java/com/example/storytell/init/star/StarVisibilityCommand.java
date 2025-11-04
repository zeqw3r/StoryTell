// StarVisibilityCommand.java
package com.example.storytell.init.star;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.BoolArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraftforge.network.PacketDistributor;
import com.example.storytell.init.network.NetworkHandler;
import com.example.storytell.init.network.StarVisibilityPacket;

public class StarVisibilityCommand {

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("star_visibility")
                .requires(source -> source.hasPermission(2))
                .then(Commands.argument("starName", StringArgumentType.string())
                        .executes(context -> {
                            // Toggle visibility without argument
                            return toggleStarVisibility(
                                    context.getSource(),
                                    StringArgumentType.getString(context, "starName")
                            );
                        })
                        .then(Commands.argument("visible", BoolArgumentType.bool())
                                .executes(context -> setStarVisibility(
                                        context.getSource(),
                                        StringArgumentType.getString(context, "starName"),
                                        BoolArgumentType.getBool(context, "visible")
                                ))
                        )
                )
                .then(Commands.literal("reset")
                        .then(Commands.argument("starName", StringArgumentType.string())
                                .executes(context -> resetStarVisibility(
                                        context.getSource(),
                                        StringArgumentType.getString(context, "starName")
                                ))
                        )
                ));
    }

    private static int toggleStarVisibility(CommandSourceStack source, String starName) {
        CustomStar star = StarManager.getStarByName(starName);
        if (star != null) {
            star.toggleVisibility();
            boolean isNowVisible = star.isVisible();

            // Отправляем пакет всем клиентам
            StarManager.sendVisibilityUpdate(starName, isNowVisible, false, true);

            source.sendSuccess(() -> Component.literal("Toggled visibility for star " + starName +
                    ". Now: " + (isNowVisible ? "visible" : "hidden")), true);
            return 1;
        } else {
            source.sendFailure(Component.literal("Star not found: " + starName));
            return 0;
        }
    }

    private static int setStarVisibility(CommandSourceStack source, String starName, boolean visible) {
        CustomStar star = StarManager.getStarByName(starName);
        if (star != null) {
            star.setVisible(visible);

            // Отправляем пакет всем клиентам
            StarManager.sendVisibilityUpdate(starName, visible, false, false);

            source.sendSuccess(() -> Component.literal("Set visibility for star " + starName +
                    " to: " + (visible ? "visible" : "hidden")), true);
            return 1;
        } else {
            source.sendFailure(Component.literal("Star not found: " + starName));
            return 0;
        }
    }

    private static int resetStarVisibility(CommandSourceStack source, String starName) {
        CustomStar star = StarManager.getStarByName(starName);
        if (star != null) {
            star.resetToDefaultVisibility();
            boolean defaultVisibility = star.getDefaultVisible();

            // Отправляем пакет всем клиентам
            StarManager.sendVisibilityUpdate(starName, defaultVisibility, true, false);

            source.sendSuccess(() -> Component.literal("Reset visibility for star " + starName +
                    " to default: " + (defaultVisibility ? "visible" : "hidden")), true);
            return 1;
        } else {
            source.sendFailure(Component.literal("Star not found: " + starName));
            return 0;
        }
    }
}