// HologramManager.java
package com.example.storytell.init.blocks;

import net.minecraft.resources.ResourceLocation;
import java.util.HashMap;
import java.util.Map;

public class HologramManager {

    // Карта сопоставления ключевых слов/фраз с текстурами
    private static final Map<String, ResourceLocation> TEXTURE_MAPPINGS = new HashMap<>();

    static {
        // Инициализация сопоставлений
        initializeMappings();
    }

    private static void initializeMappings() {
        // Русские команды
        TEXTURE_MAPPINGS.put("кто вы", new ResourceLocation("storytell", "textures/entity/whoareyou.png"));
        TEXTURE_MAPPINGS.put("спутник", new ResourceLocation("storytell", "textures/entity/satellite.png"));
        TEXTURE_MAPPINGS.put("обьекст в небе", new ResourceLocation("storytell", "textures/entity/satellite.png"));
        TEXTURE_MAPPINGS.put("монстр", new ResourceLocation("storytell", "textures/entity/monster.png"));
        TEXTURE_MAPPINGS.put("босс", new ResourceLocation("storytell", "textures/entity/monster.png"));
        TEXTURE_MAPPINGS.put("привет", new ResourceLocation("storytell", "textures/entity/hello.png"));
        TEXTURE_MAPPINGS.put("помогите", new ResourceLocation("storytell", "textures/entity/help.png"));
        TEXTURE_MAPPINGS.put("конец", new ResourceLocation("storytell", "textures/entity/end.png"));
        TEXTURE_MAPPINGS.put("колапс", new ResourceLocation("storytell", "textures/entity/end.png"));
        TEXTURE_MAPPINGS.put("апокал", new ResourceLocation("storytell", "textures/entity/end.png"));
    }

    /**
     * Обрабатывает команду и возвращает соответствующую текстуру
     */
    public static ResourceLocation processCommand(String command) {
        String lowerCommand = command.toLowerCase().trim();

        // 1. Точное совпадение
        if (TEXTURE_MAPPINGS.containsKey(lowerCommand)) {
            return TEXTURE_MAPPINGS.get(lowerCommand);
        }

        // 2. Частичное совпадение - ищем ключевые слова в команде
        for (Map.Entry<String, ResourceLocation> entry : TEXTURE_MAPPINGS.entrySet()) {
            if (lowerCommand.contains(entry.getKey())) {
                return entry.getValue();
            }
        }

        // 3. Если ничего не найдено - возвращаем null
        return null;
    }

    /**
     * Добавляет новое сопоставление (можно использовать для динамического добавления команд)
     */
    public static void addMapping(String command, ResourceLocation texture) {
        TEXTURE_MAPPINGS.put(command.toLowerCase(), texture);
    }

    /**
     * Возвращает все доступные сопоставления
     */
    public static Map<String, ResourceLocation> getAllMappings() {
        return new HashMap<>(TEXTURE_MAPPINGS);
    }
}