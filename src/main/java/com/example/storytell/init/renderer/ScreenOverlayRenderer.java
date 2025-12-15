package com.example.storytell.init.renderer;

import com.example.storytell.init.network.ClientEventHandlers;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.*;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderGuiEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.joml.Matrix4f;

@Mod.EventBusSubscriber(modid = "storytell", bus = Mod.EventBusSubscriber.Bus.FORGE, value = Dist.CLIENT)
public class ScreenOverlayRenderer {

    @SubscribeEvent
    public static void onRenderGui(RenderGuiEvent event) {
        if (!ClientEventHandlers.isRedSkyActive()) {
            return;
        }

        float intensity = ClientEventHandlers.getRedSkyIntensity();
        if (intensity <= 0) return;

        renderRedOverlay(event.getGuiGraphics().pose().last().pose(), intensity);
    }

    private static void renderRedOverlay(Matrix4f matrix, float intensity) {
        RenderSystem.disableDepthTest();
        RenderSystem.depthMask(false);
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();

        Tesselator tesselator = Tesselator.getInstance();
        BufferBuilder bufferBuilder = tesselator.getBuilder();

        RenderSystem.setShader(GameRenderer::getPositionColorShader);
        bufferBuilder.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_COLOR);

        // Полупрозрачный красный цвет с увеличенной прозрачностью
        float red = 1.0f;
        float green = 0.0f;
        float blue = 0.0f;
        float alpha = 0.25f * intensity;

        // Получаем размеры экрана
        Minecraft minecraft = Minecraft.getInstance();
        int width = minecraft.getWindow().getGuiScaledWidth();
        int height = minecraft.getWindow().getGuiScaledHeight();

        // Рисуем прямоугольник на весь экран
        bufferBuilder.vertex(matrix, 0, 0, 0).color(red, green, blue, alpha).endVertex();
        bufferBuilder.vertex(matrix, 0, height, 0).color(red, green, blue, alpha).endVertex();
        bufferBuilder.vertex(matrix, width, height, 0).color(red, green, blue, alpha).endVertex();
        bufferBuilder.vertex(matrix, width, 0, 0).color(red, green, blue, alpha).endVertex();

        tesselator.end();

        RenderSystem.disableBlend();
        RenderSystem.depthMask(true);
        RenderSystem.enableDepthTest();
    }
}