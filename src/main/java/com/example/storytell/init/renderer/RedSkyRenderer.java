package com.example.storytell.init.renderer;

import com.example.storytell.init.network.ClientEventHandlers;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.*;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderLevelStageEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.joml.Matrix4f;

@Mod.EventBusSubscriber(modid = "storytell", bus = Mod.EventBusSubscriber.Bus.FORGE, value = Dist.CLIENT)
public class RedSkyRenderer {

    @SubscribeEvent
    public static void onRenderLevel(RenderLevelStageEvent event) {
        if (event.getStage() == RenderLevelStageEvent.Stage.AFTER_SKY && ClientEventHandlers.isRedSkyActive()) {
            renderRedSkyOverlay(event.getProjectionMatrix(), ClientEventHandlers.getRedSkyIntensity());
        }
    }

    private static void renderRedSkyOverlay(Matrix4f projectionMatrix, float intensity) {
        if (intensity <= 0) return;

        RenderSystem.disableDepthTest();
        RenderSystem.depthMask(false);
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();

        Tesselator tesselator = Tesselator.getInstance();
        BufferBuilder bufferBuilder = tesselator.getBuilder();

        RenderSystem.setShader(GameRenderer::getPositionColorShader);
        bufferBuilder.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_COLOR);

        // Красный цвет с прозрачностью
        float red = 1.0f;
        float green = 0.0f;
        float blue = 0.0f;
        float alpha = 0.6f * intensity;

        // Заполняем весь экран красным цветом
        bufferBuilder.vertex(projectionMatrix, -1.0f, -1.0f, 0.0f).color(red, green, blue, alpha).endVertex();
        bufferBuilder.vertex(projectionMatrix, -1.0f, 1.0f, 0.0f).color(red, green, blue, alpha).endVertex();
        bufferBuilder.vertex(projectionMatrix, 1.0f, 1.0f, 0.0f).color(red, green, blue, alpha).endVertex();
        bufferBuilder.vertex(projectionMatrix, 1.0f, -1.0f, 0.0f).color(red, green, blue, alpha).endVertex();

        tesselator.end();

        RenderSystem.disableBlend();
        RenderSystem.depthMask(true);
        RenderSystem.enableDepthTest();
    }
}