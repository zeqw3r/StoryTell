// TabletScreen.java
package com.example.storytell.init.tablet;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

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
            // Прямое создание ResourceLocation из строки
            this.imageTexture = new ResourceLocation(imagePath);

            // Проверяем существование текстуры
            if (!checkTextureExists(imageTexture)) {
                // Если текстура не найдена, используем стандартную
                this.imageTexture = new ResourceLocation("storytell:textures/gui/tablet/default.png");
            }
        } catch (Exception e) {
            // В случае ошибки используем стандартную текстуру
            this.imageTexture = new ResourceLocation("storytell:textures/gui/tablet/default.png");
        }
    }

    private boolean checkTextureExists(ResourceLocation texture) {
        try {
            // Пытаемся загрузить текстуру для проверки её существования
            minecraft.getTextureManager().getTexture(texture);
            return true;
        } catch (Exception e) {
            return false;
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
            // Базовые размеры изображения увеличены на 10%
            // Было: 384×256 пикселей
            // Стало: 422×282 пикселей (увеличение на 10%)
            int baseWidth = 1920;
            int baseHeight = 1080;

            // Ограничиваем максимальный размер экраном
            int imageWidth = Math.min(baseWidth, this.width - 20);
            int imageHeight = Math.min(baseHeight, this.height - 20);

            // Центрируем изображение
            int x = (this.width - imageWidth) / 2;
            int y = (this.height - imageHeight) / 2;

            // Рендерим изображение
            guiGraphics.blit(imageTexture, x, y, 0, 0, imageWidth, imageHeight, imageWidth, imageHeight);
        } catch (Exception e) {
            // В случае ошибки рендеринга, просто не отображаем изображение
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