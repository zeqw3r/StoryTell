// HologramRenderer.java
package com.example.storytell.init.blocks;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.font.FontManager;
import net.minecraft.client.gui.font.FontSet;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import com.mojang.math.Axis;
import net.minecraft.util.FormattedCharSequence;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class HologramRenderer extends EntityRenderer<HologramEntity> {

    public HologramRenderer(EntityRendererProvider.Context context) {
        super(context);
    }

    @Override
    public void render(HologramEntity entity, float entityYaw, float partialTicks, PoseStack poseStack,
                       MultiBufferSource bufferSource, int packedLight) {
        float animationProgress = entity.getAnimationProgress();
        float verticalOffset = entity.getVerticalOffset();

        if (animationProgress <= 0.0F) {
            return;
        }

        ResourceLocation textureLocation = getTextureLocation(entity);

        float width = 3.0f;
        float height = 2.0f;

        poseStack.pushPose();

        poseStack.translate(0.0D, -verticalOffset, 0.0D);

        float scale = animationProgress;
        poseStack.scale(scale, scale, scale);

        poseStack.translate(0.0D, 1.0D, 0.0D);
        poseStack.mulPose(Minecraft.getInstance().getEntityRenderDispatcher().cameraOrientation());
        poseStack.mulPose(Axis.YP.rotationDegrees(180.0F));

        // Используем полупрозрачный RenderType с поддержкой альфа-канала
        VertexConsumer vertexConsumer = bufferSource.getBuffer(RenderType.entityTranslucent(textureLocation));
        PoseStack.Pose pose = poseStack.last();

        float halfWidth = width / 2.0f;
        float halfHeight = height / 2.0f;

        // Устанавливаем полупрозрачность (альфа-канал)
        int alpha = (int)(255 * animationProgress); // Максимум 200 из 255 для полупрозрачности
        int baseColor = 0xFFFFFF; // Белый цвет для минимального искажения текстуры

        // Добавляем голубоватый оттенок для эффекта голограммы
        int r = (baseColor >> 16) & 0xFF;
        int g = (baseColor >> 8) & 0xFF;
        int b = baseColor & 0xFF;

        // Слегка смещаем цвет в сторону голубого
        r = Math.min(255, (int)(r * 0.9f));
        g = Math.min(255, (int)(g * 1.0f));
        b = Math.min(255, (int)(b * 1.1f));

        vertexConsumer.vertex(pose.pose(), -halfWidth, -halfHeight, 0.0F)
                .color(r, g, b, alpha)
                .uv(0.0F, 1.0F)
                .overlayCoords(OverlayTexture.NO_OVERLAY)
                .uv2(packedLight)
                .normal(pose.normal(), 0.0F, 1.0F, 0.0F)
                .endVertex();

        vertexConsumer.vertex(pose.pose(), halfWidth, -halfHeight, 0.0F)
                .color(r, g, b, alpha)
                .uv(1.0F, 1.0F)
                .overlayCoords(OverlayTexture.NO_OVERLAY)
                .uv2(packedLight)
                .normal(pose.normal(), 0.0F, 1.0F, 0.0F)
                .endVertex();

        vertexConsumer.vertex(pose.pose(), halfWidth, halfHeight, 0.0F)
                .color(r, g, b, alpha)
                .uv(1.0F, 0.0F)
                .overlayCoords(OverlayTexture.NO_OVERLAY)
                .uv2(packedLight)
                .normal(pose.normal(), 0.0F, 1.0F, 0.0F)
                .endVertex();

        vertexConsumer.vertex(pose.pose(), -halfWidth, halfHeight, 0.0F)
                .color(r, g, b, alpha)
                .uv(0.0F, 0.0F)
                .overlayCoords(OverlayTexture.NO_OVERLAY)
                .uv2(packedLight)
                .normal(pose.normal(), 0.0F, 1.0F, 0.0F)
                .endVertex();

        Minecraft mc = Minecraft.getInstance();
        FontManager manager = new FontManager(mc.getTextureManager());
        ResourceLocation fontLoc = new ResourceLocation("storytell", "sftransrobotics_oblique");
        Map<ResourceLocation, ResourceLocation> renames = new HashMap<>();
        renames.put(
                new ResourceLocation("minecraft", "default"), fontLoc
        );
        manager.setRenames(renames);
        Font font = manager.createFont();
        String text = entity.getDisplayText();
        if (text != null && !text.isEmpty()) {
            poseStack.pushPose();

            poseStack.translate(0.0F, 0.0F, 0.01F);
            float textScale = 0.023F;
            poseStack.scale(-textScale, -textScale, textScale);
            poseStack.mulPose(Axis.YP.rotationDegrees(180.0F));

            Component comp = Component.literal(text);
            List<FormattedCharSequence> lines = font.split(comp, 100); // 100 пикселей ширина
            float y = (float) (font.lineHeight * lines.toArray().length) / -2;
            for (FormattedCharSequence line : lines) {
                float textWidth = font.width(line) / 2.0F;

                // Рисуем
                font.drawInBatch(
                        line,
                        -textWidth,
                        y,
                        0xFFFFFF, // голубоватый цвет под голограмму
                        false,     // без тени
                        poseStack.last().pose(),
                        bufferSource,
                        Font.DisplayMode.NORMAL,
                        0,
                        packedLight
                );
                y += font.lineHeight;
            }


            poseStack.popPose();
        }

        poseStack.popPose();
    }

    @Override
    public ResourceLocation getTextureLocation(HologramEntity entity) {
        return entity.getTexture();
    }
}