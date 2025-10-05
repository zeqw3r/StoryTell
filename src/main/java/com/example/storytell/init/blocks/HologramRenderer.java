// HologramRenderer.java
package com.example.storytell.init.blocks;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import com.mojang.math.Axis;

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

        VertexConsumer vertexConsumer = bufferSource.getBuffer(RenderType.entityCutoutNoCull(textureLocation));
        PoseStack.Pose pose = poseStack.last();

        float halfWidth = width / 2.0f;
        float halfHeight = height / 2.0f;

        int alpha = (int)(255 * animationProgress);

        vertexConsumer.vertex(pose.pose(), -halfWidth, -halfHeight, 0.0F)
                .color(255, 255, 255, alpha)
                .uv(0.0F, 1.0F)
                .overlayCoords(OverlayTexture.NO_OVERLAY)
                .uv2(packedLight)
                .normal(pose.normal(), 0.0F, 1.0F, 0.0F)
                .endVertex();

        vertexConsumer.vertex(pose.pose(), halfWidth, -halfHeight, 0.0F)
                .color(255, 255, 255, alpha)
                .uv(1.0F, 1.0F)
                .overlayCoords(OverlayTexture.NO_OVERLAY)
                .uv2(packedLight)
                .normal(pose.normal(), 0.0F, 1.0F, 0.0F)
                .endVertex();

        vertexConsumer.vertex(pose.pose(), halfWidth, halfHeight, 0.0F)
                .color(255, 255, 255, alpha)
                .uv(1.0F, 0.0F)
                .overlayCoords(OverlayTexture.NO_OVERLAY)
                .uv2(packedLight)
                .normal(pose.normal(), 0.0F, 1.0F, 0.0F)
                .endVertex();

        vertexConsumer.vertex(pose.pose(), -halfWidth, halfHeight, 0.0F)
                .color(255, 255, 255, alpha)
                .uv(0.0F, 0.0F)
                .overlayCoords(OverlayTexture.NO_OVERLAY)
                .uv2(packedLight)
                .normal(pose.normal(), 0.0F, 1.0F, 0.0F)
                .endVertex();

        poseStack.popPose();
    }

    @Override
    public ResourceLocation getTextureLocation(HologramEntity entity) {
        return entity.getTexture();
    }
}