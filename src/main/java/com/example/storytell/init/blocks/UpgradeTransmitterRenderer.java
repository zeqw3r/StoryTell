package com.example.storytell.init.blocks;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class UpgradeTransmitterRenderer implements BlockEntityRenderer<UpgradeTransmitterBlockEntity> {

    public UpgradeTransmitterRenderer(BlockEntityRendererProvider.Context context) {}

    @Override
    public void render(UpgradeTransmitterBlockEntity be, float partialTicks, PoseStack poseStack,
                       MultiBufferSource buffer, int combinedLight, int combinedOverlay) {
        if (!be.isShowingHologram()) return;

        poseStack.pushPose();
        poseStack.translate(0.5, 1.2, 0.5);

        VertexConsumer builder = buffer.getBuffer(RenderType.LINES);

        float size = 0.3f;
        int r = 255;
        int g = 255;
        int b = 255;
        int alpha = 255;

        addLine(builder, poseStack, -size, 0, -size, size, 0, -size, r, g, b, alpha);
        addLine(builder, poseStack, size, 0, -size, size, 0, size, r, g, b, alpha);
        addLine(builder, poseStack, size, 0, size, -size, 0, size, r, g, b, alpha);
        addLine(builder, poseStack, -size, 0, size, -size, 0, -size, r, g, b, alpha);

        addLine(builder, poseStack, -size, size, -size, size, size, -size, r, g, b, alpha);
        addLine(builder, poseStack, size, size, -size, size, size, size, r, g, b, alpha);
        addLine(builder, poseStack, size, size, size, -size, size, size, r, g, b, alpha);
        addLine(builder, poseStack, -size, size, size, -size, size, -size, r, g, b, alpha);

        addLine(builder, poseStack, -size, 0, -size, -size, size, -size, r, g, b, alpha);
        addLine(builder, poseStack, size, 0, -size, size, size, -size, r, g, b, alpha);
        addLine(builder, poseStack, size, 0, size, size, size, size, r, g, b, alpha);
        addLine(builder, poseStack, -size, 0, size, -size, size, size, r, g, b, alpha);

        poseStack.popPose();
    }

    private void addLine(VertexConsumer builder, PoseStack poseStack,
                         float x1, float y1, float z1,
                         float x2, float y2, float z2,
                         int r, int g, int b, int alpha) {
        builder.vertex(poseStack.last().pose(), x1, y1, z1)
                .color(r, g, b, alpha)
                .normal(0, 1, 0)
                .endVertex();
        builder.vertex(poseStack.last().pose(), x2, y2, z2)
                .color(r, g, b, alpha)
                .normal(0, 1, 0)
                .endVertex();
    }

    @Override
    public boolean shouldRenderOffScreen(UpgradeTransmitterBlockEntity be) {
        return false;
    }
}