// Cutscene.java
package com.example.storytell.init.cutscene;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.*;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.resources.ResourceLocation;
import org.joml.Matrix4f;
import java.util.ArrayList;
import java.util.List;

public class Cutscene {
    private static final int DELAY_BETWEEN_IMAGES = 100; // 5 секунд в тиках
    private static final int FADE_DURATION = 20; // 1 секунда для анимации
    private static final ResourceLocation DEFAULT_BACKGROUND = new ResourceLocation("storytell", "textures/cutscene/background.png");

    private final String folderName;
    private final List<ResourceLocation> images;
    private final List<Float> imageAlphas;
    private int currentImageIndex;
    private int timer;
    private boolean active;

    public Cutscene(String folderName) {
        this.folderName = folderName;
        this.images = new ArrayList<>();
        this.imageAlphas = new ArrayList<>();
        this.currentImageIndex = -1;
        this.timer = 0;
        this.active = false;
        loadImages();
    }

    private void loadImages() {
        String basePath = "textures/cutscene/" + folderName + "/";

        int imageNumber = 1;
        while (true) {
            ResourceLocation imageLocation = new ResourceLocation("storytell", basePath + imageNumber + ".png");

            try {
                var resource = Minecraft.getInstance().getResourceManager().getResource(imageLocation);
                if (resource.isPresent()) {
                    images.add(imageLocation);
                    imageAlphas.add(0.0f);
                    System.out.println("Successfully loaded cutscene image: " + imageLocation);
                    imageNumber++;
                } else {
                    break;
                }
            } catch (Exception e) {
                System.out.println("Could not load image " + imageNumber + " for cutscene: " + e.getMessage());
                break;
            }
        }

        if (images.isEmpty()) {
            System.out.println("No cutscene images found in folder: " + folderName);
            System.out.println("Expected path: assets/storytell/textures/cutscene/" + folderName + "/");
            System.out.println("Make sure images are named 1.png, 2.png, etc.");
        } else {
            System.out.println("Loaded " + images.size() + " images for cutscene: " + folderName);
        }
    }

    public void start() {
        this.active = true;
        this.currentImageIndex = 0;
        this.timer = 0;
    }

    public void stop() {
        this.active = false;
        this.currentImageIndex = -1;
    }

    public boolean isActive() {
        return active;
    }

    public void tick() {
        if (!active) return;

        timer++;

        // Обновляем анимации прозрачности
        for (int i = 0; i < images.size(); i++) {
            float currentAlpha = imageAlphas.get(i);

            if (i == currentImageIndex) {
                if (currentAlpha < 1.0f) {
                    float newAlpha = Math.min(1.0f, currentAlpha + (1.0f / FADE_DURATION));
                    imageAlphas.set(i, newAlpha);
                }
            } else if (i < currentImageIndex) {
                if (currentAlpha < 1.0f) {
                    imageAlphas.set(i, 1.0f);
                }
            } else {
                if (currentAlpha > 0.0f) {
                    imageAlphas.set(i, 0.0f);
                }
            }
        }

        if (timer >= DELAY_BETWEEN_IMAGES) {
            timer = 0;
            currentImageIndex++;

            if (currentImageIndex >= images.size()) {
                stop();
            }
        }
    }

    public void render(GuiGraphics guiGraphics) {
        if (!active || currentImageIndex < 0 || currentImageIndex >= images.size()) return;

        Minecraft minecraft = Minecraft.getInstance();
        int width = minecraft.getWindow().getGuiScaledWidth();
        int height = minecraft.getWindow().getGuiScaledHeight();

        // Рендерим полностью непрозрачный черный фон
        renderBlackBackground(guiGraphics, width, height);

        // Рендерим все изображения с их текущей прозрачностью
        for (int i = 0; i <= currentImageIndex; i++) {
            if (i < images.size()) {
                float alpha = imageAlphas.get(i);
                if (alpha > 0.0f) {
                    RenderSystem.enableBlend();
                    RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, alpha);
                    guiGraphics.blit(images.get(i), 0, 0, 0, 0, width, height, width, height);
                    RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
                    RenderSystem.disableBlend();
                }
            }
        }
    }

    private void renderBlackBackground(GuiGraphics guiGraphics, int width, int height) {
        // Используем низкоуровневый рендеринг для гарантированного отображения фона
        RenderSystem.setShader(GameRenderer::getPositionColorShader);
        BufferBuilder bufferBuilder = Tesselator.getInstance().getBuilder();
        bufferBuilder.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_COLOR);

        Matrix4f matrix = guiGraphics.pose().last().pose();
        bufferBuilder.vertex(matrix, 0, height, 0).color(0, 0, 0, 255).endVertex();
        bufferBuilder.vertex(matrix, width, height, 0).color(0, 0, 0, 255).endVertex();
        bufferBuilder.vertex(matrix, width, 0, 0).color(0, 0, 0, 255).endVertex();
        bufferBuilder.vertex(matrix, 0, 0, 0).color(0, 0, 0, 255).endVertex();

        BufferUploader.drawWithShader(bufferBuilder.end());
    }

    public boolean hasImages() {
        return !images.isEmpty();
    }
}