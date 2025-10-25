// ClientEventHandlers.java
package com.example.storytell.init.network;

import com.example.storytell.init.world.WorldModelManager;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderLevelStageEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = "storytell", bus = Mod.EventBusSubscriber.Bus.FORGE, value = Dist.CLIENT)
public class ClientEventHandlers {
    private static int redSkyDuration = 0;
    private static long redSkyStartTime = 0;

    public static void handleRedSky(boolean activate, int duration) {
        if (activate) {
            redSkyDuration = duration * 20;
            redSkyStartTime = System.currentTimeMillis();
        } else {
            redSkyDuration = 0;
        }
    }

    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase == TickEvent.Phase.END && redSkyDuration > 0) {
            redSkyDuration--;
        }
    }

    public static boolean isRedSkyActive() {
        return redSkyDuration > 0;
    }

    public static float getRedSkyIntensity() {
        if (redSkyDuration <= 0) return 0.0f;
        long currentTime = System.currentTimeMillis();
        float progress = (currentTime - redSkyStartTime) / 1000.0f;

        if (progress < 2.0f) {
            return progress / 2.0f;
        } else if (progress > 58.0f) {
            return Math.max(0.0f, (60.0f - progress) / 2.0f);
        }
        return 1.0f;
    }

    @SubscribeEvent
    public static void onRenderLevelStage(RenderLevelStageEvent event) {
        if (event.getStage() == RenderLevelStageEvent.Stage.AFTER_TRANSLUCENT_BLOCKS) {
            var poseStack = event.getPoseStack();
            var camera = event.getCamera();
            double cameraX = camera.getPosition().x;
            double cameraY = camera.getPosition().y;
            double cameraZ = camera.getPosition().z;

            poseStack.pushPose();
            WorldModelManager.renderAll(poseStack, event.getPartialTick(), cameraX, cameraY, cameraZ);
            poseStack.popPose();
        }
    }
}