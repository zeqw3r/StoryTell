package com.example.storytell.init.cutscene;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.ArrayList;
import java.util.List;

public class Cutscene {
    private static final Logger LOGGER = LogManager.getLogger();

    private static final int DELAY_BETWEEN_IMAGES = 100;
    private static final int FADE_DURATION = 20;

    // Кэш для предотвращения повторной загрузки текстур
    private static final List<ResourceLocation> TEXTURE_CACHE = new ArrayList<>();
    private static final List<Boolean> TEXTURE_LOADED = new ArrayList<>();

    private final String folderName;
    private final List<ResourceLocation> images;
    private final List<Float> imageAlphas;
    private int currentImageIndex;
    private int timer;
    private boolean active;
    private boolean texturesPreloaded;

    public Cutscene(String folderName) {
        this.folderName = folderName;
        this.images = new ArrayList<>();
        this.imageAlphas = new ArrayList<>();
        this.currentImageIndex = -1;
        this.timer = 0;
        this.active = false;
        this.texturesPreloaded = false;
        loadImages();
    }

    private void loadImages() {
        if (texturesPreloaded) {
            return;
        }

        String basePath = "textures/cutscene/" + folderName + "/";
        int imageNumber = 1;
        int loadedCount = 0;

        Minecraft minecraft = Minecraft.getInstance();

        while (true) {
            ResourceLocation imageLocation = new ResourceLocation("storytell", basePath + imageNumber + ".png");

            // Проверяем кэш
            int cacheIndex = TEXTURE_CACHE.indexOf(imageLocation);
            if (cacheIndex != -1 && TEXTURE_LOADED.get(cacheIndex)) {
                images.add(imageLocation);
                imageAlphas.add(0.0f);
                loadedCount++;
                imageNumber++;
                continue;
            }

            try {
                var resource = minecraft.getResourceManager().getResource(imageLocation);
                if (resource.isPresent()) {
                    images.add(imageLocation);
                    imageAlphas.add(0.0f);

                    // Добавляем в кэш
                    if (cacheIndex == -1) {
                        TEXTURE_CACHE.add(imageLocation);
                        TEXTURE_LOADED.add(true);
                    }

                    loadedCount++;
                    imageNumber++;
                } else {
                    break;
                }
            } catch (Exception e) {
                LOGGER.debug("Could not load image {} for cutscene: {}", imageNumber, e.getMessage());
                break;
            }
        }

        if (images.isEmpty()) {
            LOGGER.warn("No cutscene images found in folder: {}", folderName);
            LOGGER.warn("Expected path: assets/storytell/textures/cutscene/{}/", folderName);
        } else {
            LOGGER.debug("Loaded {} images for cutscene: {}", loadedCount, folderName);
        }

        texturesPreloaded = true;
    }

    public void start() {
        this.active = true;
        this.currentImageIndex = 0;
        this.timer = 0;
    }

    public void stop() {
        this.active = false;
        this.currentImageIndex = -1;
        // Не очищаем images и imageAlphas для возможного повторного использования
    }

    public void cleanup() {
        stop();
        images.clear();
        imageAlphas.clear();
        texturesPreloaded = false;
    }

    public boolean isActive() {
        return active;
    }

    public void tick() {
        if (!active) return;

        timer++;

        // Оптимизированное обновление прозрачности - только для нужных изображений
        updateImageAlphas();

        if (timer >= DELAY_BETWEEN_IMAGES) {
            timer = 0;
            currentImageIndex++;

            if (currentImageIndex >= images.size()) {
                stop();
            }
        }
    }

    private void updateImageAlphas() {
        // Обновляем только текущее и предыдущее изображение для экономии CPU
        for (int i = Math.max(0, currentImageIndex - 1); i <= Math.min(images.size() - 1, currentImageIndex + 1); i++) {
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
    }

    public void render(GuiGraphics guiGraphics) {
        if (!active || currentImageIndex < 0 || currentImageIndex >= images.size()) return;

        Minecraft minecraft = Minecraft.getInstance();
        int width = minecraft.getWindow().getGuiScaledWidth();
        int height = minecraft.getWindow().getGuiScaledHeight();

        // Оптимизированный рендеринг черного фона
        renderBlackBackground(guiGraphics, width, height);

        // Рендерим только видимые изображения (с alpha > 0)
        renderVisibleImages(guiGraphics, width, height);
    }

    private void renderBlackBackground(GuiGraphics guiGraphics, int width, int height) {
        // Используем встроенный метод вместо низкоуровневого рендеринга
        guiGraphics.fill(0, 0, width, height, 0xFF000000);
    }

    private void renderVisibleImages(GuiGraphics guiGraphics, int width, int height) {
        RenderSystem.enableBlend();

        // Рендерим ВСЕ изображения, которые должны быть видимы
        for (int i = 0; i <= currentImageIndex; i++) {
            float alpha = imageAlphas.get(i);
            if (alpha > 0.001f) { // Не рендерим почти невидимые изображения
                RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, alpha);
                guiGraphics.blit(images.get(i), 0, 0, 0, 0, width, height, width, height);
            }
        }

        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
        RenderSystem.disableBlend();
    }

    public boolean hasImages() {
        return !images.isEmpty();
    }

    // Статический метод для очистки глобального кэша
    public static void clearTextureCache() {
        TEXTURE_CACHE.clear();
        TEXTURE_LOADED.clear();
    }
}