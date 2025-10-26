// HologramConfig.java
package com.example.storytell.init;

import com.example.storytell.StoryTell;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraftforge.registries.RegistryObject;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.HashSet;
import java.util.Set;
import java.util.HashMap;
import java.util.Map;

public class HologramConfig {
    private static final String CONFIG_FILE_NAME = "storytell_config.json";
    private static ConfigData configData = null;
    private static boolean initialized = false;
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    // Внутренний класс для хранения всех конфигурационных данных
    private static class ConfigData {
        // Основная текстура голограммы
        String hologramTexture = "storytell:textures/entity/default_hologram.png";
        String hologramText = "";

        // Звуки
        String radioSound = "storytell:radio_static";
        String hologramAmbientSound = "storytell:hologram_ambient"; // Добавлено поле для ambient sound

        // Настройки спавна
        boolean repoSpawnEnabled = true;

        // Настройки энергии для голограмм
        boolean energyRequired = true;

        // Блокировка переключения голограмм через сообщения
        boolean hologramLocked = false;

        // Список боссов
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

        // Система игроков
        Set<String> seenPlayers = new HashSet<>();
        Set<String> pendingPlayers = new HashSet<>();

        // Система звезд
        Map<String, Boolean> starVisibility = new HashMap<>();
        Map<String, StarSettings> starSettings = new HashMap<>();
    }

    // Класс для хранения настроек звезды
    public static class StarSettings {
        public boolean visible;
        public float baseX;
        public float baseY;
        public float baseZ;
        public float rightAscension;
        public float declination;
        public float distance;

        public StarSettings() {}

        public StarSettings(boolean visible, float baseX, float baseY, float baseZ,
                            float rightAscension, float declination, float distance) {
            this.visible = visible;
            this.baseX = baseX;
            this.baseY = baseY;
            this.baseZ = baseZ;
            this.rightAscension = rightAscension;
            this.declination = declination;
            this.distance = distance;
        }
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
                initializeDefaults();
            } else {
                // Создаем файл с настройками по умолчанию
                configData = new ConfigData();
                initializeDefaults();
                saveConfig();
            }
        } catch (Exception e) {
            configData = new ConfigData();
            initializeDefaults();
        }

        initialized = true;
    }

    private static void initializeDefaults() {
        // Инициализируем карты, если они null
        if (configData.starVisibility == null) {
            configData.starVisibility = new HashMap<>();
        }
        if (configData.starSettings == null) {
            configData.starSettings = new HashMap<>();
        }
        if (configData.pendingPlayers == null) {
            configData.pendingPlayers = new HashSet<>();
        }
        if (configData.hologramAmbientSound == null) {
            configData.hologramAmbientSound = "storytell:hologram_ambient";
        }
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
            // Без сообщений об ошибках
        }
    }

    // Методы для управления блокировкой голограмм
    public static boolean isHologramLocked() {
        if (!initialized) {
            init();
        }
        return configData.hologramLocked;
    }

    public static void setHologramLocked(boolean locked) {
        if (!initialized) {
            init();
        }
        configData.hologramLocked = locked;
        saveConfig();
    }

    // Методы для голограмм
    public static ResourceLocation getHologramTexture() {
        if (!initialized) {
            init();
        }
        return new ResourceLocation(configData.hologramTexture);
    }

    public static String getHologramText() {
        if (!initialized) {
            init();
        }
        return configData.hologramText;
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
            return false;
        }
    }

    public static boolean setHologramText(String text) {
        if (!initialized) {
            init();
        }
        try {
            configData.hologramText = text;

            saveConfig();
            return true;
        } catch (Exception e) {
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
            // Без сообщений об ошибках
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

    // Методы для ожидания подтверждения
    public static void addPendingPlayer(String playerName) {
        if (!initialized) init();
        configData.pendingPlayers.add(playerName.toLowerCase());
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

    // Методы для видимости звезд
    public static Boolean getStarVisibility(String starName) {
        if (!initialized) init();
        return configData.starVisibility.get(starName);
    }

    public static void setStarVisibility(String starName, boolean visible) {
        if (!initialized) init();
        configData.starVisibility.put(starName, visible);
        saveConfig();
    }

    public static void removeStarVisibility(String starName) {
        if (!initialized) init();
        configData.starVisibility.remove(starName);
        saveConfig();
    }

    public static Map<String, Boolean> getAllStarVisibility() {
        if (!initialized) init();
        return new HashMap<>(configData.starVisibility);
    }

    // Методы для настроек звезд
    public static StarSettings getStarSettings(String starName) {
        if (!initialized) init();
        return configData.starSettings.get(starName);
    }

    public static void setStarSettings(String starName, StarSettings settings) {
        if (!initialized) init();
        configData.starSettings.put(starName, settings);
        saveConfig();
    }

    public static void removeStarSettings(String starName) {
        if (!initialized) init();
        configData.starSettings.remove(starName);
        saveConfig();
    }

    public static Map<String, StarSettings> getAllStarSettings() {
        if (!initialized) init();
        return new HashMap<>(configData.starSettings);
    }

    // Универсальный метод для удаления всех данных о звезде
    public static void removeStar(String starName) {
        if (!initialized) init();
        configData.starVisibility.remove(starName);
        configData.starSettings.remove(starName);
        saveConfig();
    }

    // Методы для управления спавном REPO
    public static boolean isRepoSpawnEnabled() {
        if (!initialized) init();
        return configData.repoSpawnEnabled;
    }

    public static void setRepoSpawnEnabled(boolean enabled) {
        if (!initialized) init();
        configData.repoSpawnEnabled = enabled;
        saveConfig();
    }

    // Методы для управления энергией голограмм
    public static boolean isEnergyRequired() {
        if (!initialized) init();
        return configData.energyRequired;
    }

    public static void setEnergyRequired(boolean required) {
        if (!initialized) init();
        configData.energyRequired = required;
        saveConfig();
    }

    // Методы для управления ambient sound голограмм
    public static String getHologramAmbientSoundLocation() {
        if (!initialized) init();
        return configData.hologramAmbientSound;
    }

    public static boolean setHologramAmbientSound(String soundLocation) {
        if (!initialized) init();
        try {
            // Проверяем валидность пути звука
            new ResourceLocation(soundLocation);
            configData.hologramAmbientSound = soundLocation;
            saveConfig();
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    // Безопасный метод для получения SoundEvent
    public static SoundEvent getHologramAmbientSound() {
        try {
            String soundLocation = getHologramAmbientSoundLocation();
            ResourceLocation soundRes = new ResourceLocation(soundLocation);

            // Пытаемся найти звук в реестре
            SoundEvent sound = net.minecraftforge.registries.ForgeRegistries.SOUND_EVENTS.getValue(soundRes);
            if (sound != null) {
                return sound;
            }

            // Fallback на стандартный звук если указанный не найден
            return ModSounds.HOLOGRAM_AMBIENT.get();
        } catch (Exception e) {
            // Fallback на стандартный звук при ошибке
            return ModSounds.HOLOGRAM_AMBIENT.get();
        }
    }
}