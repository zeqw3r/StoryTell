// TabletCommand.java
package com.example.storytell.init.tablet;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

public class TabletCommand {
    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("tablet")
                .requires(source -> source.hasPermission(2)) // Только для админов
                .then(Commands.literal("setimage")
                        .then(Commands.argument("image_path", StringArgumentType.string())
                                .suggests(TabletImageSuggestions.getProvider()) // Автодополнение
                                .executes(context -> setImage(
                                        context,
                                        StringArgumentType.getString(context, "image_path")
                                )))));
    }

    private static int setImage(CommandContext<CommandSourceStack> context, String imagePath) {
        try {
            Player player = context.getSource().getPlayerOrException();
            ItemStack mainHandItem = player.getMainHandItem();

            if (mainHandItem.getItem() instanceof NewsTabletItem) {
                NewsTabletItem.setImagePath(mainHandItem, imagePath);
                context.getSource().sendSuccess(() ->
                        Component.literal("Установлена новая картинка для планшета: " + imagePath), true);
                return 1;
            } else {
                context.getSource().sendFailure(Component.literal("Вы должны держать новостной планшет в основной руке"));
                return 0;
            }
        } catch (CommandSyntaxException e) {
            context.getSource().sendFailure(Component.literal("Эта команда может быть выполнена только игроком"));
            return 0;
        }
    }
}