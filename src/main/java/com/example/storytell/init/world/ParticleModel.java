// ParticleModel.java
package com.example.storytell.init.world;

import com.mojang.blaze3d.vertex.*;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.resources.ResourceLocation;
import org.joml.Matrix4f;

public class ParticleModel {
    private float x, y, z;
    private float prevX, prevY, prevZ;
    private float size;
    private int color;
    private int lifetime;
    private int age;
    private int particleType;

    // Типы частиц
    public static final int TYPE_FIRE = 0;
    public static final int TYPE_SMOKE = 1;

    // Текстуры частиц из ванильного майнкрафта
    private static final ResourceLocation FIRE_TEXTURE = new ResourceLocation("textures/particle/flame.png");
    // Исправленные текстуры дыма - правильные названия файлов из Minecraft
    private static final ResourceLocation[] SMOKE_TEXTURES = {
            new ResourceLocation("textures/particle/big_smoke_0.png"),
            new ResourceLocation("textures/particle/big_smoke_1.png"),
            new ResourceLocation("textures/particle/big_smoke_2.png"),
            new ResourceLocation("textures/particle/big_smoke_3.png"),
            new ResourceLocation("textures/particle/big_smoke_4.png"),
            new ResourceLocation("textures/particle/big_smoke_5.png"),
            new ResourceLocation("textures/particle/big_smoke_6.png"),
            new ResourceLocation("textures/particle/big_smoke_7.png"),
            new ResourceLocation("textures/particle/big_smoke_8.png"),
            new ResourceLocation("textures/particle/big_smoke_9.png"),
            new ResourceLocation("textures/particle/big_smoke_10.png"),
            new ResourceLocation("textures/particle/big_smoke_11.png")
    };

    public ParticleModel(float x, float y, float z, float size, int color, int lifetime, int particleType) {
        this.x = x;
        this.y = y;
        this.z = z;
        this.prevX = x;
        this.prevY = y;
        this.prevZ = z;
        this.size = size;
        this.color = color;
        this.lifetime = lifetime;
        this.age = 0;
        this.particleType = particleType;
    }

    public void update() {
        prevX = x;
        prevY = y;
        prevZ = z;
        age++;
    }

    public void render(PoseStack poseStack, float partialTick, double cameraX, double cameraY, double cameraZ) {
        // Интерполяция позиции для плавности
        float renderX = (float) (prevX + (x - prevX) * partialTick - cameraX);
        float renderY = (float) (prevY + (y - prevY) * partialTick - cameraY);
        float renderZ = (float) (prevZ + (z - prevZ) * partialTick - cameraZ);

        // Вычисляем прозрачность в зависимости от возраста
        float lifeRatio = (float) age / lifetime;
        float alpha = 1.0f - lifeRatio;

        // Для дыма делаем более плавное исчезновение
        if (particleType == TYPE_SMOKE) {
            alpha = (float) (1.0f - Math.pow(lifeRatio, 0.5));
        }

        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.depthMask(false);
        RenderSystem.disableCull();

        poseStack.pushPose();

        // Позиционируем частицу
        poseStack.translate(renderX, renderY, renderZ);

        // Всегда смотрим на камеру (billboard эффект)
        poseStack.mulPose(Minecraft.getInstance().getEntityRenderDispatcher().cameraOrientation());

        // Масштабируем частицу
        poseStack.scale(size, size, size);

        // Применяем цвет с учетом прозрачности
        float r = ((color >> 16) & 0xFF) / 255.0f;
        float g = ((color >> 8) & 0xFF) / 255.0f;
        float b = (color & 0xFF) / 255.0f;
        float a = alpha;

        // Выбираем текстуру в зависимости от типа частицы
        ResourceLocation texture;
        switch (particleType) {
            case TYPE_FIRE:
                texture = FIRE_TEXTURE;
                break;
            case TYPE_SMOKE:
                // Для дыма выбираем текстуру на основе возраста для анимации
                int textureIndex = (age / 2) % SMOKE_TEXTURES.length; // Меняем текстуру каждые 2 тика
                texture = SMOKE_TEXTURES[textureIndex];
                break;
            default:
                texture = FIRE_TEXTURE;
        }

        // Рендерим текстуру частицы
        renderParticleTexture(poseStack, texture, r, g, b, a);

        poseStack.popPose();

        RenderSystem.setShaderColor(1.0f, 1.0f, 1.0f, 1.0f);
        RenderSystem.enableCull();
        RenderSystem.depthMask(true);
        RenderSystem.disableBlend();
    }

    private void renderParticleTexture(PoseStack poseStack, ResourceLocation texture, float r, float g, float b, float a) {
        RenderSystem.setShader(GameRenderer::getPositionTexShader);
        RenderSystem.setShaderTexture(0, texture);

        Tesselator tesselator = Tesselator.getInstance();
        BufferBuilder bufferBuilder = tesselator.getBuilder();
        bufferBuilder.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_TEX_COLOR);

        Matrix4f matrix = poseStack.last().pose();
        float halfSize = 0.5f;

        // Рендерим квадрат с текстурой частицы
        // Правильные UV координаты для текстуры
        bufferBuilder.vertex(matrix, -halfSize, -halfSize, 0).uv(0.0f, 1.0f).color(r, g, b, a).endVertex();
        bufferBuilder.vertex(matrix, -halfSize, halfSize, 0).uv(0.0f, 0.0f).color(r, g, b, a).endVertex();
        bufferBuilder.vertex(matrix, halfSize, halfSize, 0).uv(1.0f, 0.0f).color(r, g, b, a).endVertex();
        bufferBuilder.vertex(matrix, halfSize, -halfSize, 0).uv(1.0f, 1.0f).color(r, g, b, a).endVertex();

        BufferUploader.drawWithShader(bufferBuilder.end());
    }

    public boolean isAlive() {
        return age < lifetime;
    }

    public void setPosition(float x, float y, float z) {
        this.x = x;
        this.y = y;
        this.z = z;
    }

    // Геттеры
    public float getX() { return x; }
    public float getY() { return y; }
    public float getZ() { return z; }
    public int getParticleType() { return particleType; }
}