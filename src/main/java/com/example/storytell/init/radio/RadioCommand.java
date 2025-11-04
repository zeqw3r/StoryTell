// RadioCommand.java
package com.example.storytell.init.radio;

import com.example.storytell.init.HologramConfig;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.stream.Collectors;

public class RadioCommand {
    // Провайдер предложений для автодополнения звуков
    private static final SuggestionProvider<CommandSourceStack> SOUND_SUGGESTIONS =
            (context, builder) -> {
                // Получаем все зарегистрированные звуки и преобразуем их в строки
                var soundSuggestions = ForgeRegistries.SOUND_EVENTS.getKeys().stream()
                        .map(ResourceLocation::toString)
                        .collect(Collectors.toList());
                return SharedSuggestionProvider.suggest(soundSuggestions, builder);
            };

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("radio")
                .requires(source -> source.hasPermission(2))
                .then(Commands.literal("setsound")
                        .then(Commands.argument("sound", StringArgumentType.string())
                                .suggests(SOUND_SUGGESTIONS) // Добавляем автодополнение
                                .executes(context -> {
                                    String sound = StringArgumentType.getString(context, "sound");
                                    boolean success = HologramConfig.setRadioSound(sound);

                                    if (success) {
                                        String currentSound = HologramConfig.getRadioSound();
                                        context.getSource().sendSuccess(
                                                () -> Component.literal("Radio sound set to: " + currentSound), true);
                                    } else {
                                        context.getSource().sendFailure(
                                                Component.literal("Failed to set radio sound! Use format: modname:soundname"));
                                    }
                                    return 1;
                                })
                        )
                )
                .then(Commands.literal("getsound")
                        .executes(context -> {
                            String currentSound = HologramConfig.getRadioSound();
                            String soundType = "storytell:radio_static".equals(currentSound) ?
                                    " (continuous static)" : " (plays every minute)";
                            context.getSource().sendSuccess(
                                    () -> Component.literal("Current radio sound: " + currentSound + soundType), false);
                            return 1;
                        })
                )
                .then(Commands.literal("list")
                        .executes(context -> {
                            context.getSource().sendSuccess(
                                    () -> Component.literal("Available radio sounds:"), false);
                            context.getSource().sendSuccess(
                                    () -> Component.literal("- storytell:radio_static (continuous static noise)"), false);
                            context.getSource().sendSuccess(
                                    () -> Component.literal("- storytell:radio_music (plays every minute)"), false);
                            context.getSource().sendSuccess(
                                    () -> Component.literal("- minecraft:block.note_block.bit (plays every minute)"), false);
                            context.getSource().sendSuccess(
                                    () -> Component.literal("You can use any sound from any mod!"), false);
                            return 1;
                        })
                )
        );
    }
}