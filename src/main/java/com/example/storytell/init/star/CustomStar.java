// CustomStar.java
package com.example.storytell.init.star;

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

import java.util.ArrayList;
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
    private final float baseX, baseY, baseZ;

    // Animation properties
    private float rotationAngle = 0.0f;
    private final float rotationSpeed;
    private final float pulseSpeed;
    private final float pulseAmount;

    // Visibility properties
    private boolean visible;
    private boolean wasVisibleBeforeHiding;
    private final boolean defaultVisible;

    // Modifiers
    private final List<StarModifierManager.StarModifier> activeModifiers = new ArrayList<>();

    public CustomStar(String name, String modelPath, float size, int color,
                      float rightAscension, float declination, float distance,
                      float rotationSpeed, float pulseSpeed, float pulseAmount, boolean defaultVisible) {
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
        this.defaultVisible = defaultVisible;

        calculatePosition();
        // Store base position
        this.baseX = x;
        this.baseY = y;
        this.baseZ = z;

        // Загружаем сохраненную видимость из конфига, если есть
        Boolean savedVisibility = com.example.storytell.init.HologramConfig.getStarVisibility(name);
        if (savedVisibility != null) {
            this.visible = savedVisibility;
        } else {
            this.visible = defaultVisible;
            // Сохраняем видимость по умолчанию в конфиг
            com.example.storytell.init.HologramConfig.setStarVisibility(name, defaultVisible);
        }

        this.wasVisibleBeforeHiding = this.visible;

        // Сохраняем настройки звезды в конфиг
        saveStarSettings();
    }

    // Конструктор для обратной совместимости - по умолчанию видимая
    public CustomStar(String name, String modelPath, float size, int color,
                      float rightAscension, float declination, float distance,
                      float rotationSpeed, float pulseSpeed, float pulseAmount) {
        this(name, modelPath, size, color, rightAscension, declination, distance,
                rotationSpeed, pulseSpeed, pulseAmount, true);
    }

    public void render(com.mojang.blaze3d.vertex.PoseStack poseStack, float partialTick) {
        // Check visibility - if star is hidden, don't render
        if (!visible) {
            return;
        }

        // Update animation and apply modifiers
        updateAnimation(partialTick);
        updatePositionWithModifiers();

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

    private void updatePositionWithModifiers() {
        // Reset to base position
        this.x = baseX;
        this.y = baseY;
        this.z = baseZ;

        // Apply all active modifiers
        List<StarModifierManager.StarModifier> modifiers = StarModifierManager.getModifiers(name);
        for (StarModifierManager.StarModifier modifier : modifiers) {
            if (modifier.isSmoothMove() && modifier.getType().equals("smooth_move")) {
                // Применяем плавное перемещение с интерполяцией
                float progress = modifier.getProgress();
                float easedProgress = calculateEasing(progress, modifier.getEasingType());

                this.x = modifier.getStartX() + (modifier.getTargetX() - modifier.getStartX()) * easedProgress;
                this.y = modifier.getStartY() + (modifier.getTargetY() - modifier.getStartY()) * easedProgress;
                this.z = modifier.getStartZ() + (modifier.getTargetZ() - modifier.getStartZ()) * easedProgress;
            } else if (modifier.getType().equals("offset")) {
                // Apply offset to all coordinates
                this.x += modifier.getX();
                this.y += modifier.getY();
                this.z += modifier.getZ();
            } else if (modifier.getType().equals("player_position")) {
                // For player-relative positioning
                this.y = modifier.getY();
            }
        }

        // Update local list
        this.activeModifiers.clear();
        this.activeModifiers.addAll(modifiers);
    }

    // Вычисление easing функции в зависимости от типа
    private float calculateEasing(float progress, String easingType) {
        switch (easingType) {
            case "easeInCubic":
                return easeInCubic(progress);
            case "easeOutCubic":
                return easeOutCubic(progress);
            case "easeInOutCubic":
                return easeInOutCubic(progress);
            case "easeInExpo":
                return easeInExpo(progress);
            case "easeOutExpo":
                return easeOutExpo(progress);
            case "easeInBack":
                return easeInBack(progress);
            case "easeOutBack":
                return easeOutBack(progress);
            case "easeInElastic":
                return easeInElastic(progress);
            case "easeOutElastic":
                return easeOutElastic(progress);
            case "linear":
            default:
                return progress;
        }
    }

    // Функции плавности (easing functions)

    // Линейная (по умолчанию)
    private float linear(float x) {
        return x;
    }

    // Кубическое ускорение (начинается медленно, затем ускоряется)
    private float easeInCubic(float x) {
        return x * x * x;
    }

    // Кубическое замедление (начинается быстро, затем замедляется)
    private float easeOutCubic(float x) {
        return (float) (1 - Math.pow(1 - x, 3));
    }

    // Кубическое ускорение и замедление
    private float easeInOutCubic(float x) {
        return x < 0.5 ? 4 * x * x * x : 1 - (float)Math.pow(-2 * x + 2, 3) / 2;
    }

    // Экспоненциальное ускорение (очень медленное начало, быстрое ускорение)
    private float easeInExpo(float x) {
        return x == 0 ? 0 : (float)Math.pow(2, 10 * x - 10);
    }

    // Экспоненциальное замедление (быстрое начало, очень медленный конец)
    private float easeOutExpo(float x) {
        return x == 1 ? 1 : 1 - (float)Math.pow(2, -10 * x);
    }

    // Эффект отскока в начале
    private float easeInBack(float x) {
        float c1 = 1.70158f;
        float c3 = c1 + 1;
        return c3 * x * x * x - c1 * x * x;
    }

    // Эффект отскока в конце
    private float easeOutBack(float x) {
        float c1 = 1.70158f;
        float c3 = c1 + 1;
        return 1 + c3 * (float)Math.pow(x - 1, 3) + c1 * (float)Math.pow(x - 1, 2);
    }

    // Эластичный эффект в начале
    private float easeInElastic(float x) {
        float c4 = (2 * (float)Math.PI) / 3;
        return x == 0 ? 0 : x == 1 ? 1 :
                -(float)Math.pow(2, 10 * x - 10) * (float)Math.sin((x * 10 - 10.75) * c4);
    }

    // Эластичный эффект в конце
    private float easeOutElastic(float x) {
        float c4 = (2 * (float)Math.PI) / 3;
        return x == 0 ? 0 : x == 1 ? 1 :
                (float)Math.pow(2, -10 * x) * (float)Math.sin((x * 10 - 0.75) * c4) + 1;
    }

    public void applyPositionModifier(String type, float x, float y, float z, int duration, Runnable onExpire) {
        StarModifierManager.StarModifier modifier = new StarModifierManager.StarModifier(type, x, y, z, duration, onExpire);
        StarModifierManager.addModifier(name, modifier);
        updatePositionWithModifiers();
    }

    // Новый метод для плавного перемещения с выбором типа easing
    public void applySmoothMovement(float startX, float startY, float startZ,
                                    float targetX, float targetY, float targetZ,
                                    int duration, Runnable onExpire, String easingType) {
        StarModifierManager.StarModifier modifier = new StarModifierManager.StarModifier(
                "smooth_move", startX, startY, startZ, targetX, targetY, targetZ, duration, onExpire, easingType);
        StarModifierManager.addModifier(name, modifier);
        updatePositionWithModifiers();
    }

    public void resetToBasePosition() {
        this.x = baseX;
        this.y = baseY;
        this.z = baseZ;
        StarModifierManager.removeModifiers(name);
    }

    // Visibility methods
    public void setVisible(boolean visible) {
        this.visible = visible;
        if (!visible) {
            this.wasVisibleBeforeHiding = this.visible;
        }
        // Сохраняем в конфиг
        com.example.storytell.init.HologramConfig.setStarVisibility(name, visible);
    }

    public boolean isVisible() {
        return visible;
    }

    public void toggleVisibility() {
        this.visible = !this.visible;
        if (!visible) {
            this.wasVisibleBeforeHiding = this.visible;
        }
        // Сохраняем в конфиг
        com.example.storytell.init.HologramConfig.setStarVisibility(name, visible);
    }

    public void restoreVisibility() {
        this.visible = this.wasVisibleBeforeHiding;
        com.example.storytell.init.HologramConfig.setStarVisibility(name, visible);
    }

    public void resetToDefaultVisibility() {
        this.visible = this.defaultVisible;
        this.wasVisibleBeforeHiding = this.defaultVisible;
        // Сохраняем в конфиг
        com.example.storytell.init.HologramConfig.setStarVisibility(name, defaultVisible);
    }

    public boolean getDefaultVisible() {
        return defaultVisible;
    }

    // Сохранение настроек звезды в конфиг
    private void saveStarSettings() {
        com.example.storytell.init.HologramConfig.StarSettings settings =
                new com.example.storytell.init.HologramConfig.StarSettings(
                        visible, baseX, baseY, baseZ, rightAscension, declination, distance
                );
        com.example.storytell.init.HologramConfig.setStarSettings(name, settings);
    }

    // Удаление звезды из конфига
    public void removeFromConfig() {
        com.example.storytell.init.HologramConfig.removeStar(name);
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
    public float getBaseX() { return baseX; }
    public float getBaseY() { return baseY; }
    public float getBaseZ() { return baseZ; }
}