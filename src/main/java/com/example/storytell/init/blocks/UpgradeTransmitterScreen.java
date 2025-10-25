// UpgradeTransmitterScreen.java
package com.example.storytell.init.blocks;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.core.BlockPos;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class UpgradeTransmitterScreen extends Screen {
    private static final ResourceLocation GUI_TEXTURE =
            new ResourceLocation("storytell", "textures/gui/upgrade_transmitter.png");
    private static final ResourceLocation TEXT_FIELD_TEXTURE =
            new ResourceLocation("storytell", "textures/gui/text_field.png");
    private static final ResourceLocation BUTTON_TEXTURE =
            new ResourceLocation("storytell", "textures/gui/button.png");
    private static final ResourceLocation BUTTON_HOVERED_TEXTURE =
            new ResourceLocation("storytell", "textures/gui/button_hovered.png");

    private static final int GUI_WIDTH = 176;
    private static final int GUI_HEIGHT = 166;
    private static final int TEXT_FIELD_WIDTH = 116;
    private static final int TEXT_FIELD_HEIGHT = 20;
    private static final int BUTTON_WIDTH = 76;
    private static final int BUTTON_HEIGHT = 20;

    private CustomTextField textField;
    private CustomImageButton sendButton;
    private final BlockPos blockPos;

    private int guiLeft;
    private int guiTop;

    public UpgradeTransmitterScreen(BlockPos pos) {
        super(Component.literal("")); // Пустой заголовок
        this.blockPos = pos;
    }

    @Override
    protected void init() {
        super.init();

        this.guiLeft = (this.width - GUI_WIDTH) / 2;
        this.guiTop = (this.height - GUI_HEIGHT) / 2;

        // Кастомное поле ввода с правильным смещением текста
        this.textField = new CustomTextField(
                this.font,
                guiLeft + 30, guiTop + 60,
                TEXT_FIELD_WIDTH, TEXT_FIELD_HEIGHT,
                Component.literal("")
        );
        this.textField.setMaxLength(100);
        this.textField.setValue("");
        this.textField.setBordered(false);
        this.textField.setTextColor(0xFFFFFF);
        this.addWidget(this.textField);
        this.setInitialFocus(this.textField);

        // Кастомная кнопка без сжатия при наведении
        this.sendButton = new CustomImageButton(
                guiLeft + 50, guiTop + 90,
                BUTTON_WIDTH, BUTTON_HEIGHT,
                button -> this.sendCommand()
        );
        this.addRenderableWidget(this.sendButton);
    }

    private void sendCommand() {
        String message = this.textField.getValue().trim();

        if (!message.isEmpty()) {
            // Отправляем пакет на сервер
            com.example.storytell.init.network.NetworkHandler.INSTANCE.sendToServer(
                    new UpgradeTransmitterCommandPacket(this.blockPos, message)
            );
        }

        // Закрываем GUI после отправки
        this.onClose();
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTicks) {
        this.renderBackground(guiGraphics);

        // Рендерим кастомную текстуру GUI
        guiGraphics.blit(GUI_TEXTURE, guiLeft, guiTop, 0, 0, GUI_WIDTH, GUI_HEIGHT, GUI_WIDTH, GUI_HEIGHT);

        // Рендерим кастомную текстуру для поля ввода
        guiGraphics.blit(TEXT_FIELD_TEXTURE,
                guiLeft + 30, guiTop + 60,
                0, 0, TEXT_FIELD_WIDTH, TEXT_FIELD_HEIGHT,
                TEXT_FIELD_WIDTH, TEXT_FIELD_HEIGHT);

        // Рендерим текст поля ввода поверх кастомной текстуры
        if (this.textField != null) {
            this.textField.render(guiGraphics, mouseX, mouseY, partialTicks);
        }

        // Кнопка рендерится сама через CustomImageButton
        super.render(guiGraphics, mouseX, mouseY, partialTicks);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (keyCode == 256) { // ESC
            this.onClose();
            return true;
        } else if (keyCode == 257) { // Enter
            this.sendCommand();
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    // Кастомное поле ввода со смещением текста
    private static class CustomTextField extends EditBox {
        public CustomTextField(net.minecraft.client.gui.Font font, int x, int y, int width, int height, Component message) {
            super(font, x, y, width, height, message);
        }

        @Override
        public void renderWidget(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTicks) {
            // Смещаем текст на 5% вниз и влево
            int textOffsetX = (int) (this.width * 0.05);
            int textOffsetY = (int) (this.height * 0.3);

            // Сохраняем оригинальные позиции
            int originalX = this.getX();
            int originalY = this.getY();

            // Временно смещаем позицию для рендеринга текста
            this.setX(originalX + textOffsetX);
            this.setY(originalY + textOffsetY);

            // Рендерим текст со смещением
            super.renderWidget(guiGraphics, mouseX, mouseY, partialTicks);

            // Восстанавливаем оригинальные позиции
            this.setX(originalX);
            this.setY(originalY);
        }
    }

    // Кастомная кнопка без сжатия при наведении
    private static class CustomImageButton extends Button {
        private boolean isHovered;

        public CustomImageButton(int x, int y, int width, int height, OnPress onPress) {
            super(x, y, width, height, Component.literal(""), onPress, DEFAULT_NARRATION);
        }

        @Override
        public void renderWidget(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTicks) {
            this.isHovered = this.isMouseOver(mouseX, mouseY);

            ResourceLocation texture = this.isHovered ? BUTTON_HOVERED_TEXTURE : BUTTON_TEXTURE;

            // Рендерим текстуру кнопки без изменения размеров
            guiGraphics.blit(texture, this.getX(), this.getY(), 0, 0, this.width, this.height, this.width, this.height);
        }

        @Override
        public void onPress() {
            super.onPress();
        }

        @Override
        public boolean isMouseOver(double mouseX, double mouseY) {
            return this.active && this.visible &&
                    mouseX >= (double)this.getX() &&
                    mouseY >= (double)this.getY() &&
                    mouseX < (double)(this.getX() + this.width) &&
                    mouseY < (double)(this.getY() + this.height);
        }
    }
}