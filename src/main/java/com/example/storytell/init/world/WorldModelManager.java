// WorldModelManager.java
package com.example.storytell.init.world;

import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import com.mojang.blaze3d.vertex.PoseStack;

import java.util.*;

public class WorldModelManager {
    private static final Map<String, WorldModel> WORLD_MODELS = new HashMap<>();

    public static void addModel(WorldModel model) {
        WORLD_MODELS.put(model.getId(), model);
    }

    public static void removeModel(String id) {
        WORLD_MODELS.remove(id);
    }

    public static WorldModel getModel(String id) {
        return WORLD_MODELS.get(id);
    }

    public static Collection<WorldModel> getAllModels() {
        return WORLD_MODELS.values();
    }

    @OnlyIn(Dist.CLIENT)
    public static void renderAll(PoseStack poseStack, float partialTick, double cameraX, double cameraY, double cameraZ) {
        for (WorldModel model : WORLD_MODELS.values()) {
            model.render(poseStack, partialTick, cameraX, cameraY, cameraZ);
        }
    }
}