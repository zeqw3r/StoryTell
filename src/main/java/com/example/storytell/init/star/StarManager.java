// StarManager.java
package com.example.storytell.init.star;

import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.network.PacketDistributor;
import com.example.storytell.init.network.NetworkHandler;
import com.example.storytell.init.network.StarVisibilityPacket;
import com.example.storytell.init.network.StarMovePacket;
import com.example.storytell.init.network.StarSmoothMovePacket;
import com.example.storytell.init.network.StarModifierPacket;
import com.example.storytell.init.network.SyncAllStarsPacket;
import com.example.storytell.init.network.StarColorPacket;
import com.example.storytell.init.network.StarColorAnimationPacket;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Mod.EventBusSubscriber(modid = "storytell", bus = Mod.EventBusSubscriber.Bus.MOD)
public class StarManager {
    private static final List<CustomStar> stars = new ArrayList<>();
    private static boolean initialized = false;
    private static long lastUpdateTime = 0;

    public static void init() {
        if (initialized) return;

        System.out.println("Initializing custom star system with JSON models...");

        // Создаем первую звезду - blue_star
        CustomStar blueStar = new CustomStar(
                "blue_star",
                "star/blue_star",
                50.0f,        // Size
                0xFFFF0000,  // Light blue color
                45.0f,       // Right Ascension
                30.0f,       // Declination
                150.0f,      // Distance
                0.5f,        // Rotation speed
                0.0f,        // Pulse speed (циклов в секунду)
                0.3f,        // Pulse amount (30% изменения размера)
                false         // Default visibility - HIDDEN
        );

        // Создаем вторую звезду - core с такими же параметрами
        CustomStar coreStar = new CustomStar(
                "core",
                "star/core",  // Такая же модель
                200.0f,             // Size
                0xFF87CEEB,       // Light blue color
                60.0f,            // Right Ascension (немного другое положение)
                -90.0f,            // Declination
                80.0f,           // Distance
                3f,             // Rotation speed
                0.0f,           // Pulse speed
                0.4f,           // Pulse amount
                true             // Default visibility - VISIBLE
        );

        // Создаем третью звезду - core_2 с моделью core
        CustomStar core2Star = new CustomStar(
                "core_2",
                "star/core_2",       // Модель core
                200.0f,             // Size
                0xFFFF0000,       // Light blue color
                60.0f,            // Right Ascension (немного другое положение)
                -90.0f,            // Declination
                80.0f,           // Distance
                3f,             // Rotation speed
                0.1f,           // Pulse speed
                0.4f,           // Pulse amount
                true             // Default visibility - VISIBLE
        );

        stars.add(blueStar);
        stars.add(coreStar);
        stars.add(core2Star);

        lastUpdateTime = System.currentTimeMillis();
        initialized = true;
        System.out.println("Custom star system initialized with " + stars.size() + " stars");

        // Выводим информацию о загруженных настройках
        com.example.storytell.init.HologramConfig.StarSettings blueStarSettings =
                com.example.storytell.init.HologramConfig.getStarSettings("blue_star");
        if (blueStarSettings != null) {
            System.out.println("Loaded blue_star settings from config: visible=" + blueStarSettings.visible);
        }

        com.example.storytell.init.HologramConfig.StarSettings coreStarSettings =
                com.example.storytell.init.HologramConfig.getStarSettings("core");
        if (coreStarSettings != null) {
            System.out.println("Loaded core settings from config: visible=" + coreStarSettings.visible);
        }

        com.example.storytell.init.HologramConfig.StarSettings core2StarSettings =
                com.example.storytell.init.HologramConfig.getStarSettings("core_2");
        if (core2StarSettings != null) {
            System.out.println("Loaded core_2 settings from config: visible=" + core2StarSettings.visible);
        }
    }

    @SubscribeEvent
    public static void onCommonSetup(FMLCommonSetupEvent event) {
        event.enqueueWork(StarManager::init);
    }

    public static List<CustomStar> getStars() {
        return new ArrayList<>(stars);
    }

    public static boolean isInitialized() {
        return initialized;
    }

    public static CustomStar getStarByName(String name) {
        for (CustomStar star : stars) {
            if (star.getName().equals(name)) {
                return star;
            }
        }
        return null;
    }

    // Оптимизированный метод для обновления анимаций только тех звезд, которые нуждаются в обновлении
    public static void updateStars(float partialTick) {
        if (!initialized) return;

        long currentTime = System.currentTimeMillis();
        float deltaTime = (currentTime - lastUpdateTime) / 1000.0f; // Convert to seconds
        lastUpdateTime = currentTime;

        for (CustomStar star : stars) {
            // Обновляем только видимые звезды или звезды с активными модификаторами
            if (star.isVisible() || !StarModifierManager.getModifiers(star.getName()).isEmpty()) {
                star.update(deltaTime, partialTick);
            }
        }
    }

    public static void applyStarOffset(String starName, float offsetX, float offsetY, float offsetZ, int duration) {
        CustomStar star = getStarByName(starName);
        if (star != null) {
            Runnable onExpire = () -> {
                System.out.println("Star " + starName + " returned to base position");
                star.resetToBasePosition();
            };
            star.applyPositionModifier("offset", offsetX, offsetY, offsetZ, duration, onExpire);
            System.out.println("Applied star offset: " + starName + " moved by (" + offsetX + ", " + offsetY + ", " + offsetZ + ") for " + duration + " ticks");
        } else {
            System.err.println("Star not found: " + starName);
        }
    }

    // Новый метод для плавного перемещения с выбором типа easing
    public static void applySmoothMovement(String starName, float targetX, float targetY, float targetZ,
                                           int duration, String easingType) {
        CustomStar star = getStarByName(starName);
        if (star != null) {
            // Получаем текущую позицию звезды
            float currentX = star.getX();
            float currentY = star.getY();
            float currentZ = star.getZ();

            Runnable onExpire = () -> {
                System.out.println("Smooth movement completed for star " + starName);
                // Звезда остается в конечной позиции после завершения анимации
            };

            star.applySmoothMovement(currentX, currentY, currentZ, targetX, targetY, targetZ,
                    duration, onExpire, easingType);
            System.out.println("Started smooth movement for star " + starName +
                    " from (" + currentX + ", " + currentY + ", " + currentZ + ") " +
                    "to (" + targetX + ", " + targetY + ", " + targetZ + ") " +
                    "over " + duration + " ticks with easing: " + easingType);
        } else {
            System.err.println("Star not found: " + starName);
        }
    }

    public static void moveStarAbovePlayer(String starName, ServerPlayer player, float height, int duration) {
        CustomStar star = getStarByName(starName);
        if (star != null) {
            // Calculate position directly above player
            float playerY = (float) player.getY();
            float newY = playerY + height;

            Runnable onExpire = () -> {
                System.out.println("Star " + starName + " returned to base position");
                star.resetToBasePosition();
            };

            // Use player_position type to set absolute Y position above player
            star.applyPositionModifier("player_position", 0, newY, 0, duration, onExpire);
            System.out.println("Moved star " + starName + " above player " + player.getScoreboardName() +
                    " at height " + height + " for " + duration + " ticks");
        } else {
            System.err.println("Star not found: " + starName);
        }
    }

    public static void setStarVisibility(String starName, boolean visible) {
        CustomStar star = getStarByName(starName);
        if (star != null) {
            star.setVisible(visible);
            System.out.println("Set star " + starName + " visibility to: " + visible);
        } else {
            System.err.println("Star not found: " + starName);
        }
    }

    // Новый метод для переключения видимости
    public static void toggleStarVisibility(String starName) {
        CustomStar star = getStarByName(starName);
        if (star != null) {
            star.toggleVisibility();
            System.out.println("Toggled star " + starName + " visibility to: " + star.isVisible());
        } else {
            System.err.println("Star not found: " + starName);
        }
    }

    public static void resetStarVisibility(String starName) {
        CustomStar star = getStarByName(starName);
        if (star != null) {
            star.resetToDefaultVisibility();
            System.out.println("Reset star " + starName + " visibility to default: " + star.getDefaultVisible());
        } else {
            System.err.println("Star not found: " + starName);
        }
    }

    // Метод для изменения цвета звезды
    public static void setStarColor(String starName, int color) {
        CustomStar star = getStarByName(starName);
        if (star != null) {
            star.setColor(color);
            System.out.println("Set star " + starName + " color to: " + Integer.toHexString(color));
        } else {
            System.err.println("Star not found: " + starName);
        }
    }

    // Новый метод для плавного изменения цвета звезды
    public static void startStarColorAnimation(String starName, int targetColor, int durationTicks) {
        CustomStar star = getStarByName(starName);
        if (star != null) {
            star.startColorAnimation(targetColor, durationTicks);
            System.out.println("Started color animation for star " + starName +
                    " to color " + Integer.toHexString(targetColor) + " over " + durationTicks + " ticks");
        } else {
            System.err.println("Star not found: " + starName);
        }
    }

    // Метод для удаления звезды
    public static void removeStar(String starName) {
        CustomStar star = getStarByName(starName);
        if (star != null) {
            stars.remove(star);
            star.removeFromConfig();
            System.out.println("Removed star: " + starName);
        } else {
            System.err.println("Star not found: " + starName);
        }
    }

    // Новый метод для применения модификаторов
    public static void applyStarModifier(String starName, String modifierType, float x, float y, float z, int duration) {
        CustomStar star = getStarByName(starName);
        if (star != null) {
            Runnable onExpire = () -> {
                System.out.println("Modifier expired for star " + starName);
                if ("offset".equals(modifierType)) {
                    star.resetToBasePosition();
                }
            };
            star.applyPositionModifier(modifierType, x, y, z, duration, onExpire);
            System.out.println("Applied modifier to star " + starName + ": " + modifierType +
                    " (" + x + ", " + y + ", " + z + ") for " + duration + " ticks");
        } else {
            System.err.println("Star not found: " + starName);
        }
    }

    // Метод для синхронизации всех звезд с новым игроком
    public static void syncStarsToPlayer(ServerPlayer player) {
        Map<String, Boolean> visibilityMap = new HashMap<>();
        for (CustomStar star : stars) {
            visibilityMap.put(star.getName(), star.isVisible());
        }
        SyncAllStarsPacket packet = new SyncAllStarsPacket(visibilityMap);
        NetworkHandler.INSTANCE.send(PacketDistributor.PLAYER.with(() -> player), packet);
    }

    // Методы для отправки сетевых пакетов
    public static void sendVisibilityUpdate(String starName, boolean visible, boolean isReset, boolean isToggle) {
        StarVisibilityPacket packet = new StarVisibilityPacket(starName, visible, isReset, isToggle);
        NetworkHandler.INSTANCE.send(PacketDistributor.ALL.noArg(), packet);
    }

    public static void sendMoveUpdate(String starName, float offsetX, float offsetY, float offsetZ, int duration) {
        StarMovePacket packet = new StarMovePacket(starName, offsetX, offsetY, offsetZ, duration);
        NetworkHandler.INSTANCE.send(PacketDistributor.ALL.noArg(), packet);
    }

    public static void sendSmoothMoveUpdate(String starName, float targetX, float targetY, float targetZ, int duration, String easingType) {
        StarSmoothMovePacket packet = new StarSmoothMovePacket(starName, targetX, targetY, targetZ, duration, easingType);
        NetworkHandler.INSTANCE.send(PacketDistributor.ALL.noArg(), packet);
    }

    public static void sendModifierUpdate(String starName, String modifierType, float x, float y, float z, int duration) {
        StarModifierPacket packet = new StarModifierPacket(starName, modifierType, x, y, z, duration);
        NetworkHandler.INSTANCE.send(PacketDistributor.ALL.noArg(), packet);
    }

    // Новый метод для отправки обновления цвета
    public static void sendColorUpdate(String starName, int color) {
        StarColorPacket packet = new StarColorPacket(starName, color);
        NetworkHandler.INSTANCE.send(PacketDistributor.ALL.noArg(), packet);
    }

    // Новый метод для отправки анимации цвета
    public static void sendColorAnimationUpdate(String starName, int targetColor, int durationTicks) {
        StarColorAnimationPacket packet = new StarColorAnimationPacket(starName, targetColor, durationTicks);
        NetworkHandler.INSTANCE.send(PacketDistributor.ALL.noArg(), packet);
    }
}