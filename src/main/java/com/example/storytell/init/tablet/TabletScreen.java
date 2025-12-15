package com.example.storytell.init.tablet;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class TabletScreen extends Screen {
    private final String imagePath;
    private ResourceLocation imageTexture;

    public TabletScreen(String imagePath) {
        super(Component.literal("News Tablet"));
        this.imagePath = imagePath;
    }

    @Override
    protected void init() {
        super.init();
        loadImageTexture();
    }

    private void loadImageTexture() {
        try {
            // Безопасное создание ResourceLocation
            this.imageTexture = new ResourceLocation(imagePath);

            // Проверяем существование текстуры с обработкой ошибок
            if (minecraft != null && minecraft.getResourceManager() != null) {
                try {
                    var resource = minecraft.getResourceManager().getResource(imageTexture);
                    if (resource.isEmpty()) {
                        throw new RuntimeException("Resource not found");
                    }
                } catch (Exception e) {
                    this.imageTexture = new ResourceLocation("storytell:textures/gui/tablet/default.png");
                }
            }
        } catch (Exception e) {

            this.imageTexture = new ResourceLocation("storytell:textures/gui/tablet/default.png");
        }
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTicks) {
        // Рендерим темный полупрозрачный фон
        renderBackground(guiGraphics);

        // Рендерим изображение планшета
        renderTabletImage(guiGraphics);

        super.render(guiGraphics, mouseX, mouseY, partialTicks);
    }

    @Override
    public void renderBackground(GuiGraphics guiGraphics) {
        // Темный полупрозрачный фон
        guiGraphics.fillGradient(0, 0, this.width, this.height, 0xC0101010, 0xD0101010);
    }

    private void renderTabletImage(GuiGraphics guiGraphics) {
        if (imageTexture == null) return;

        try {
            // Базовые размеры изображения
            int baseWidth = 1920;
            int baseHeight = 1080;

            // Ограничиваем максимальный размер экраном
            int imageWidth = Math.min(baseWidth, this.width - 20);
            int imageHeight = Math.min(baseHeight, this.height - 20);

            // Сохраняем пропорции
            float aspectRatio = (float) baseWidth / baseHeight;
            if ((float) imageWidth / imageHeight > aspectRatio) {
                imageWidth = (int) (imageHeight * aspectRatio);
            } else {
                imageHeight = (int) (imageWidth / aspectRatio);
            }

            // Центрируем изображение
            int x = (this.width - imageWidth) / 2;
            int y = (this.height - imageHeight) / 2;

            // Рендерим изображение
            guiGraphics.blit(imageTexture, x, y, 0, 0, imageWidth, imageHeight, imageWidth, imageHeight);
        } catch (Exception e) {
        }
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        // Закрываем по ESC, E или кнопке инвентаря
        if (keyCode == 256 || // ESC
                keyCode == 69 || // E
                (minecraft != null && minecraft.options.keyInventory.matches(keyCode, scanCode))) {
            this.onClose();
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        // Закрываем по любому клику, как книга
        this.onClose();
        return true;
    }
}