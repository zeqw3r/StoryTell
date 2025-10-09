// SkyboxMixin.java
package com.example.storytell.mixin;

import com.example.storytell.init.star.StarManager;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.*;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.util.Mth;
import org.joml.Matrix4f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LevelRenderer.class)
public class SkyboxMixin {

    @Inject(method = "renderSky", at = @At("TAIL"))
    private void renderCustomStars(com.mojang.blaze3d.vertex.PoseStack poseStack, Matrix4f projectionMatrix, float partialTick, Camera camera, boolean isFoggy, Runnable setupFog, CallbackInfo ci) {
        // Initialize star manager if not already done
        StarManager.init();

        // Get the custom stars from manager
        var stars = StarManager.getStars();
        if (stars.isEmpty()) return;

        // Setup rendering state
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.depthMask(false);

        poseStack.pushPose();

        // Apply the same sky rotation as vanilla stars
        ClientLevel level = Minecraft.getInstance().level;
        if (level != null) {
            float timeOfDay = level.getTimeOfDay(partialTick);
            float skyRotation = timeOfDay * 360.0f;
            poseStack.mulPose(com.mojang.math.Axis.YP.rotationDegrees(skyRotation));
        }

        // Render each custom star
        for (var star : stars) {
            star.render(poseStack, partialTick);
        }

        poseStack.popPose();

        // Cleanup
        RenderSystem.depthMask(true);
        RenderSystem.disableBlend();
    }
}