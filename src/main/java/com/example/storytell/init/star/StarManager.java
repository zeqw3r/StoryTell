// StarManager.java
package com.example.storytell.init.star;

import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.ArrayList;
import java.util.List;

@Mod.EventBusSubscriber(modid = "storytell", bus = Mod.EventBusSubscriber.Bus.MOD)
public class StarManager {
    private static final List<CustomStar> stars = new ArrayList<>();
    private static boolean initialized = false;

    public static void init() {
        if (initialized) return;

        System.out.println("Initializing custom star system with JSON models...");

        // Создаем звезду, загружая настройки из конфига если есть
        CustomStar blueStar = new CustomStar(
                "blue_star",
                "star/blue_star",
                50.0f,        // Size
                0xFF87CEEB,  // Light blue color
                45.0f,       // Right Ascension
                30.0f,       // Declination
                150.0f,      // Distance
                0.5f,        // Rotation speed
                2.0f,        // Pulse speed
                0.3f,        // Pulse amount
                true         // Default visibility - visible
        );

        stars.add(blueStar);

        // Удалена звезда meteor

        initialized = true;
        System.out.println("Custom star system initialized with " + stars.size() + " stars");

        // Выводим информацию о загруженных настройках
        com.example.storytell.init.HologramConfig.StarSettings blueStarSettings =
                com.example.storytell.init.HologramConfig.getStarSettings("blue_star");
        if (blueStarSettings != null) {
            System.out.println("Loaded blue_star settings from config: visible=" + blueStarSettings.visible);
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

    public static void resetStarVisibility(String starName) {
        CustomStar star = getStarByName(starName);
        if (star != null) {
            star.resetToDefaultVisibility();
            System.out.println("Reset star " + starName + " visibility to default: " + star.getDefaultVisible());
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

    public static void updateStars(float partialTick) {
        if (!initialized) return;

        for (CustomStar star : stars) {
            // Future: individual star movement logic could go here
            // Modifiers are now handled in CustomStar.render()
        }
    }
}