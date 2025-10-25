// TabletImageSuggestions.java
package com.example.storytell.init.tablet;

import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import net.minecraft.client.Minecraft;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

public class TabletImageSuggestions {
    private static final String TABLET_IMAGE_PATH = "textures/gui/tablet";
    private static final SuggestionProvider<CommandSourceStack> PROVIDER = TabletImageSuggestions::getSuggestions;

    public static SuggestionProvider<CommandSourceStack> getProvider() {
        return PROVIDER;
    }

    private static CompletableFuture<Suggestions> getSuggestions(CommandContext<CommandSourceStack> context, SuggestionsBuilder builder) {
        List<String> suggestions = getAvailableTabletImages();
        return SharedSuggestionProvider.suggest(suggestions, builder);
    }

    private static List<String> getAvailableTabletImages() {
        List<String> imagePaths = new ArrayList<>();

        try {
            ResourceManager resourceManager = Minecraft.getInstance().getResourceManager();

            // Получаем все ресурсы в папке tablet
            resourceManager.listResources(TABLET_IMAGE_PATH, location -> {
                // Фильтруем только PNG файлы
                return location.getPath().endsWith(".png");
            }).forEach((resourceLocation, resource) -> {
                // Преобразуем ResourceLocation в строку для команды
                String path = resourceLocation.toString();
                imagePaths.add(path);
            });

        } catch (Exception e) {
            // В случае ошибки добавляем хотя бы стандартную картинку
            imagePaths.add("storytell:textures/gui/tablet/default.png");
        }

        // Если ничего не найдено, добавляем стандартную картинку
        if (imagePaths.isEmpty()) {
            imagePaths.add("storytell:textures/gui/tablet/default.png");
        }

        return imagePaths;
    }
}