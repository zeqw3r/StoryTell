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

        public StarModifier(String type, float x, float y, float z, int duration, Runnable onExpire) {
            this.type = type;
            this.x = x;
            this.y = y;
            this.z = z;
            this.duration = duration;
            this.onExpire = onExpire;
        }

        public boolean shouldRemove() {
            duration--;
            if (duration <= 0 && onExpire != null) {
                onExpire.run();
            }
            return duration <= 0;
        }

        public String getType() { return type; }
        public float getX() { return x; }
        public float getY() { return y; }
        public float getZ() { return z; }
        public int getDuration() { return duration; }
    }
}