// HologramConfig.java
package com.example.storytell.init;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import net.minecraft.resources.ResourceLocation;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.HashSet;
import java.util.Set;

public class HologramConfig {
    private static final String CONFIG_FILE_NAME = "storytell_config.json";
    private static ConfigData configData = null;
    private static boolean initialized = false;
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    // Внутренний класс для хранения всех конфигурационных данных
    private static class ConfigData {
        String hologramTexture = "minecraft:textures/block/stone.png";
        String radioSound = "storytell:radio_static";
        List<String> bossList = new ArrayList<>(Arrays.asList(
                "fdbosses:chesed",
                "fdbosses:malkuth",
                "cataclysm:scylla",
                "cataclysm:maledictus",
                "cataclysm:ancient_remnant",
                "cataclysm:ender_guardian",
                "cataclysm:the_leviathan",
                "cataclysm:the_harbinger",
                "cataclysm:ignis",
                "cataclysm:netherite_monstrosity"
        ));
        Set<String> seenPlayers = new HashSet<>();
        Set<String> pendingPlayers = new HashSet<>(); // Новое поле для игроков, ожидающих подтверждения
    }

    public static void init() {
        if (initialized) return;

        try {
            Path configDir = Paths.get("config", "storytell");
            if (!Files.exists(configDir)) {
                Files.createDirectories(configDir);
            }

            Path configFile = configDir.resolve(CONFIG_FILE_NAME);
            if (Files.exists(configFile)) {
                String json = Files.readString(configFile);
                configData = GSON.fromJson(json, ConfigData.class);
                System.out.println("Loaded StoryTell config with " + configData.bossList.size() + " bosses");
                System.out.println("Loaded " + configData.seenPlayers.size() + " seen players");
            } else {
                // Создаем файл с настройками по умолчанию
                configData = new ConfigData();
                saveConfig();
                System.out.println("Created default StoryTell config with " + configData.bossList.size() + " bosses");
            }
        } catch (Exception e) {
            System.err.println("Failed to load StoryTell config: " + e.getMessage());
            configData = new ConfigData();
        }

        initialized = true;
    }

    private static void saveConfig() {
        try {
            Path configDir = Paths.get("config", "storytell");
            if (!Files.exists(configDir)) {
                Files.createDirectories(configDir);
            }

            Path configFile = configDir.resolve(CONFIG_FILE_NAME);
            String json = GSON.toJson(configData);
            Files.writeString(configFile, json);
        } catch (Exception e) {
            System.err.println("Failed to save StoryTell config: " + e.getMessage());
        }
    }

    // Методы для голограмм
    public static ResourceLocation getHologramTexture() {
        if (!initialized) {
            init();
        }
        return new ResourceLocation(configData.hologramTexture);
    }

    public static boolean setHologramTexture(String texturePath) {
        if (!initialized) {
            init();
        }
        try {
            // Проверяем валидность пути текстуры
            new ResourceLocation(texturePath);
            configData.hologramTexture = texturePath;
            saveConfig();
            return true;
        } catch (Exception e) {
            System.err.println("Failed to save hologram texture: " + e.getMessage());
            return false;
        }
    }

    // Методы для боссов
    public static List<String> getBossList() {
        if (!initialized) {
            init();
        }
        return new ArrayList<>(configData.bossList);
    }

    public static boolean isBoss(ResourceLocation entityId) {
        if (!initialized) {
            init();
        }
        String entityString = entityId.toString();
        return configData.bossList.contains(entityString);
    }

    public static boolean addBoss(String bossId) {
        if (!initialized) {
            init();
        }
        try {
            // Проверяем валидность ResourceLocation
            new ResourceLocation(bossId);
            if (!configData.bossList.contains(bossId)) {
                configData.bossList.add(bossId);
                saveConfig();
                return true;
            }
        } catch (Exception e) {
            System.err.println("Invalid boss ID: " + bossId);
        }
        return false;
    }

    public static boolean removeBoss(String bossId) {
        if (!initialized) {
            init();
        }
        if (configData.bossList.remove(bossId)) {
            saveConfig();
            return true;
        }
        return false;
    }

    public static void setBossList(List<String> newBossList) {
        if (!initialized) {
            init();
        }
        configData.bossList = new ArrayList<>(newBossList);
        saveConfig();
    }

    // Методы для радио
    public static String getRadioSound() {
        if (!initialized) init();
        return configData.radioSound;
    }

    public static boolean setRadioSound(String sound) {
        if (!initialized) init();
        try {
            // Убираем лишние части из пути звука
            String cleanSound = sound;
            if (cleanSound.contains(":")) {
                cleanSound = cleanSound.replace("sounds/", "").replace(".ogg", "");
            }

            // Проверяем валидность пути звука
            new ResourceLocation(cleanSound);
            configData.radioSound = cleanSound;
            saveConfig();
            return true;
        } catch (Exception e) {
            System.err.println("Failed to save radio sound: " + e.getMessage());
            return false;
        }
    }

    // Методы для отслеживания игроков
    public static boolean hasPlayerSeenCutscene(String playerName) {
        if (!initialized) init();
        return configData.seenPlayers.contains(playerName.toLowerCase());
    }

    public static void markPlayerAsSeen(String playerName) {
        if (!initialized) init();
        configData.seenPlayers.add(playerName.toLowerCase());
        saveConfig();
    }

    public static Set<String> getSeenPlayers() {
        if (!initialized) init();
        return new HashSet<>(configData.seenPlayers);
    }

    // Новые методы для ожидания подтверждения
    public static void addPendingPlayer(String playerName) {
        if (!initialized) init();
        configData.pendingPlayers.add(playerName.toLowerCase());
        // Не сохраняем в конфиг, так как это временное состояние
    }

    public static boolean isPlayerPending(String playerName) {
        if (!initialized) init();
        return configData.pendingPlayers.contains(playerName.toLowerCase());
    }

    public static void removePendingPlayer(String playerName) {
        if (!initialized) init();
        configData.pendingPlayers.remove(playerName.toLowerCase());
    }

    public static Set<String> getPendingPlayers() {
        if (!initialized) init();
        return new HashSet<>(configData.pendingPlayers);
    }
}