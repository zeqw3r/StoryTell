package com.example.storytell.init.altar.client;

import com.example.storytell.init.altar.SummoningAltarContainer;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.ObjectSelectionList;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.registries.ForgeRegistries;
import com.example.storytell.init.HologramConfig;
import com.example.storytell.init.network.NetworkHandler;
import com.example.storytell.init.altar.network.SummoningAltarSelectPacket;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

import java.util.List;

@OnlyIn(Dist.CLIENT)
public class SummoningAltarScreen extends AbstractContainerScreen<SummoningAltarContainer> {
    private EntityListWidget entityList;
    private Button selectButton;

    private static final int GUI_WIDTH = 300;
    private static final int GUI_HEIGHT = 300;
    private static final int BUTTON_WIDTH = 120;
    private static final int BUTTON_HEIGHT = 20;

    private int guiLeft;
    private int guiTop;

    public SummoningAltarScreen(SummoningAltarContainer container, Inventory playerInventory, Component title) {
        super(container, playerInventory, title);
        this.imageWidth = GUI_WIDTH;
        this.imageHeight = GUI_HEIGHT;

        this.titleLabelX = 0;
        this.titleLabelY = 10;
        this.inventoryLabelY = -1000;
    }

    @Override
    protected void init() {
        super.init();

        this.guiLeft = (this.width - GUI_WIDTH) / 2;
        this.guiTop = (this.height - GUI_HEIGHT) / 2;

        this.leftPos = guiLeft;
        this.topPos = guiTop;

        List<String> bossList = HologramConfig.getBossList();
        this.entityList = new EntityListWidget(
                this.minecraft,
                GUI_WIDTH,
                GUI_HEIGHT - 60,
                this.guiTop + 30,
                this.guiTop + GUI_HEIGHT - 40,
                20
        );
        this.entityList.setLeftPos(this.guiLeft);

        this.selectButton = Button.builder(
                Component.translatable("button.storytell.select_entity"),
                button -> this.onSelect()
        ).bounds(
                (this.width - BUTTON_WIDTH) / 2,
                this.guiTop + GUI_HEIGHT - 30,
                BUTTON_WIDTH,
                BUTTON_HEIGHT
        ).build();

        this.addRenderableWidget(entityList);
        this.addRenderableWidget(selectButton);

        this.entityList.populateEntities(bossList);
    }

    private void onSelect() {
        String selected = entityList.getSelectedEntity();
        if (selected != null) {
            NetworkHandler.INSTANCE.sendToServer(new SummoningAltarSelectPacket(this.menu.getBlockPos(), selected));
            this.onClose();
        }
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTicks) {
        this.renderBackground(guiGraphics);
        super.render(guiGraphics, mouseX, mouseY, partialTicks);

        guiGraphics.drawCenteredString(
                this.font,
                this.title,
                this.width / 2,
                this.guiTop + 10,
                0xFFFFFF
        );
    }

    @Override
    protected void renderBg(GuiGraphics guiGraphics, float partialTicks, int mouseX, int mouseY) {
        guiGraphics.fill(guiLeft, guiTop, guiLeft + GUI_WIDTH, guiTop + GUI_HEIGHT, 0xCC000000);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (keyCode == 256) {
            this.onClose();
            return true;
        } else if (keyCode == 257 && this.entityList.getSelectedEntity() != null) {
            this.onSelect();
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    @OnlyIn(Dist.CLIENT)
    class EntityListWidget extends ObjectSelectionList<EntityListWidget.Entry> {
        private List<String> entities;
        private String selectedEntity;

        public EntityListWidget(net.minecraft.client.Minecraft minecraft, int width, int height, int y0, int y1, int itemHeight) {
            super(minecraft, width, height, y0, y1, itemHeight);
        }

        public void populateEntities(List<String> bossList) {
            this.entities = bossList;
            this.selectedEntity = null;
            this.clearEntries();

            for (String entityId : entities) {
                this.addEntry(new Entry(entityId));
            }
        }

        public String getSelectedEntity() {
            return selectedEntity;
        }

        @Override
        public void setSelected(Entry entry) {
            super.setSelected(entry);
            if (entry != null) {
                this.selectedEntity = entry.entityId;
            } else {
                this.selectedEntity = null;
            }
        }

        // УДАЛЕНО: переопределение renderWidget, так как оно вызывает ошибку
        // Вместо этого используем стандартное поведение ObjectSelectionList

        protected void updateWidgetNarration(NarrationElementOutput narrationElementOutput) {
            // Базовая реализация для narration
        }

        class Entry extends ObjectSelectionList.Entry<Entry> {
            private final String entityId;

            public Entry(String entityId) {
                this.entityId = entityId;
            }

            @Override
            public void render(GuiGraphics guiGraphics, int index, int top, int left,
                               int width, int height, int mouseX, int mouseY, boolean isMouseOver,
                               float partialTicks) {
                ResourceLocation resource = new ResourceLocation(entityId);
                String displayName = resource.getNamespace() + ":" + resource.getPath();

                try {
                    var entityType = ForgeRegistries.ENTITY_TYPES.getValue(resource);
                    if (entityType != null) {
                        displayName = entityType.getDescription().getString();
                    }
                } catch (Exception e) {
                    // Используем ID если не удалось получить имя
                }

                if (displayName.length() > 30) {
                    displayName = displayName.substring(0, 27) + "...";
                }

                int textColor = entityId.equals(selectedEntity) ? 0xFF00FF00 : 0xFFFFFFFF;
                guiGraphics.drawString(
                        SummoningAltarScreen.this.font,
                        displayName,
                        left + 5,
                        top + 5,
                        textColor,
                        false
                );
            }

            @Override
            public boolean mouseClicked(double mouseX, double mouseY, int button) {
                if (button == 0) {
                    EntityListWidget.this.setSelected(this);
                    return true;
                }
                return false;
            }

            @Override
            public Component getNarration() {
                return Component.literal("Entity: " + entityId);
            }
        }
    }
}