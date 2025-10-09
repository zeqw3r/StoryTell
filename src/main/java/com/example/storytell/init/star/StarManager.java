// StarManager.java
package com.example.storytell.init.star;

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

        // Create only one star for testing
        stars.add(new CustomStar(
                "blue_star",
                "star/blue_star",
                50.0f,        // Size
                0xFF87CEEB,  // Light blue color
                45.0f,       // Right Ascension
                30.0f,       // Declination
                150.0f,      // Distance
                0.5f,        // Rotation speed
                2.0f,        // Pulse speed
                0.3f         // Pulse amount
        ));

        initialized = true;
        System.out.println("Custom star system initialized with " + stars.size() + " star");
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

    public static void updateStars(float partialTick) {
        if (!initialized) return;

        for (CustomStar star : stars) {
            // Future: individual star movement logic could go here
        }
    }
}