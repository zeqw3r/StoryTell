// WorldModel.java
package com.example.storytell.init.world;

import com.mojang.blaze3d.vertex.*;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;
import org.joml.Matrix4f;
import org.joml.Quaternionf;
import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class WorldModel {
    private final String id;
    private final ResourceLocation modelLocation;
    private float x, y, z;
    private float prevX, prevY, prevZ;
    private float startX, startY, startZ;
    private float targetX, targetY, targetZ;
    private long moveStartTime;
    private int moveDuration;
    private String easingType;
    private boolean isMoving = false;

    // Параметры вращения
    private float rotationX = 0.0f;
    private float rotationY = 0.0f;
    private float rotationZ = 0.0f;
    private float rotationSpeedX = 0.02f;
    private float rotationSpeedY = 0.03f;
    private float rotationSpeedZ = 0.01f;

    // Система частиц
    private final List<ParticleModel> particles = new ArrayList<>();
    private final List<Vector3f> trailPositions = new ArrayList<>();
    private static final int MAX_TRAIL_LENGTH = 30;
    private long lastParticleTime = 0;
    private static final long PARTICLE_INTERVAL = 45; // Увеличили интервал для уменьшения количества частиц

    private final float size;
    private final int color;

    public WorldModel(String id, ResourceLocation modelLocation, float x, float y, float z, float size, int color) {
        this.id = id;
        this.modelLocation = modelLocation;
        this.x = x;
        this.y = y;
        this.z = z;
        this.prevX = x;
        this.prevY = y;
        this.prevZ = z;
        this.startX = x;
        this.startY = y;
        this.startZ = z;
        this.targetX = x;
        this.targetY = y;
        this.targetZ = z;
        this.size = size;
        this.color = color;
        this.moveDuration = 0;
        this.easingType = "easeInCubic";
        this.isMoving = false;

        // Случайные скорости вращения
        this.rotationSpeedX = (float) (Math.random() * 0.05f + 0.01f);
        this.rotationSpeedY = (float) (Math.random() * 0.05f + 0.01f);
        this.rotationSpeedZ = (float) (Math.random() * 0.05f + 0.01f);
    }

    public void setTargetPosition(float targetX, float targetY, float targetZ, int moveDuration, String easingType) {
        this.startX = this.x;
        this.startY = this.y;
        this.startZ = this.z;
        this.targetX = targetX;
        this.targetY = targetY;
        this.targetZ = targetZ;
        this.moveStartTime = System.currentTimeMillis();
        this.moveDuration = moveDuration;
        this.easingType = (easingType != null && !easingType.isEmpty()) ? easingType : "easeInCubic";
        this.isMoving = true;

        trailPositions.clear();
    }

    public void update() {
        prevX = x;
        prevY = y;
        prevZ = z;

        if (isMoving && moveDuration > 0) {
            long currentTime = System.currentTimeMillis();
            long elapsed = currentTime - moveStartTime;

            if (elapsed >= moveDuration) {
                this.x = targetX;
                this.y = targetY;
                this.z = targetZ;
                this.isMoving = false;
                trailPositions.clear();
            } else {
                float progress = (float) elapsed / moveDuration;
                float easedProgress = applyEasing(progress, easingType);

                this.x = startX + (targetX - startX) * easedProgress;
                this.y = startY + (targetY - startY) * easedProgress;
                this.z = startZ + (targetZ - startZ) * easedProgress;

                // Создаем частицы
                spawnParticles();
                updateTrail();
            }
        }

        // Обновляем вращение
        rotationX += rotationSpeedX;
        rotationY += rotationSpeedY;
        rotationZ += rotationSpeedZ;

        // Сбрасываем углы
        if (rotationX > 360.0f) rotationX -= 360.0f;
        if (rotationY > 360.0f) rotationY -= 360.0f;
        if (rotationZ > 360.0f) rotationZ -= 360.0f;

        // Обновляем частицы
        updateParticles();
    }

    private void updateTrail() {
        trailPositions.add(0, new Vector3f(x, y, z));
        while (trailPositions.size() > MAX_TRAIL_LENGTH) {
            trailPositions.remove(trailPositions.size() - 1);
        }
    }

    private void updateParticles() {
        Iterator<ParticleModel> iterator = particles.iterator();
        while (iterator.hasNext()) {
            ParticleModel particle = iterator.next();
            particle.update();
            if (!particle.isAlive()) {
                iterator.remove();
            }
        }
    }

    private void spawnParticles() {
        long currentTime = System.currentTimeMillis();
        if (currentTime - lastParticleTime < PARTICLE_INTERVAL) {
            return;
        }
        lastParticleTime = currentTime;

        // Частицы вокруг метеорита
        spawnCoreParticles();

        // Частицы хвоста
        spawnTrailParticles();
    }

    private void spawnCoreParticles() {
        // Огненные частицы вокруг ядра - уменьшено в 3 раза
        for (int i = 0; i < 2; i++) { // Было 6, стало 2
            float offsetX = (float) (Math.random() - 0.5) * size * 0.1f;
            float offsetY = (float) (Math.random() - 0.5) * size * 0.1f;
            float offsetZ = (float) (Math.random() - 0.5) * size * 0.1f;

            int particleColor = 0xFFFF4500; // Оранжево-красный для огня
            float particleSize = size * 0.08f;
            int lifetime = 20 + (int)(Math.random() * 10);

            ParticleModel particle = new ParticleModel(
                    x + offsetX, y + offsetY, z + offsetZ,
                    particleSize, particleColor, lifetime,
                    ParticleModel.TYPE_FIRE
            );
            particles.add(particle);
        }

        // Дымовые частицы вокруг ядра - уменьшено в 3 раза
        for (int i = 0; i < 1; i++) { // Было 3, стало 1
            float offsetX = (float) (Math.random() - 0.5) * size * 0.15f;
            float offsetY = (float) (Math.random() - 0.5) * size * 0.15f;
            float offsetZ = (float) (Math.random() - 0.5) * size * 0.15f;

            int particleColor = 0xFF808080; // Серый для дыма
            float particleSize = size * 0.12f;
            int lifetime = 40 + (int)(Math.random() * 20);

            ParticleModel particle = new ParticleModel(
                    x + offsetX, y + offsetY, z + offsetZ,
                    particleSize, particleColor, lifetime,
                    ParticleModel.TYPE_SMOKE
            );
            particles.add(particle);
        }
    }

    private void spawnTrailParticles() {
        // Создаем частицы вдоль хвоста - уменьшено в 3 раза
        for (int i = 0; i < trailPositions.size(); i++) {
            Vector3f trailPos = trailPositions.get(i);
            float intensity = 1.0f - (float)i / trailPositions.size();

            // Огненные частицы в хвосте - уменьшено в 3 раза
            for (int j = 0; j < 1; j++) { // Было 2, стало 1
                float offsetX = (float) (Math.random() - 0.5) * size * 0.07f * intensity;
                float offsetY = (float) (Math.random() - 0.5) * size * 0.07f * intensity;
                float offsetZ = (float) (Math.random() - 0.5) * size * 0.07f * intensity;

                int particleColor = 0xFFFF4500;
                float particleSize = size * 0.06f * intensity;
                int lifetime = 15 + (int)(Math.random() * 10);

                ParticleModel particle = new ParticleModel(
                        trailPos.x + offsetX, trailPos.y + offsetY, trailPos.z + offsetZ,
                        particleSize, particleColor, lifetime,
                        ParticleModel.TYPE_FIRE
                );
                particles.add(particle);
            }

            // Дымовые частицы в хвосте - уменьшена вероятность
            if (Math.random() < 0.1f) { // Было 0.3f, стало 0.1f
                float offsetX = (float) (Math.random() - 0.5) * size * 0.1f * intensity;
                float offsetY = (float) (Math.random() - 0.5) * size * 0.1f * intensity;
                float offsetZ = (float) (Math.random() - 0.5) * size * 0.1f * intensity;

                int particleColor = 0xFF808080;
                float particleSize = size * 0.09f * intensity;
                int lifetime = 30 + (int)(Math.random() * 15);

                ParticleModel particle = new ParticleModel(
                        trailPos.x + offsetX, trailPos.y + offsetY, trailPos.z + offsetZ,
                        particleSize, particleColor, lifetime,
                        ParticleModel.TYPE_SMOKE
                );
                particles.add(particle);
            }
        }

        // Заполняем пробелы в хвосте интерполяцией - уменьшена вероятность
        if (trailPositions.size() >= 2) {
            for (int i = 0; i < trailPositions.size() - 1; i++) {
                Vector3f current = trailPositions.get(i);
                Vector3f next = trailPositions.get(i + 1);

                int interpolationSteps = 2;
                for (int step = 1; step < interpolationSteps; step++) {
                    float t = (float)step / interpolationSteps;
                    float interpX = next.x + (current.x - next.x) * t;
                    float interpY = next.y + (current.y - next.y) * t;
                    float interpZ = next.z + (current.z - next.z) * t;

                    float intensity = 1.0f - (float)(i * interpolationSteps + step) / (trailPositions.size() * interpolationSteps);

                    if (Math.random() < 0.17f) { // Было 0.5f, стало 0.17f
                        float offsetX = (float) (Math.random() - 0.5) * size * 0.05f * intensity;
                        float offsetY = (float) (Math.random() - 0.5) * size * 0.05f * intensity;
                        float offsetZ = (float) (Math.random() - 0.5) * size * 0.05f * intensity;

                        int particleColor = 0xFFFF4500;
                        float particleSize = size * 0.04f * intensity;
                        int lifetime = 10 + (int)(Math.random() * 5);

                        ParticleModel particle = new ParticleModel(
                                interpX + offsetX, interpY + offsetY, interpZ + offsetZ,
                                particleSize, particleColor, lifetime,
                                ParticleModel.TYPE_FIRE
                        );
                        particles.add(particle);
                    }
                }
            }
        }
    }

    public void render(PoseStack poseStack, float partialTick, double cameraX, double cameraY, double cameraZ) {
        update();

        // Рендерим метеорит
        renderMeteor(poseStack, partialTick, cameraX, cameraY, cameraZ);

        // Рендерим все частицы
        for (ParticleModel particle : particles) {
            particle.render(poseStack, partialTick, cameraX, cameraY, cameraZ);
        }
    }

    private void renderMeteor(PoseStack poseStack, float partialTick, double cameraX, double cameraY, double cameraZ) {
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.depthMask(false);
        RenderSystem.disableCull();

        poseStack.pushPose();

        // Правильное преобразование мировых координат
        poseStack.translate(x - cameraX, y - cameraY, z - cameraZ);

        // Применяем вращение
        poseStack.mulPose(new Quaternionf().rotationXYZ(
                (float)Math.toRadians(rotationX),
                (float)Math.toRadians(rotationY),
                (float)Math.toRadians(rotationZ)
        ));

        // Применяем нормальный масштаб
        float scale = size / 16.0f;
        poseStack.scale(scale, scale, scale);
        poseStack.translate(-0.5f, -0.5f, -0.5f);

        // Применяем цвет
        float r = ((color >> 16) & 0xFF) / 255.0f;
        float g = ((color >> 8) & 0xFF) / 255.0f;
        float b = (color & 0xFF) / 255.0f;
        float a = ((color >> 24) & 0xFF) / 255.0f;

        if (!renderJsonModel(poseStack, r, g, b, a)) {
            renderFallbackQuad(poseStack, r, g, b, a);
        }

        poseStack.popPose();

        RenderSystem.setShaderColor(1.0f, 1.0f, 1.0f, 1.0f);
        RenderSystem.enableCull();
        RenderSystem.depthMask(true);
        RenderSystem.disableBlend();
    }

    private boolean renderJsonModel(PoseStack poseStack, float r, float g, float b, float a) {
        try {
            BakedModel model = Minecraft.getInstance().getModelManager().getModel(modelLocation);

            if (model == null || model == Minecraft.getInstance().getModelManager().getMissingModel()) {
                return false;
            }

            RenderSystem.setShader(GameRenderer::getPositionTexShader);
            RenderSystem.setShaderTexture(0, TextureAtlas.LOCATION_BLOCKS);

            Tesselator tesselator = Tesselator.getInstance();
            BufferBuilder bufferBuilder = tesselator.getBuilder();
            bufferBuilder.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_TEX_COLOR);

            RandomSource random = RandomSource.create(42L);

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
            return true;

        } catch (Exception e) {
            return false;
        }
    }

    private void renderFallbackQuad(PoseStack poseStack, float r, float g, float b, float a) {
        Tesselator tesselator = Tesselator.getInstance();
        BufferBuilder bufferBuilder = tesselator.getBuilder();

        bufferBuilder.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_TEX_COLOR);

        Matrix4f matrix = poseStack.last().pose();
        float halfSize = 0.5f;

        bufferBuilder.vertex(matrix, -halfSize, -halfSize, 0).uv(0.0f, 0.0f).color(r, g, b, a).endVertex();
        bufferBuilder.vertex(matrix, -halfSize, halfSize, 0).uv(0.0f, 1.0f).color(r, g, b, a).endVertex();
        bufferBuilder.vertex(matrix, halfSize, halfSize, 0).uv(1.0f, 1.0f).color(r, g, b, a).endVertex();
        bufferBuilder.vertex(matrix, halfSize, -halfSize, 0).uv(1.0f, 0.0f).color(r, g, b, a).endVertex();

        BufferUploader.drawWithShader(bufferBuilder.end());
    }

    private float applyEasing(float progress, String easingType) {
        switch (easingType) {
            case "easeInQuad":
                return progress * progress;
            case "easeInCubic":
                return progress * progress * progress;
            case "easeInQuart":
                return progress * progress * progress * progress;
            case "easeInQuint":
                return progress * progress * progress * progress * progress;
            case "easeInExpo":
                return progress == 0 ? 0 : (float)Math.pow(2, 10 * progress - 10);
            case "easeInCirc":
                return 1 - (float)Math.sqrt(1 - Math.pow(progress, 2));
            case "easeInBack":
                float c1 = 1.70158f;
                return progress * progress * ((c1 + 1) * progress - c1);
            case "easeOutQuad":
                return 1 - (1 - progress) * (1 - progress);
            case "easeOutCubic":
                return (float) (1 - Math.pow(1 - progress, 3));
            case "easeOutQuart":
                return (float) (1 - Math.pow(1 - progress, 4));
            case "easeOutQuint":
                return (float) (1 - Math.pow(1 - progress, 5));
            case "easeOutExpo":
                return progress == 1 ? 1 : 1 - (float)Math.pow(2, -10 * progress);
            case "easeOutCirc":
                return (float)Math.sqrt(1 - Math.pow(progress - 1, 2));
            case "easeOutBack":
                float c1b = 1.70158f;
                return 1 + c1b * (float)Math.pow(progress - 1, 3) + (float)Math.pow(progress - 1, 2);
            case "easeInOutQuad":
                return progress < 0.5 ? 2 * progress * progress : 1 - (float)Math.pow(-2 * progress + 2, 2) / 2;
            case "easeInOutCubic":
                return progress < 0.5 ? 4 * progress * progress * progress : 1 - (float)Math.pow(-2 * progress + 2, 3) / 2;
            case "easeInOutQuart":
                return progress < 0.5 ? 8 * progress * progress * progress * progress : 1 - (float)Math.pow(-2 * progress + 2, 4) / 2;
            case "easeInOutQuint":
                return progress < 0.5 ? 16 * progress * progress * progress * progress * progress : 1 - (float)Math.pow(-2 * progress + 2, 5) / 2;
            case "easeInOutExpo":
                return progress == 0 ? 0 : progress == 1 ? 1 : progress < 0.5 ?
                        (float)Math.pow(2, 20 * progress - 10) / 2 :
                        (2 - (float)Math.pow(2, -20 * progress + 10)) / 2;
            case "easeInOutCirc":
                return progress < 0.5 ?
                        (1 - (float)Math.sqrt(1 - Math.pow(2 * progress, 2))) / 2 :
                        ((float)Math.sqrt(1 - Math.pow(-2 * progress + 2, 2)) + 1) / 2;
            case "easeInOutBack":
                float c2 = 1.70158f * 1.525f;
                return progress < 0.5 ?
                        ((float)Math.pow(2 * progress, 2) * ((c2 + 1) * 2 * progress - c2)) / 2 :
                        ((float)Math.pow(2 * progress - 2, 2) * ((c2 + 1) * (progress * 2 - 2) + c2) + 2) / 2;
            case "linear":
            default:
                return progress;
        }
    }

    // Геттеры и сеттеры для вращения
    public void setRotationSpeeds(float speedX, float speedY, float speedZ) {
        this.rotationSpeedX = speedX;
        this.rotationSpeedY = speedY;
        this.rotationSpeedZ = speedZ;
    }

    public void setRotation(float rotX, float rotY, float rotZ) {
        this.rotationX = rotX;
        this.rotationY = rotY;
        this.rotationZ = rotZ;
    }

    public String getId() { return id; }
    public float getX() { return x; }
    public float getY() { return y; }
    public float getZ() { return z; }
    public float getSize() { return size; }
    public int getColor() { return color; }
    public ResourceLocation getModelLocation() { return modelLocation; }
    public boolean isMoving() { return isMoving; }

    public void setPosition(float x, float y, float z) {
        this.x = x;
        this.y = y;
        this.z = z;
        this.startX = x;
        this.startY = y;
        this.startZ = z;
        this.targetX = x;
        this.targetY = y;
        this.targetZ = z;
        this.isMoving = false;
        trailPositions.clear();
        particles.clear();
    }

    public void clearParticles() {
        particles.clear();
    }
}