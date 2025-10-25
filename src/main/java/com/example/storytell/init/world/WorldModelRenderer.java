// WorldModelRenderer.java - новый класс для правильного рендеринга на любых расстояниях
package com.example.storytell.init.world;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.*;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;

public class WorldModelRenderer {

    public static void setupRenderState() {
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.depthMask(false);
        // Отключаем culling для видимости со всех сторон
        RenderSystem.disableCull();
    }

    public static void cleanupRenderState() {
        RenderSystem.enableCull();
        RenderSystem.depthMask(true);
        RenderSystem.disableBlend();
        RenderSystem.setShaderColor(1.0f, 1.0f, 1.0f, 1.0f);
    }

    public static void renderWorldModel(WorldModel model, PoseStack poseStack, Camera camera, float partialTick) {
        Vec3 cameraPos = camera.getPosition();
        double camX = cameraPos.x;
        double camY = cameraPos.y;
        double camZ = cameraPos.z;

        poseStack.pushPose();

        // Правильное преобразование мировых координат
        double relX = model.getX() - camX;
        double relY = model.getY() - camY;
        double relZ = model.getZ() - camZ;

        // Перемещаем в мировую позицию
        poseStack.translate(relX, relY, relZ);

        // Масштабируем модель
        float scale = model.getSize() / 16.0f;
        poseStack.scale(scale, scale, scale);
        poseStack.translate(-0.5f, -0.5f, -0.5f);

        // Рендерим модель
        renderModel(poseStack, model);

        poseStack.popPose();
    }

    private static void renderModel(PoseStack poseStack, WorldModel model) {
        try {
            // Пробуем рендерить JSON модель
            if (renderJsonModel(poseStack, model)) {
                return;
            }

            // Fallback: рендерим простой куб
            renderFallbackCube(poseStack, model);

        } catch (Exception e) {
            // В случае ошибки рендерим fallback
            renderFallbackCube(poseStack, model);
        }
    }

    private static boolean renderJsonModel(PoseStack poseStack, WorldModel model) {
        try {
            var bakedModel = Minecraft.getInstance().getModelManager().getModel(model.getModelLocation());

            if (bakedModel == null || bakedModel == Minecraft.getInstance().getModelManager().getMissingModel()) {
                return false;
            }

            RenderSystem.setShader(GameRenderer::getPositionTexShader);
            RenderSystem.setShaderTexture(0, net.minecraft.client.renderer.texture.TextureAtlas.LOCATION_BLOCKS);

            Tesselator tesselator = Tesselator.getInstance();
            BufferBuilder buffer = tesselator.getBuilder();
            buffer.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_TEX_COLOR);

            float r = ((model.getColor() >> 16) & 0xFF) / 255.0f;
            float g = ((model.getColor() >> 8) & 0xFF) / 255.0f;
            float b = (model.getColor() & 0xFF) / 255.0f;
            float a = ((model.getColor() >> 24) & 0xFF) / 255.0f;

            RandomSource random = RandomSource.create(42L);
            var quads = bakedModel.getQuads(null, null, random);

            for (var quad : quads) {
                for (int i = 0; i < 4; i++) {
                    var vertex = quad.getVertices();
                    float x = Float.intBitsToFloat(vertex[i * 8]);
                    float y = Float.intBitsToFloat(vertex[i * 8 + 1]);
                    float z = Float.intBitsToFloat(vertex[i * 8 + 2]);
                    float u = Float.intBitsToFloat(vertex[i * 8 + 4]);
                    float v = Float.intBitsToFloat(vertex[i * 8 + 5]);

                    buffer.vertex(poseStack.last().pose(), x, y, z)
                            .uv(u, v)
                            .color(r, g, b, a)
                            .endVertex();
                }
            }

            BufferUploader.drawWithShader(buffer.end());
            return true;

        } catch (Exception e) {
            return false;
        }
    }

    private static void renderFallbackCube(PoseStack poseStack, WorldModel model) {
        Tesselator tesselator = Tesselator.getInstance();
        BufferBuilder buffer = tesselator.getBuilder();

        float r = ((model.getColor() >> 16) & 0xFF) / 255.0f;
        float g = ((model.getColor() >> 8) & 0xFF) / 255.0f;
        float b = (model.getColor() & 0xFF) / 255.0f;
        float a = ((model.getColor() >> 24) & 0xFF) / 255.0f;

        RenderSystem.setShader(GameRenderer::getPositionColorShader);
        buffer.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_COLOR);

        Matrix4f matrix = poseStack.last().pose();
        renderColoredCube(buffer, matrix, r, g, b, a);

        BufferUploader.drawWithShader(buffer.end());
    }

    private static void renderColoredCube(BufferBuilder buffer, Matrix4f matrix, float r, float g, float b, float a) {
        // Нижняя грань
        buffer.vertex(matrix, 0, 0, 0).color(r, g, b, a).endVertex();
        buffer.vertex(matrix, 1, 0, 0).color(r, g, b, a).endVertex();
        buffer.vertex(matrix, 1, 0, 1).color(r, g, b, a).endVertex();
        buffer.vertex(matrix, 0, 0, 1).color(r, g, b, a).endVertex();

        // Верхняя грань
        buffer.vertex(matrix, 0, 1, 0).color(r, g, b, a).endVertex();
        buffer.vertex(matrix, 0, 1, 1).color(r, g, b, a).endVertex();
        buffer.vertex(matrix, 1, 1, 1).color(r, g, b, a).endVertex();
        buffer.vertex(matrix, 1, 1, 0).color(r, g, b, a).endVertex();

        // Северная грань
        buffer.vertex(matrix, 0, 0, 0).color(r, g, b, a).endVertex();
        buffer.vertex(matrix, 0, 1, 0).color(r, g, b, a).endVertex();
        buffer.vertex(matrix, 1, 1, 0).color(r, g, b, a).endVertex();
        buffer.vertex(matrix, 1, 0, 0).color(r, g, b, a).endVertex();

        // Южная грань
        buffer.vertex(matrix, 0, 0, 1).color(r, g, b, a).endVertex();
        buffer.vertex(matrix, 1, 0, 1).color(r, g, b, a).endVertex();
        buffer.vertex(matrix, 1, 1, 1).color(r, g, b, a).endVertex();
        buffer.vertex(matrix, 0, 1, 1).color(r, g, b, a).endVertex();

        // Западная грань
        buffer.vertex(matrix, 0, 0, 0).color(r, g, b, a).endVertex();
        buffer.vertex(matrix, 0, 0, 1).color(r, g, b, a).endVertex();
        buffer.vertex(matrix, 0, 1, 1).color(r, g, b, a).endVertex();
        buffer.vertex(matrix, 0, 1, 0).color(r, g, b, a).endVertex();

        // Восточная грань
        buffer.vertex(matrix, 1, 0, 0).color(r, g, b, a).endVertex();
        buffer.vertex(matrix, 1, 1, 0).color(r, g, b, a).endVertex();
        buffer.vertex(matrix, 1, 1, 1).color(r, g, b, a).endVertex();
        buffer.vertex(matrix, 1, 0, 1).color(r, g, b, a).endVertex();
    }
}