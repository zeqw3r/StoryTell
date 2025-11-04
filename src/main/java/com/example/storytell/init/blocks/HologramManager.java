// HologramManager.java
package com.example.storytell.init.blocks;

import java.util.HashMap;
import java.util.Map;

public class HologramManager {

    // Карта сопоставления ключевых слов/фраз с текстом
    private static final Map<String, String> TEXT_MAPPINGS = new HashMap<>();

    static {
        // Инициализация сопоставлений
        initializeMappings();
    }
    //sethologram "Наконец мы можем вести полноценное общение. Эта линия защищена и вы можете задавать вопросы. Мы постараемся ответить"
    private static void initializeMappings() {
        // Русские команды
        TEXT_MAPPINGS.put("кто вы", "Мы остатки человечества. За нами ведется охота");
        TEXT_MAPPINGS.put("охота", "Они хотят уничтожить остатки живых");
        TEXT_MAPPINGS.put("они", "Мы не можем доверять вам полностью");
        TEXT_MAPPINGS.put("убежище", "Мы не можем доверять вам полностью");
        TEXT_MAPPINGS.put("бункер", "Мы не можем доверять вам полностью");
        TEXT_MAPPINGS.put("где вы", "Мы не можем доверять вам полностью");
        TEXT_MAPPINGS.put("спутник", "Спутник - устройства слежки за остатками живых существ. Будьте аккуратны, неизвестно когда они решат действовать");
        TEXT_MAPPINGS.put("обьект в небе", "Спутник - устройства слежки за остатками живых существ. Будьте аккуратны, неизвестно когда они решат действовать");
        TEXT_MAPPINGS.put("монстр", "Они оружие, созданное для уничтожения остатков человечества");
        TEXT_MAPPINGS.put("объект в небе", "Спутник - устройства слежки за остатками живых существ. Будьте аккуратны, неизвестно когда они решат действовать");
        TEXT_MAPPINGS.put("босс", "Они оружие, созданное для уничтожения остатков человечества");
        TEXT_MAPPINGS.put("привет", "Здравствуйте");
        TEXT_MAPPINGS.put("помогите", "Мы не можем еще сильнее помочь вам. Наши возможности ограничены");
        TEXT_MAPPINGS.put("конец", "Конец человечества произошел из-за взрыва реактора. он дестабилизировал ядро и вызвал коллапс");
        TEXT_MAPPINGS.put("колапс", "Конец человечества произошел из-за взрыва реактора. он дестабилизировал ядро и вызвал коллапс");
        TEXT_MAPPINGS.put("апокал", "Конец человечества произошел из-за взрыва реактора. он дестабилизировал ядро и вызвал коллапс");
    }

    /**
     * Обрабатывает команду и возвращает соответствующий текст
     */
    public static String processCommand(String command) {
        String lowerCommand = command.toLowerCase().trim();

        // 1. Точное совпадение
        if (TEXT_MAPPINGS.containsKey(lowerCommand)) {
            return TEXT_MAPPINGS.get(lowerCommand);
        }

        // 2. Частичное совпадение - ищем ключевые слова в команде
        for (Map.Entry<String, String> entry : TEXT_MAPPINGS.entrySet()) {
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
        TEXT_MAPPINGS.put(command.toLowerCase(), text);
    }

    /**
     * Возвращает все доступные сопоставления
     */
    public static Map<String, String> getAllMappings() {
        return new HashMap<>(TEXT_MAPPINGS);
    }
}