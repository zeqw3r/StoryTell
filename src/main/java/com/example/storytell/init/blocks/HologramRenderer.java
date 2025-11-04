// HologramRenderer.java
package com.example.storytell.init.blocks;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import com.mojang.math.Axis;
import net.minecraft.util.FormattedCharSequence;

import java.util.List;

public class HologramRenderer extends EntityRenderer<HologramEntity> {

    // Кэшированные константы для оптимизации
    private static final float WIDTH = 3.0f;
    private static final float HEIGHT = 2.0f;
    private static final float HALF_WIDTH = WIDTH / 2.0f;
    private static final float HALF_HEIGHT = HEIGHT / 2.0f;
    private static final float TEXT_SCALE = 0.023F;
    private static final int TEXT_COLOR = 0x88CCFF;

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

        poseStack.pushPose();

        poseStack.translate(0.0D, -verticalOffset, 0.0D);

        float scale = animationProgress;
        poseStack.scale(scale, scale, scale);

        poseStack.translate(0.0D, 1.0D, 0.0D);
        poseStack.mulPose(Minecraft.getInstance().getEntityRenderDispatcher().cameraOrientation());
        poseStack.mulPose(Axis.YP.rotationDegrees(180.0F));

        // Рендерим текстуру голограммы
        renderTexture(poseStack, bufferSource, textureLocation, packedLight, animationProgress);

        // Рендерим текст, если он есть
        String text = entity.getDisplayText();
        if (text != null && !text.isEmpty()) {
            renderText(poseStack, bufferSource, text, packedLight);
        }

        poseStack.popPose();
    }

    private void renderTexture(PoseStack poseStack, MultiBufferSource bufferSource,
                               ResourceLocation textureLocation, int packedLight, float animationProgress) {
        // Используем полупрозрачный RenderType с поддержкой альфа-канала
        VertexConsumer vertexConsumer = bufferSource.getBuffer(RenderType.entityTranslucent(textureLocation));
        PoseStack.Pose pose = poseStack.last();

        // Устанавливаем полупрозрачность (альфа-канал)
        int alpha = (int)(255 * animationProgress);

        // Добавляем голубоватый оттенок для эффекта голограммы
        int r = (int)(255 * 0.9f);
        int g = 255;
        int b = Math.min(255, (int)(255 * 1.1f));

        // Рендерим квадрат - оптимизированная версия
        renderQuad(vertexConsumer, pose, r, g, b, alpha, packedLight);
    }

    private void renderQuad(VertexConsumer consumer, PoseStack.Pose pose, int r, int g, int b, int alpha, int light) {
        consumer.vertex(pose.pose(), -HALF_WIDTH, -HALF_HEIGHT, 0.0F)
                .color(r, g, b, alpha)
                .uv(0.0F, 1.0F)
                .overlayCoords(OverlayTexture.NO_OVERLAY)
                .uv2(light)
                .normal(pose.normal(), 0.0F, 1.0F, 0.0F)
                .endVertex();

        consumer.vertex(pose.pose(), HALF_WIDTH, -HALF_HEIGHT, 0.0F)
                .color(r, g, b, alpha)
                .uv(1.0F, 1.0F)
                .overlayCoords(OverlayTexture.NO_OVERLAY)
                .uv2(light)
                .normal(pose.normal(), 0.0F, 1.0F, 0.0F)
                .endVertex();

        consumer.vertex(pose.pose(), HALF_WIDTH, HALF_HEIGHT, 0.0F)
                .color(r, g, b, alpha)
                .uv(1.0F, 0.0F)
                .overlayCoords(OverlayTexture.NO_OVERLAY)
                .uv2(light)
                .normal(pose.normal(), 0.0F, 1.0F, 0.0F)
                .endVertex();

        consumer.vertex(pose.pose(), -HALF_WIDTH, HALF_HEIGHT, 0.0F)
                .color(r, g, b, alpha)
                .uv(0.0F, 0.0F)
                .overlayCoords(OverlayTexture.NO_OVERLAY)
                .uv2(light)
                .normal(pose.normal(), 0.0F, 1.0F, 0.0F)
                .endVertex();
    }

    private void renderText(PoseStack poseStack, MultiBufferSource bufferSource, String text, int packedLight) {
        Minecraft mc = Minecraft.getInstance();
        Font font = mc.font;

        poseStack.pushPose();

        poseStack.translate(0.0F, 0.0F, 0.01F);
        poseStack.scale(-TEXT_SCALE, -TEXT_SCALE, TEXT_SCALE);
        poseStack.mulPose(Axis.YP.rotationDegrees(180.0F));

        Component comp = Component.literal(text);
        List<FormattedCharSequence> lines = font.split(comp, 100);
        float y = (float) (font.lineHeight * lines.size()) / -2;

        for (FormattedCharSequence line : lines) {
            float textWidth = font.width(line) / 2.0F;

            // Рисуем текст
            font.drawInBatch(
                    line,
                    -textWidth,
                    y,
                    TEXT_COLOR,
                    false,
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

    @Override
    public ResourceLocation getTextureLocation(HologramEntity entity) {
        return entity.getTexture();
    }
}