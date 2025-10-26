// HologramManager.java
package com.example.storytell.init.blocks;

import net.minecraft.resources.ResourceLocation;
import java.util.HashMap;
import java.util.Map;

public class HologramManager {

    // Карта сопоставления ключевых слов/фраз с текстурами
    private static final Map<String, String> TEXTURE_MAPPINGS = new HashMap<>();

    static {
        // Инициализация сопоставлений
        initializeMappings();
    }

    private static void initializeMappings() {
        // Русские команды
        TEXTURE_MAPPINGS.put("кто вы", "Зубенко Михаил Петрович");
        TEXTURE_MAPPINGS.put("спутник", "Спутник - устройства слежки за остатками живых существ. Будьте аккуратны, неизвестно когда они решат действовать");
        TEXTURE_MAPPINGS.put("обьекст в небе", "Спутник - устройства слежки за остатками живых существ. Будьте аккуратны, неизвестно когда они решат действовать");
        TEXTURE_MAPPINGS.put("монстр", "Они оружие, созданное для уничтожения остатков человечества");
        TEXTURE_MAPPINGS.put("босс", "Они оружие, созданное для уничтожения остатков человечества");
        TEXTURE_MAPPINGS.put("привет", "Здравствуйте");
        TEXTURE_MAPPINGS.put("помогите", "Мы не можем еще сильнее помочь вам. Наши возможности ограничены");
        TEXTURE_MAPPINGS.put("конец", "Конец человечества произошел из-за взрыва реактора. он дестабилизировал ядро и вызвал коллапс");
        TEXTURE_MAPPINGS.put("колапс", "Конец человечества произошел из-за взрыва реактора. он дестабилизировал ядро и вызвал коллапс");
        TEXTURE_MAPPINGS.put("апокал", "Конец человечества произошел из-за взрыва реактора. он дестабилизировал ядро и вызвал коллапс");
    }

    /**
     * Обрабатывает команду и возвращает соответствующую текстуру
     */
    public static String processCommand(String command) {
        String lowerCommand = command.toLowerCase().trim();

        // 1. Точное совпадение
        if (TEXTURE_MAPPINGS.containsKey(lowerCommand)) {
            return TEXTURE_MAPPINGS.get(lowerCommand);
        }

        // 2. Частичное совпадение - ищем ключевые слова в команде
        for (Map.Entry<String, String> entry : TEXTURE_MAPPINGS.entrySet()) {
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
    public static void addMapping(String command, String text) {
        TEXTURE_MAPPINGS.put(command.toLowerCase(), text);
    }

    /**
     * Возвращает все доступные сопоставления
     */
    public static Map<String, String> getAllMappings() {
        return new HashMap<>(TEXTURE_MAPPINGS);
    }
}