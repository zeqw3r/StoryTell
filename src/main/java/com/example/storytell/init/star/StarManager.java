// StarManager.java
package com.example.storytell.init.star;

import com.example.storytell.init.network.*;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.network.PacketDistributor;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Mod.EventBusSubscriber(modid = "storytell")
public class StarManager {
    private static final List<CustomStar> stars = new ArrayList<>();
    private static boolean initialized = false;
    private static long lastUpdateTime = 0;

    // Оптимизация: отслеживание предыдущих состояний для избежания избыточных обновлений
    private static final Map<String, Boolean> lastVisibilityStates = new ConcurrentHashMap<>();
    private static final Map<String, float[]> lastPositionStates = new ConcurrentHashMap<>();
    private static final Map<String, Integer> lastColorStates = new ConcurrentHashMap<>();
    private static final Map<String, Boolean> needsInitialSync = new ConcurrentHashMap<>();

    // Флаги для управления синхронизацией
    private static boolean needsFullSync = true;
    private static long lastFullSyncTime = 0;
    private static final long FULL_SYNC_COOLDOWN = 5000; // 5 секунд между полными синхронизациями

    // Оптимизация: ограничение частоты сообщений об инициализации
    private static long lastInitMessageTime = 0;
    private static final long INIT_MESSAGE_COOLDOWN = 5000; // 5 секунд между сообщениями об инициализации

    // Гарантированная инициализация при первом обращении
    private static void ensureInitialized() {
        if (!initialized) {
            init();
        }
    }

    public static void init() {
        if (initialized) {
            long currentTime = System.currentTimeMillis();
            if (currentTime - lastInitMessageTime > INIT_MESSAGE_COOLDOWN) {
                System.out.println("StarManager already initialized, skipping...");
                lastInitMessageTime = currentTime;
            }
            return;
        }

        System.out.println("Initializing custom star system with JSON models...");

        // Очищаем списки на случай повторной инициализации
        stars.clear();
        lastVisibilityStates.clear();
        lastPositionStates.clear();
        lastColorStates.clear();
        needsInitialSync.clear();

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
                60.0f,           // Distance
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
                60.0f,           // Distance
                3f,             // Rotation speed
                0.1f,           // Pulse speed
                0.4f,           // Pulse amount
                true             // Default visibility - VISIBLE
        );

        stars.add(blueStar);
        stars.add(coreStar);
        stars.add(core2Star);

        // Инициализируем отслеживание состояний
        for (CustomStar star : stars) {
            String starName = star.getName();
            lastVisibilityStates.put(starName, star.isVisible());
            lastPositionStates.put(starName, new float[]{
                    star.getRightAscension(),
                    star.getDeclination(),
                    star.getDistance()
            });
            lastColorStates.put(starName, star.getColor());
            needsInitialSync.put(starName, true);
        }

        lastUpdateTime = System.currentTimeMillis();
        lastInitMessageTime = System.currentTimeMillis();
        initialized = true;
        needsFullSync = true;

        System.out.println("Custom star system initialized with " + stars.size() + " stars");
        System.out.println("Available stars: " + stars.stream().map(CustomStar::getName).reduce((a, b) -> a + ", " + b).orElse("none"));

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

    @SubscribeEvent
    public static void onServerTick(TickEvent.ServerTickEvent event) {
        if (event.phase == TickEvent.Phase.START && needsFullSync &&
                System.currentTimeMillis() - lastFullSyncTime > FULL_SYNC_COOLDOWN) {
            // Выполняем полную синхронизацию при первом тике сервера после кулдауна
            syncAllStars();
            lastFullSyncTime = System.currentTimeMillis();
            needsFullSync = false;
        }
    }

    public static List<CustomStar> getStars() {
        ensureInitialized();
        return new ArrayList<>(stars);
    }

    public static boolean isInitialized() {
        return initialized;
    }

    public static CustomStar getStarByName(String name) {
        ensureInitialized();
        for (CustomStar star : stars) {
            if (star.getName().equals(name)) {
                return star;
            }
        }

        // Оптимизация: ограничиваем частоту сообщений об ошибках
        long currentTime = System.currentTimeMillis();
        if (currentTime - lastInitMessageTime > INIT_MESSAGE_COOLDOWN) {
            System.err.println("Star not found: " + name + ". Available stars: " +
                    stars.stream().map(CustomStar::getName).reduce((a, b) -> a + ", " + b).orElse("none"));
            lastInitMessageTime = currentTime;
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
        ensureInitialized();

        // Оптимизация: ограничиваем частоту отладочных сообщений
        long currentTime = System.currentTimeMillis();
        if (currentTime - lastInitMessageTime > INIT_MESSAGE_COOLDOWN) {
            System.out.println("Applying offset to star " + starName + " on side: " +
                    (net.minecraftforge.fml.loading.FMLEnvironment.dist.isClient() ? "CLIENT" : "SERVER"));
            lastInitMessageTime = currentTime;
        }

        CustomStar star = getStarByName(starName);
        if (star != null) {
            Runnable onExpire = () -> {
                if (currentTime - lastInitMessageTime > INIT_MESSAGE_COOLDOWN) {
                    System.out.println("Star " + starName + " returned to base position");
                    lastInitMessageTime = currentTime;
                }
            };
            star.applyPositionModifier("offset", offsetX, offsetY, offsetZ, duration, onExpire);

            if (currentTime - lastInitMessageTime > INIT_MESSAGE_COOLDOWN) {
                System.out.println("Applied star offset: " + starName + " moved by (" + offsetX + ", " + offsetY + ", " + offsetZ + ") for " + duration + " ticks");
                lastInitMessageTime = currentTime;
            }

            // Отправляем обновление только если это сервер
            if (!net.minecraftforge.fml.loading.FMLEnvironment.dist.isClient()) {
                sendMoveUpdate(starName, offsetX, offsetY, offsetZ, duration);
            }
        } else {
            if (currentTime - lastInitMessageTime > INIT_MESSAGE_COOLDOWN) {
                System.err.println("Star not found: " + starName);
                lastInitMessageTime = currentTime;
            }
        }
    }

    // Новый метод для плавного перемещения с выбором типа easing
    public static void applySmoothMovement(String starName, float targetX, float targetY, float targetZ,
                                           int duration, String easingType) {
        ensureInitialized();
        CustomStar star = getStarByName(starName);
        if (star != null) {
            // Получаем текущую позицию звезды
            float currentX = star.getX();
            float currentY = star.getY();
            float currentZ = star.getZ();

            Runnable onExpire = () -> {
                long currentTime = System.currentTimeMillis();
                if (currentTime - lastInitMessageTime > INIT_MESSAGE_COOLDOWN) {
                    System.out.println("Smooth movement completed for star " + starName);
                    lastInitMessageTime = currentTime;
                }
            };

            star.applySmoothMovement(currentX, currentY, currentZ, targetX, targetY, targetZ,
                    duration, onExpire, easingType);

            long currentTime = System.currentTimeMillis();
            if (currentTime - lastInitMessageTime > INIT_MESSAGE_COOLDOWN) {
                System.out.println("Started smooth movement for star " + starName +
                        " from (" + currentX + ", " + currentY + ", " + currentZ + ") " +
                        "to (" + targetX + ", " + targetY + ", " + targetZ + ") " +
                        "over " + duration + " ticks with easing: " + easingType);
                lastInitMessageTime = currentTime;
            }

            // Отправляем обновление только если это сервер
            if (!net.minecraftforge.fml.loading.FMLEnvironment.dist.isClient()) {
                sendSmoothMoveUpdate(starName, targetX, targetY, targetZ, duration, easingType);
            }
        } else {
            long currentTime = System.currentTimeMillis();
            if (currentTime - lastInitMessageTime > INIT_MESSAGE_COOLDOWN) {
                System.err.println("Star not found: " + starName);
                lastInitMessageTime = currentTime;
            }
        }
    }

    public static void applyAbsolutePosition(String starName, float rightAscension, float declination, float distance, int duration) {
        ensureInitialized();
        try {
            CustomStar star = getStarByName(starName);
            if (star != null) {
                // Проверяем, действительно ли изменились координаты
                boolean positionChanged =
                        star.getRightAscension() != rightAscension ||
                                star.getDeclination() != declination ||
                                star.getDistance() != distance;

                // Если позиция не изменилась и это постоянное позиционирование, пропускаем
                if (!positionChanged && duration <= 1) {
                    long currentTime = System.currentTimeMillis();
                    if (currentTime - lastInitMessageTime > INIT_MESSAGE_COOLDOWN) {
                        System.out.println("Skipping position update for " + starName + " - no changes detected");
                        lastInitMessageTime = currentTime;
                    }
                    return;
                }

                // Если duration = 1, это означает постоянное позиционирование
                if (duration <= 1) {
                    // Сбрасываем все модификаторы и устанавливаем базовую позицию
                    StarModifierManager.removeModifiers(starName);

                    // Обновляем небесные координаты звезды
                    star.setCelestialCoordinates(rightAscension, declination, distance);
                    star.calculatePosition();

                    // Обновляем отслеживание состояния
                    lastPositionStates.put(starName, new float[]{rightAscension, declination, distance});

                    long currentTime = System.currentTimeMillis();
                    if (currentTime - lastInitMessageTime > INIT_MESSAGE_COOLDOWN) {
                        System.out.println("Permanently set star " + starName + " to RA=" + rightAscension +
                                ", Dec=" + declination + ", Dist=" + distance);
                        lastInitMessageTime = currentTime;
                    }
                } else {
                    // Временное позиционирование через модификатор
                    double raRad = Math.toRadians(rightAscension);
                    double decRad = Math.toRadians(declination);

                    float targetX = (float) (distance * Math.cos(decRad) * Math.cos(raRad));
                    float targetY = (float) (distance * Math.sin(decRad));
                    float targetZ = (float) (distance * Math.cos(decRad) * Math.sin(raRad));

                    Runnable onExpire = () -> {
                        long currentTime = System.currentTimeMillis();
                        if (currentTime - lastInitMessageTime > INIT_MESSAGE_COOLDOWN) {
                            System.out.println("Absolute position modifier expired for star " + starName);
                            lastInitMessageTime = currentTime;
                        }
                    };

                    StarModifierManager.StarModifier modifier = new StarModifierManager.StarModifier(
                            "absolute", targetX, targetY, targetZ, duration, onExpire
                    );
                    StarModifierManager.addModifier(starName, modifier);

                    long currentTime = System.currentTimeMillis();
                    if (currentTime - lastInitMessageTime > INIT_MESSAGE_COOLDOWN) {
                        System.out.println("Applied temporary absolute position to star " + starName);
                        lastInitMessageTime = currentTime;
                    }
                }

                // Отправляем обновление только если позиция изменилась и это сервер
                if (positionChanged && !net.minecraftforge.fml.loading.FMLEnvironment.dist.isClient()) {
                    sendAbsolutePositionUpdate(starName, rightAscension, declination, distance, duration);
                }

            } else {
                long currentTime = System.currentTimeMillis();
                if (currentTime - lastInitMessageTime > INIT_MESSAGE_COOLDOWN) {
                    System.err.println("Star not found: " + starName);
                    lastInitMessageTime = currentTime;
                }
            }
        } catch (Exception e) {
            long currentTime = System.currentTimeMillis();
            if (currentTime - lastInitMessageTime > INIT_MESSAGE_COOLDOWN) {
                System.err.println("Error in applyAbsolutePosition for star " + starName + ": " + e.getMessage());
                lastInitMessageTime = currentTime;
            }
            e.printStackTrace();
        }
    }

    public static void sendAbsolutePositionUpdate(String starName, float rightAscension, float declination, float distance, int duration) {
        ensureInitialized();
        StarAbsolutePositionPacket packet = new StarAbsolutePositionPacket(starName, rightAscension, declination, distance, duration);
        NetworkHandler.INSTANCE.send(PacketDistributor.ALL.noArg(), packet);

        long currentTime = System.currentTimeMillis();
        if (currentTime - lastInitMessageTime > INIT_MESSAGE_COOLDOWN) {
            System.out.println("Sent absolute position update for star " + starName + " to all clients");
            lastInitMessageTime = currentTime;
        }
    }

    public static void resetStarToBasePosition(String starName) {
        ensureInitialized();
        try {
            CustomStar star = getStarByName(starName);
            if (star != null) {
                // Удаляем все модификаторы для этой звезды
                StarModifierManager.removeModifiers(starName);

                // Сбрасываем позицию
                star.resetToBasePosition();

                long currentTime = System.currentTimeMillis();
                if (currentTime - lastInitMessageTime > INIT_MESSAGE_COOLDOWN) {
                    System.out.println("Reset star " + starName + " to base position safely");
                    lastInitMessageTime = currentTime;
                }
            }
        } catch (Exception e) {
            long currentTime = System.currentTimeMillis();
            if (currentTime - lastInitMessageTime > INIT_MESSAGE_COOLDOWN) {
                System.err.println("Error resetting star to base position: " + e.getMessage());
                lastInitMessageTime = currentTime;
            }
            e.printStackTrace();
        }
    }

    public static void moveStarAbovePlayer(String starName, ServerPlayer player, float height, int duration) {
        ensureInitialized();
        CustomStar star = getStarByName(starName);
        if (star != null) {
            // Calculate position directly above player
            float playerY = (float) player.getY();
            float newY = playerY + height;

            Runnable onExpire = () -> {
                long currentTime = System.currentTimeMillis();
                if (currentTime - lastInitMessageTime > INIT_MESSAGE_COOLDOWN) {
                    System.out.println("Star " + starName + " returned to base position");
                    lastInitMessageTime = currentTime;
                }
            };

            // Use player_position type to set absolute Y position above player
            star.applyPositionModifier("player_position", 0, newY, 0, duration, onExpire);

            long currentTime = System.currentTimeMillis();
            if (currentTime - lastInitMessageTime > INIT_MESSAGE_COOLDOWN) {
                System.out.println("Moved star " + starName + " above player " + player.getScoreboardName() +
                        " at height " + height + " for " + duration + " ticks");
                lastInitMessageTime = currentTime;
            }
        } else {
            long currentTime = System.currentTimeMillis();
            if (currentTime - lastInitMessageTime > INIT_MESSAGE_COOLDOWN) {
                System.err.println("Star not found: " + starName);
                lastInitMessageTime = currentTime;
            }
        }
    }

    public static void setStarVisibility(String starName, boolean visible) {
        ensureInitialized();
        CustomStar star = getStarByName(starName);
        if (star != null) {
            // Проверяем, действительно ли изменилась видимость
            boolean currentVisibility = star.isVisible();
            if (currentVisibility == visible && !needsInitialSync.get(starName)) {
                long currentTime = System.currentTimeMillis();
                if (currentTime - lastInitMessageTime > INIT_MESSAGE_COOLDOWN) {
                    System.out.println("Skipping visibility update for " + starName + " - no changes");
                    lastInitMessageTime = currentTime;
                }
                return;
            }

            star.setVisible(visible);
            needsInitialSync.put(starName, false);
            lastVisibilityStates.put(starName, visible);

            // Синхронизируем видимость со всеми клиентами только если это сервер
            if (!net.minecraftforge.fml.loading.FMLEnvironment.dist.isClient()) {
                sendVisibilityUpdate(starName, visible, false, false);
            }

            long currentTime = System.currentTimeMillis();
            if (currentTime - lastInitMessageTime > INIT_MESSAGE_COOLDOWN) {
                System.out.println("Set star " + starName + " visibility to: " + visible);
                lastInitMessageTime = currentTime;
            }
        } else {
            long currentTime = System.currentTimeMillis();
            if (currentTime - lastInitMessageTime > INIT_MESSAGE_COOLDOWN) {
                System.err.println("Star not found: " + starName);
                lastInitMessageTime = currentTime;
            }
        }
    }

    // Новый метод для переключения видимости
    public static void toggleStarVisibility(String starName) {
        ensureInitialized();
        CustomStar star = getStarByName(starName);
        if (star != null) {
            boolean oldVisibility = star.isVisible();
            star.toggleVisibility();
            boolean newVisibility = star.isVisible();

            // Обновляем отслеживание состояния
            lastVisibilityStates.put(starName, newVisibility);

            // Синхронизируем видимость со всеми клиентами только если это сервер и видимость изменилась
            if (!net.minecraftforge.fml.loading.FMLEnvironment.dist.isClient() && oldVisibility != newVisibility) {
                sendVisibilityUpdate(starName, newVisibility, false, true);
            }

            long currentTime = System.currentTimeMillis();
            if (currentTime - lastInitMessageTime > INIT_MESSAGE_COOLDOWN) {
                System.out.println("Toggled star " + starName + " visibility to: " + newVisibility);
                lastInitMessageTime = currentTime;
            }
        } else {
            long currentTime = System.currentTimeMillis();
            if (currentTime - lastInitMessageTime > INIT_MESSAGE_COOLDOWN) {
                System.err.println("Star not found: " + starName);
                lastInitMessageTime = currentTime;
            }
        }
    }

    public static void resetStarVisibility(String starName) {
        ensureInitialized();
        CustomStar star = getStarByName(starName);
        if (star != null) {
            boolean oldVisibility = star.isVisible();
            star.resetToDefaultVisibility();
            boolean defaultVisibility = star.getDefaultVisible();

            // Проверяем, действительно ли изменилась видимость
            if (oldVisibility == defaultVisibility && !needsInitialSync.get(starName)) {
                long currentTime = System.currentTimeMillis();
                if (currentTime - lastInitMessageTime > INIT_MESSAGE_COOLDOWN) {
                    System.out.println("Skipping visibility reset for " + starName + " - already at default");
                    lastInitMessageTime = currentTime;
                }
                return;
            }

            lastVisibilityStates.put(starName, defaultVisibility);
            needsInitialSync.put(starName, false);

            // Синхронизируем видимость со всеми клиентами только если это сервер
            if (!net.minecraftforge.fml.loading.FMLEnvironment.dist.isClient()) {
                sendVisibilityUpdate(starName, defaultVisibility, true, false);
            }

            long currentTime = System.currentTimeMillis();
            if (currentTime - lastInitMessageTime > INIT_MESSAGE_COOLDOWN) {
                System.out.println("Reset star " + starName + " visibility to default: " + defaultVisibility);
                lastInitMessageTime = currentTime;
            }
        } else {
            long currentTime = System.currentTimeMillis();
            if (currentTime - lastInitMessageTime > INIT_MESSAGE_COOLDOWN) {
                System.err.println("Star not found: " + starName);
                lastInitMessageTime = currentTime;
            }
        }
    }

    // Метод для изменения цвета звезды
    public static void setStarColor(String starName, int color) {
        ensureInitialized();
        CustomStar star = getStarByName(starName);
        if (star != null) {
            // Проверяем, действительно ли изменился цвет
            int currentColor = star.getColor();
            if (currentColor == color && !needsInitialSync.get(starName)) {
                long currentTime = System.currentTimeMillis();
                if (currentTime - lastInitMessageTime > INIT_MESSAGE_COOLDOWN) {
                    System.out.println("Skipping color update for " + starName + " - no changes");
                    lastInitMessageTime = currentTime;
                }
                return;
            }

            star.setColor(color);
            lastColorStates.put(starName, color);
            needsInitialSync.put(starName, false);

            // Отправляем обновление только если это сервер
            if (!net.minecraftforge.fml.loading.FMLEnvironment.dist.isClient()) {
                sendColorUpdate(starName, color);
            }

            long currentTime = System.currentTimeMillis();
            if (currentTime - lastInitMessageTime > INIT_MESSAGE_COOLDOWN) {
                System.out.println("Set star " + starName + " color to: " + Integer.toHexString(color));
                lastInitMessageTime = currentTime;
            }
        } else {
            long currentTime = System.currentTimeMillis();
            if (currentTime - lastInitMessageTime > INIT_MESSAGE_COOLDOWN) {
                System.err.println("Star not found: " + starName);
                lastInitMessageTime = currentTime;
            }
        }
    }

    // Новый метод для плавного изменения цвета звезды
    public static void startStarColorAnimation(String starName, int targetColor, int durationTicks) {
        ensureInitialized();
        CustomStar star = getStarByName(starName);
        if (star != null) {
            // Проверяем, действительно ли нужно менять цвет
            int currentColor = star.getColor();
            if (currentColor == targetColor && !needsInitialSync.get(starName)) {
                long currentTime = System.currentTimeMillis();
                if (currentTime - lastInitMessageTime > INIT_MESSAGE_COOLDOWN) {
                    System.out.println("Skipping color animation for " + starName + " - already at target color");
                    lastInitMessageTime = currentTime;
                }
                return;
            }

            star.startColorAnimation(targetColor, durationTicks);
            lastColorStates.put(starName, targetColor);
            needsInitialSync.put(starName, false);

            // Отправляем обновление только если это сервер
            if (!net.minecraftforge.fml.loading.FMLEnvironment.dist.isClient()) {
                sendColorAnimationUpdate(starName, targetColor, durationTicks);
            }

            long currentTime = System.currentTimeMillis();
            if (currentTime - lastInitMessageTime > INIT_MESSAGE_COOLDOWN) {
                System.out.println("Started color animation for star " + starName +
                        " to color " + Integer.toHexString(targetColor) + " over " + durationTicks + " ticks");
                lastInitMessageTime = currentTime;
            }
        } else {
            long currentTime = System.currentTimeMillis();
            if (currentTime - lastInitMessageTime > INIT_MESSAGE_COOLDOWN) {
                System.err.println("Star not found: " + starName);
                lastInitMessageTime = currentTime;
            }
        }
    }

    // Метод для удаления звезды
    public static void removeStar(String starName) {
        ensureInitialized();
        CustomStar star = getStarByName(starName);
        if (star != null) {
            stars.remove(star);
            star.removeFromConfig();

            // Удаляем из отслеживания состояний
            lastVisibilityStates.remove(starName);
            lastPositionStates.remove(starName);
            lastColorStates.remove(starName);
            needsInitialSync.remove(starName);

            long currentTime = System.currentTimeMillis();
            if (currentTime - lastInitMessageTime > INIT_MESSAGE_COOLDOWN) {
                System.out.println("Removed star: " + starName);
                lastInitMessageTime = currentTime;
            }
        } else {
            long currentTime = System.currentTimeMillis();
            if (currentTime - lastInitMessageTime > INIT_MESSAGE_COOLDOWN) {
                System.err.println("Star not found: " + starName);
                lastInitMessageTime = currentTime;
            }
        }
    }

    // Новый метод для применения модификаторов
    public static void applyStarModifier(String starName, String modifierType, float x, float y, float z, int duration) {
        ensureInitialized();
        CustomStar star = getStarByName(starName);
        if (star != null) {
            Runnable onExpire = () -> {
                long currentTime = System.currentTimeMillis();
                if (currentTime - lastInitMessageTime > INIT_MESSAGE_COOLDOWN) {
                    System.out.println("Modifier expired for star " + starName);
                    lastInitMessageTime = currentTime;
                }
            };
            star.applyPositionModifier(modifierType, x, y, z, duration, onExpire);

            long currentTime = System.currentTimeMillis();
            if (currentTime - lastInitMessageTime > INIT_MESSAGE_COOLDOWN) {
                System.out.println("Applied modifier to star " + starName + ": " + modifierType +
                        " (" + x + ", " + y + ", " + z + ") for " + duration + " ticks");
                lastInitMessageTime = currentTime;
            }

            // Отправляем обновление только если это сервер
            if (!net.minecraftforge.fml.loading.FMLEnvironment.dist.isClient()) {
                sendModifierUpdate(starName, modifierType, x, y, z, duration);
            }
        } else {
            long currentTime = System.currentTimeMillis();
            if (currentTime - lastInitMessageTime > INIT_MESSAGE_COOLDOWN) {
                System.err.println("Star not found: " + starName);
                lastInitMessageTime = currentTime;
            }
        }
    }

    // Новый метод для синхронизации позиций звезд
    public static void syncStarPositionsToPlayer(ServerPlayer player) {
        ensureInitialized();
        for (CustomStar star : stars) {
            // Отправляем текущую позицию каждой звезды
            StarAbsolutePositionPacket packet = new StarAbsolutePositionPacket(
                    star.getName(),
                    star.getRightAscension(),
                    star.getDeclination(),
                    star.getDistance(),
                    1 // permanent
            );
            NetworkHandler.INSTANCE.send(PacketDistributor.PLAYER.with(() -> player), packet);
        }

        long currentTime = System.currentTimeMillis();
        if (currentTime - lastInitMessageTime > INIT_MESSAGE_COOLDOWN) {
            System.out.println("Synchronized star positions with player: " + player.getScoreboardName());
            lastInitMessageTime = currentTime;
        }
    }

    // Метод для синхронизации всех звезд с новым игроком
    public static void syncStarsToPlayer(ServerPlayer player) {
        ensureInitialized();

        // Используем один пакет для синхронизации всех состояний видимости
        Map<String, Boolean> visibilityMap = new HashMap<>();
        for (CustomStar star : stars) {
            visibilityMap.put(star.getName(), star.isVisible());
            needsInitialSync.put(star.getName(), false); // Помечаем как синхронизированные
        }

        SyncAllStarsPacket packet = new SyncAllStarsPacket(visibilityMap);
        NetworkHandler.INSTANCE.send(PacketDistributor.PLAYER.with(() -> player), packet);

        // Синхронизируем позиции
        syncStarPositionsToPlayer(player);

        // Синхронизируем цвета
        for (CustomStar star : stars) {
            StarColorPacket colorPacket = new StarColorPacket(star.getName(), star.getColor());
            NetworkHandler.INSTANCE.send(PacketDistributor.PLAYER.with(() -> player), colorPacket);
        }

        long currentTime = System.currentTimeMillis();
        if (currentTime - lastInitMessageTime > INIT_MESSAGE_COOLDOWN) {
            System.out.println("Fully synchronized all stars with player: " + player.getScoreboardName());
            lastInitMessageTime = currentTime;
        }
    }

    // Новый метод для пакетной синхронизации всех звезд
    public static void syncAllStars() {
        if (!initialized) return;

        ensureInitialized();

        long currentTime = System.currentTimeMillis();
        if (currentTime - lastInitMessageTime > INIT_MESSAGE_COOLDOWN) {
            System.out.println("Performing full star synchronization...");
            lastInitMessageTime = currentTime;
        }

        // Синхронизируем все звезды одним пакетом
        Map<String, Boolean> visibilityMap = new HashMap<>();
        for (CustomStar star : stars) {
            visibilityMap.put(star.getName(), star.isVisible());
            needsInitialSync.put(star.getName(), false);
        }

        SyncAllStarsPacket packet = new SyncAllStarsPacket(visibilityMap);
        NetworkHandler.INSTANCE.send(PacketDistributor.ALL.noArg(), packet);

        // Отправляем позиции для каждой звезды
        for (CustomStar star : stars) {
            StarAbsolutePositionPacket posPacket = new StarAbsolutePositionPacket(
                    star.getName(),
                    star.getRightAscension(),
                    star.getDeclination(),
                    star.getDistance(),
                    1
            );
            NetworkHandler.INSTANCE.send(PacketDistributor.ALL.noArg(), posPacket);
        }

        // Отправляем цвета для каждой звезды
        for (CustomStar star : stars) {
            StarColorPacket colorPacket = new StarColorPacket(star.getName(), star.getColor());
            NetworkHandler.INSTANCE.send(PacketDistributor.ALL.noArg(), colorPacket);
        }

        if (currentTime - lastInitMessageTime > INIT_MESSAGE_COOLDOWN) {
            System.out.println("Full star synchronization completed for " + stars.size() + " stars");
            lastInitMessageTime = currentTime;
        }
    }

    // Методы для отправки сетевых пакетов
    public static void sendVisibilityUpdate(String starName, boolean visible, boolean isReset, boolean isToggle) {
        ensureInitialized();
        StarVisibilityPacket packet = new StarVisibilityPacket(starName, visible, isReset, isToggle);
        NetworkHandler.INSTANCE.send(PacketDistributor.ALL.noArg(), packet);

        long currentTime = System.currentTimeMillis();
        if (currentTime - lastInitMessageTime > INIT_MESSAGE_COOLDOWN) {
            System.out.println("Sent visibility update for star " + starName + " to all clients");
            lastInitMessageTime = currentTime;
        }
    }

    public static void sendMoveUpdate(String starName, float offsetX, float offsetY, float offsetZ, int duration) {
        ensureInitialized();
        StarMovePacket packet = new StarMovePacket(starName, offsetX, offsetY, offsetZ, duration);
        NetworkHandler.INSTANCE.send(PacketDistributor.ALL.noArg(), packet);
    }

    public static void sendSmoothMoveUpdate(String starName, float targetX, float targetY, float targetZ, int duration, String easingType) {
        ensureInitialized();
        StarSmoothMovePacket packet = new StarSmoothMovePacket(starName, targetX, targetY, targetZ, duration, easingType);
        NetworkHandler.INSTANCE.send(PacketDistributor.ALL.noArg(), packet);
    }

    public static void sendModifierUpdate(String starName, String modifierType, float x, float y, float z, int duration) {
        ensureInitialized();
        StarModifierPacket packet = new StarModifierPacket(starName, modifierType, x, y, z, duration);
        NetworkHandler.INSTANCE.send(PacketDistributor.ALL.noArg(), packet);
    }

    // Новый метод для отправки обновления цвета
    public static void sendColorUpdate(String starName, int color) {
        ensureInitialized();
        StarColorPacket packet = new StarColorPacket(starName, color);
        NetworkHandler.INSTANCE.send(PacketDistributor.ALL.noArg(), packet);
    }

    // Новый метод для отправки анимации цвета
    public static void sendColorAnimationUpdate(String starName, int targetColor, int durationTicks) {
        ensureInitialized();
        StarColorAnimationPacket packet = new StarColorAnimationPacket(starName, targetColor, durationTicks);
        NetworkHandler.INSTANCE.send(PacketDistributor.ALL.noArg(), packet);
    }

    // Метод для принудительной синхронизации всех состояний
    public static void forceFullSync() {
        needsFullSync = true;
        for (String starName : needsInitialSync.keySet()) {
            needsInitialSync.put(starName, true);
        }

        long currentTime = System.currentTimeMillis();
        if (currentTime - lastInitMessageTime > INIT_MESSAGE_COOLDOWN) {
            System.out.println("Forced full synchronization for next tick");
            lastInitMessageTime = currentTime;
        }
    }
}