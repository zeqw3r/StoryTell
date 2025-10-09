// CustomStar.java
package com.example.storytell.init.star;

import com.mojang.blaze3d.vertex.*;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;
import org.joml.Matrix4f;
import org.joml.Quaternionf;

import java.util.List;

public class CustomStar {
    private final String name;
    private final ResourceLocation modelLocation;
    private final float size;
    private final int color;

    // Celestial coordinates
    private final float rightAscension;
    private final float declination;
    private final float distance;

    // Position fields
    private float x, y, z;

    // Animation properties
    private float rotationAngle = 0.0f;
    private final float rotationSpeed;
    private final float pulseSpeed;
    private final float pulseAmount;

    public CustomStar(String name, String modelPath, float size, int color,
                      float rightAscension, float declination, float distance,
                      float rotationSpeed, float pulseSpeed, float pulseAmount) {
        this.name = name;
        this.modelLocation = new ResourceLocation("storytell", modelPath);
        this.size = size;
        this.color = color;
        this.rightAscension = rightAscension;
        this.declination = declination;
        this.distance = distance;
        this.rotationSpeed = rotationSpeed;
        this.pulseSpeed = pulseSpeed;
        this.pulseAmount = pulseAmount;

        calculatePosition();
    }

    public void render(com.mojang.blaze3d.vertex.PoseStack poseStack, float partialTick) {
        // Update animation
        updateAnimation(partialTick);

        // Setup rendering
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.depthMask(false);

        poseStack.pushPose();

        // Apply transformations
        poseStack.translate(x, y, z);

        // Individual star rotation
        poseStack.mulPose(new Quaternionf().rotationZ((float) Math.toRadians(rotationAngle)));

        // Calculate pulse effect
        float pulse = 1.0f + (float)Math.sin(System.currentTimeMillis() * 0.001f * pulseSpeed) * pulseAmount;
        float currentSize = size * pulse;

        // Apply color tint
        float r = ((color >> 16) & 0xFF) / 255.0f;
        float g = ((color >> 8) & 0xFF) / 255.0f;
        float b = (color & 0xFF) / 255.0f;
        float a = ((color >> 24) & 0xFF) / 255.0f;

        // Try to render the model, fallback to quad if needed
        if (!renderJsonModel(poseStack, currentSize, r, g, b, a)) {
            renderFallbackQuad(poseStack, currentSize, r, g, b, a);
        }

        poseStack.popPose();

        // Reset color and cleanup
        RenderSystem.setShaderColor(1.0f, 1.0f, 1.0f, 1.0f);
        RenderSystem.depthMask(true);
        RenderSystem.disableBlend();
    }

    private void calculatePosition() {
        double raRad = Math.toRadians(rightAscension);
        double decRad = Math.toRadians(declination);

        this.x = (float) (distance * Math.cos(decRad) * Math.cos(raRad));
        this.y = (float) (distance * Math.sin(decRad));
        this.z = (float) (distance * Math.cos(decRad) * Math.sin(raRad));
    }

    private void updateAnimation(float partialTick) {
        rotationAngle += rotationSpeed * partialTick;
        if (rotationAngle >= 360.0f) {
            rotationAngle -= 360.0f;
        }
    }

    private boolean renderJsonModel(com.mojang.blaze3d.vertex.PoseStack poseStack, float currentSize,
                                    float r, float g, float b, float a) {
        try {
            BakedModel model = Minecraft.getInstance().getModelManager().getModel(modelLocation);

            if (model == null || model == Minecraft.getInstance().getModelManager().getMissingModel()) {
                System.out.println("Model not found: " + modelLocation);
                return false;
            }

            // Setup rendering - use block atlas
            RenderSystem.setShader(GameRenderer::getPositionTexShader);
            RenderSystem.setShaderTexture(0, TextureAtlas.LOCATION_BLOCKS);

            // Apply scaling
            poseStack.pushPose();
            float scale = currentSize / 16.0f;
            poseStack.scale(scale, scale, scale);
            poseStack.translate(-0.5f, -0.5f, -0.5f);

            // Render the model
            Tesselator tesselator = Tesselator.getInstance();
            BufferBuilder bufferBuilder = tesselator.getBuilder();
            bufferBuilder.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_TEX_COLOR);

            RandomSource random = RandomSource.create(42L);

            // Get and render all quads
            List<BakedQuad> quads = model.getQuads(null, null, random);
            for (BakedQuad quad : quads) {
                int[] vertices = quad.getVertices();
                for (int i = 0; i < 4; i++) {
                    float x = Float.intBitsToFloat(vertices[i * 8]);
                    float y = Float.intBitsToFloat(vertices[i * 8 + 1]);
                    float z = Float.intBitsToFloat(vertices[i * 8 + 2]);
                    float u = Float.intBitsToFloat(vertices[i * 8 + 4]);
                    float v = Float.intBitsToFloat(vertices[i * 8 + 5]);

                    bufferBuilder.vertex(poseStack.last().pose(), x, y, z)
                            .uv(u, v)
                            .color(r, g, b, a)
                            .endVertex();
                }
            }

            BufferUploader.drawWithShader(bufferBuilder.end());
            poseStack.popPose();
            return true;

        } catch (Exception e) {
            System.err.println("Error rendering model " + modelLocation + ": " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }

    private void renderFallbackQuad(com.mojang.blaze3d.vertex.PoseStack poseStack, float currentSize,
                                    float r, float g, float b, float a) {
        Tesselator tesselator = Tesselator.getInstance();
        BufferBuilder bufferBuilder = tesselator.getBuilder();

        bufferBuilder.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_TEX_COLOR);

        Matrix4f matrix = poseStack.last().pose();
        float halfSize = currentSize / 2.0f;

        // Build simple quad
        bufferBuilder.vertex(matrix, -halfSize, -halfSize, 0).uv(0.0f, 0.0f).color(r, g, b, a).endVertex();
        bufferBuilder.vertex(matrix, -halfSize, halfSize, 0).uv(0.0f, 1.0f).color(r, g, b, a).endVertex();
        bufferBuilder.vertex(matrix, halfSize, halfSize, 0).uv(1.0f, 1.0f).color(r, g, b, a).endVertex();
        bufferBuilder.vertex(matrix, halfSize, -halfSize, 0).uv(1.0f, 0.0f).color(r, g, b, a).endVertex();

        BufferUploader.drawWithShader(bufferBuilder.end());
    }

    // Getters
    public String getName() { return name; }
    public ResourceLocation getModelLocation() { return modelLocation; }
    public float getX() { return x; }
    public float getY() { return y; }
    public float getZ() { return z; }
    public float getRightAscension() { return rightAscension; }
    public float getDeclination() { return declination; }
    public float getDistance() { return distance; }
}