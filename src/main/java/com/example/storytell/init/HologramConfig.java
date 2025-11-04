// HologramConfig.java
package com.example.storytell.init;

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

    // Клиентские поля для хранения данных
    private static String clientHologramTexture = "storytell:textures/entity/default_hologram.png";
    private static String clientHologramText = "";
    private static String clientHologramAmbientSound = "storytell:hologram_ambient";
    private static boolean clientHologramLocked = false;
    private static boolean clientTextMode = false;

    // Внутренний класс для хранения конфигурационных данных
    private static class ConfigData {
        String hologramTexture = "storytell:textures/entity/default_hologram.png";
        String hologramText = "";
        boolean textMode = false;

        String radioSound = "storytell:radio_static";
        String hologramAmbientSound = "storytell:hologram_ambient";

        boolean repoSpawnEnabled = true;
        boolean energyRequired = true;
        boolean hologramLocked = false;

        List<String> bossList = new ArrayList<>(Arrays.asList(
                "fdbosses:chesed", "fdbosses:malkuth", "cataclysm:scylla", "cataclysm:maledictus",
                "cataclysm:ancient_remnant", "cataclysm:ender_guardian", "cataclysm:the_leviathan",
                "cataclysm:the_harbinger", "cataclysm:ignis", "cataclysm:netherite_monstrosity",
                "minecraft:ender_dragon", "minecraft:wither"
        ));

        Set<String> seenPlayers = new HashSet<>();
        Set<String> pendingPlayers = new HashSet<>();

        Map<String, Boolean> starVisibility = new HashMap<>();
        Map<String, StarSettings> starSettings = new HashMap<>();
    }

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
                configData = new ConfigData();
                initializeDefaults();
                saveConfig();
            }
        } catch (Exception e) {
            configData = new ConfigData();
            initializeDefaults();
        }

        clientHologramTexture = configData.hologramTexture;
        clientHologramText = configData.hologramText;
        clientHologramAmbientSound = configData.hologramAmbientSound;
        clientHologramLocked = configData.hologramLocked;
        clientTextMode = configData.textMode;

        initialized = true;
    }

    private static void initializeDefaults() {
        if (configData.starVisibility == null) configData.starVisibility = new HashMap<>();
        if (configData.starSettings == null) configData.starSettings = new HashMap<>();
        if (configData.pendingPlayers == null) configData.pendingPlayers = new HashSet<>();
        if (configData.hologramAmbientSound == null) configData.hologramAmbientSound = "storytell:hologram_ambient";
        if (configData.hologramText == null) configData.hologramText = "";
    }

    private static void saveConfig() {
        try {
            Path configDir = Paths.get("config", "storytell");
            if (!Files.exists(configDir)) Files.createDirectories(configDir);
            Path configFile = configDir.resolve(CONFIG_FILE_NAME);
            String json = GSON.toJson(configData);
            Files.writeString(configFile, json);
        } catch (Exception e) {}
    }

    // Геттеры и сеттеры
    public static boolean isTextMode() {
        if (!initialized) init();
        return configData.textMode;
    }

    public static void setTextMode(boolean textMode) {
        if (!initialized) init();
        configData.textMode = textMode;
        clientTextMode = textMode;
        saveConfig();
    }

    public static boolean isTextModeClient() { return clientTextMode; }
    public static void setTextModeClient(boolean textMode) { clientTextMode = textMode; }

    public static boolean isHologramLocked() {
        if (!initialized) init();
        return configData.hologramLocked;
    }

    public static void setHologramLocked(boolean locked) {
        if (!initialized) init();
        configData.hologramLocked = locked;
        clientHologramLocked = locked;
        saveConfig();
    }

    public static void setHologramLockedClient(boolean locked) { clientHologramLocked = locked; }
    public static boolean isHologramLockedClient() { return clientHologramLocked; }

    public static ResourceLocation getHologramTexture() {
        if (!initialized) init();
        return new ResourceLocation(configData.hologramTexture);
    }

    public static String getHologramText() {
        if (!initialized) init();
        return configData.hologramText;
    }

    public static boolean setHologramTexture(String texturePath) {
        if (!initialized) init();
        try {
            new ResourceLocation(texturePath);
            configData.hologramTexture = texturePath;
            clientHologramTexture = texturePath;
            configData.textMode = false;
            clientTextMode = false;
            saveConfig();
            return true;
        } catch (Exception e) { return false; }
    }

    public static boolean setHologramText(String text) {
        if (!initialized) init();
        try {
            configData.hologramText = text;
            clientHologramText = text;
            configData.textMode = true;
            clientTextMode = true;
            saveConfig();
            return true;
        } catch (Exception e) { return false; }
    }

    public static void setHologramTextureClient(String texturePath) {
        try {
            new ResourceLocation(texturePath);
            clientHologramTexture = texturePath;
            clientTextMode = false;
        } catch (Exception e) {}
    }

    public static void setHologramTextClient(String text) {
        clientHologramText = text;
        clientTextMode = true;
    }

    public static ResourceLocation getHologramTextureClient() { return new ResourceLocation(clientHologramTexture); }
    public static String getHologramTextClient() { return clientHologramText; }

    // Остальные методы остаются без изменений (только геттеры/сеттеры)
    public static List<String> getBossList() {
        if (!initialized) init();
        return new ArrayList<>(configData.bossList);
    }

    public static boolean isBoss(ResourceLocation entityId) {
        if (!initialized) init();
        return configData.bossList.contains(entityId.toString());
    }

    public static boolean addBoss(String bossId) {
        if (!initialized) init();
        try {
            new ResourceLocation(bossId);
            if (!configData.bossList.contains(bossId)) {
                configData.bossList.add(bossId);
                saveConfig();
                return true;
            }
        } catch (Exception e) {}
        return false;
    }

    public static boolean removeBoss(String bossId) {
        if (!initialized) init();
        if (configData.bossList.remove(bossId)) {
            saveConfig();
            return true;
        }
        return false;
    }

    public static void setBossList(List<String> newBossList) {
        if (!initialized) init();
        configData.bossList = new ArrayList<>(newBossList);
        saveConfig();
    }

    public static String getRadioSound() {
        if (!initialized) init();
        return configData.radioSound;
    }

    public static boolean setRadioSound(String sound) {
        if (!initialized) init();
        try {
            String cleanSound = sound.replace("sounds/", "").replace(".ogg", "");
            new ResourceLocation(cleanSound);
            configData.radioSound = cleanSound;
            saveConfig();
            return true;
        } catch (Exception e) { return false; }
    }

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

    public static void removeStar(String starName) {
        if (!initialized) init();
        configData.starVisibility.remove(starName);
        configData.starSettings.remove(starName);
        saveConfig();
    }

    public static boolean isRepoSpawnEnabled() {
        if (!initialized) init();
        return configData.repoSpawnEnabled;
    }

    public static void setRepoSpawnEnabled(boolean enabled) {
        if (!initialized) init();
        configData.repoSpawnEnabled = enabled;
        saveConfig();
    }

    public static boolean isEnergyRequired() {
        if (!initialized) init();
        return configData.energyRequired;
    }

    public static void setEnergyRequired(boolean required) {
        if (!initialized) init();
        configData.energyRequired = required;
        saveConfig();
    }

    public static String getHologramAmbientSoundLocation() {
        if (!initialized) init();
        return configData.hologramAmbientSound;
    }

    public static boolean setHologramAmbientSound(String soundLocation) {
        if (!initialized) init();
        try {
            new ResourceLocation(soundLocation);
            configData.hologramAmbientSound = soundLocation;
            clientHologramAmbientSound = soundLocation;
            saveConfig();
            return true;
        } catch (Exception e) { return false; }
    }

    public static void setHologramAmbientSoundClient(String soundLocation) {
        try {
            new ResourceLocation(soundLocation);
            clientHologramAmbientSound = soundLocation;
        } catch (Exception e) {}
    }

    public static SoundEvent getHologramAmbientSound() {
        try {
            String soundLocation = getHologramAmbientSoundLocation();
            SoundEvent sound = net.minecraftforge.registries.ForgeRegistries.SOUND_EVENTS.getValue(new ResourceLocation(soundLocation));
            return sound != null ? sound : ModSounds.HOLOGRAM_AMBIENT.get();
        } catch (Exception e) { return ModSounds.HOLOGRAM_AMBIENT.get(); }
    }

    public static SoundEvent getHologramAmbientSoundClient() {
        try {
            SoundEvent sound = net.minecraftforge.registries.ForgeRegistries.SOUND_EVENTS.getValue(new ResourceLocation(clientHologramAmbientSound));
            return sound != null ? sound : ModSounds.HOLOGRAM_AMBIENT.get();
        } catch (Exception e) { return ModSounds.HOLOGRAM_AMBIENT.get(); }
    }

    public static void resetToDefaults() {
        configData = new ConfigData();
        initializeDefaults();
        clientHologramTexture = configData.hologramTexture;
        clientHologramText = configData.hologramText;
        clientHologramAmbientSound = configData.hologramAmbientSound;
        clientHologramLocked = configData.hologramLocked;
        clientTextMode = configData.textMode;
        saveConfig();
    }

    public static String getConfigSummary() {
        if (!initialized) init();
        return String.format("HologramConfig: texture=%s, text=%s, textMode=%s, locked=%s, ambientSound=%s",
                configData.hologramTexture, configData.hologramText, configData.textMode,
                configData.hologramLocked, configData.hologramAmbientSound);
    }
}