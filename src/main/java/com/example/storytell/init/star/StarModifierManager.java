// StarModifierManager.java
package com.example.storytell.init.star;

import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@Mod.EventBusSubscriber(modid = "storytell")
public class StarModifierManager {
    private static final Map<String, List<StarModifier>> ACTIVE_MODIFIERS = new ConcurrentHashMap<>();

    public static void addModifier(String starName, StarModifier modifier) {
        ACTIVE_MODIFIERS.computeIfAbsent(starName, k -> new ArrayList<>()).add(modifier);
    }

    public static void removeModifiers(String starName) {
        ACTIVE_MODIFIERS.remove(starName);
    }

    public static List<StarModifier> getModifiers(String starName) {
        return ACTIVE_MODIFIERS.getOrDefault(starName, new ArrayList<>());
    }

    @SubscribeEvent
    public static void onServerTick(TickEvent.ServerTickEvent event) {
        if (event.phase == TickEvent.Phase.END) {
            updateModifiers();
        }
    }

    private static void updateModifiers() {
        Iterator<Map.Entry<String, List<StarModifier>>> iterator = ACTIVE_MODIFIERS.entrySet().iterator();

        while (iterator.hasNext()) {
            Map.Entry<String, List<StarModifier>> entry = iterator.next();
            List<StarModifier> modifiers = entry.getValue();

            modifiers.removeIf(StarModifier::shouldRemove);

            if (modifiers.isEmpty()) {
                iterator.remove();
            }
        }
    }

    public static class StarModifier {
        private final String type;
        private final float x;
        private final float y;
        private final float z;
        private int duration;
        private final Runnable onExpire;

        // Новые поля для плавного перемещения
        private final float startX, startY, startZ;
        private final float targetX, targetY, targetZ;
        private int elapsed;
        private boolean isSmoothMove;
        private final String easingType;

        public StarModifier(String type, float x, float y, float z, int duration, Runnable onExpire) {
            this.type = type;
            this.x = x;
            this.y = y;
            this.z = z;
            this.duration = duration;
            this.onExpire = onExpire;
            this.isSmoothMove = false;
            this.startX = 0;
            this.startY = 0;
            this.startZ = 0;
            this.targetX = 0;
            this.targetY = 0;
            this.targetZ = 0;
            this.elapsed = 0;
            this.easingType = "linear";
        }

        // Новый конструктор для плавного перемещения
        public StarModifier(String type, float startX, float startY, float startZ,
                            float targetX, float targetY, float targetZ, int duration,
                            Runnable onExpire, String easingType) {
            this.type = type;
            this.isSmoothMove = true;
            this.startX = startX;
            this.startY = startY;
            this.startZ = startZ;
            this.targetX = targetX;
            this.targetY = targetY;
            this.targetZ = targetZ;
            this.duration = duration;
            this.onExpire = onExpire;
            this.elapsed = 0;
            this.easingType = easingType != null ? easingType : "easeInCubic";
            this.x = 0;
            this.y = 0;
            this.z = 0;
        }

        public boolean shouldRemove() {
            if (isSmoothMove) {
                elapsed++;
                if (elapsed >= duration && onExpire != null) {
                    onExpire.run();
                }
                return elapsed >= duration;
            } else {
                duration--;
                if (duration <= 0 && onExpire != null) {
                    onExpire.run();
                }
                return duration <= 0;
            }
        }

        public String getType() { return type; }
        public float getX() { return x; }
        public float getY() { return y; }
        public float getZ() { return z; }
        public int getDuration() { return duration; }
        public boolean isSmoothMove() { return isSmoothMove; }
        public float getStartX() { return startX; }
        public float getStartY() { return startY; }
        public float getStartZ() { return startZ; }
        public float getTargetX() { return targetX; }
        public float getTargetY() { return targetY; }
        public float getTargetZ() { return targetZ; }
        public int getElapsed() { return elapsed; }
        public String getEasingType() { return easingType; }
        public float getProgress() {
            return duration > 0 ? (float) elapsed / duration : 1.0f;
        }
    }
}