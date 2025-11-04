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
    private int color; // Убрали final

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

    // Pulsation animation fields
    private float pulseTime = 0.0f;
    private float currentAnimatedSize;

    // Color animation fields
    private int startColor;
    private int targetColor;
    private int colorAnimationDuration;
    private int colorAnimationProgress;
    private boolean isColorAnimating = false;

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
        this.currentAnimatedSize = size;

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
            com.example.storytell.init.HologramConfig.setStarVisibility(name, defaultVisible);
        }

        this.wasVisibleBeforeHiding = this.visible;
        saveStarSettings();
    }

    // Конструктор для обратной совместимости
    public CustomStar(String name, String modelPath, float size, int color,
                      float rightAscension, float declination, float distance,
                      float rotationSpeed, float pulseSpeed, float pulseAmount) {
        this(name, modelPath, size, color, rightAscension, declination, distance,
                rotationSpeed, pulseSpeed, pulseAmount, true);
    }

    public void update(float deltaTime, float partialTick) {
        if (!visible) return;

        updateAnimation(deltaTime);
        updatePulsation(deltaTime);
        updatePositionWithModifiers();
        updateColorAnimation();
    }

    public void render(com.mojang.blaze3d.vertex.PoseStack poseStack, float partialTick) {
        if (!visible) {
            return;
        }

        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.depthMask(false);

        poseStack.pushPose();

        // Apply transformations
        poseStack.translate(x, y, z);
        poseStack.mulPose(new Quaternionf().rotationZ((float) Math.toRadians(rotationAngle)));

        // Extract color components
        float r = ((color >> 16) & 0xFF) / 255.0f;
        float g = ((color >> 8) & 0xFF) / 255.0f;
        float b = (color & 0xFF) / 255.0f;
        float a = ((color >> 24) & 0xFF) / 255.0f;
        if (a == 0) a = 1.0f; // Ensure visibility if alpha is 0

        // Set shader color - это важно для применения цвета
        RenderSystem.setShaderColor(r, g, b, a);

        // Try to render the model, fallback to quad if needed
        if (!renderJsonModel(poseStack, currentAnimatedSize)) {
            renderFallbackQuad(poseStack, currentAnimatedSize);
        }

        poseStack.popPose();

        // Reset color
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

    private void updateAnimation(float deltaTime) {
        rotationAngle += rotationSpeed * deltaTime * 50.0f;
        if (rotationAngle >= 360.0f) {
            rotationAngle -= 360.0f;
        }
    }

    private void updatePulsation(float deltaTime) {
        pulseTime += deltaTime * pulseSpeed;
        float sineValue = (float) Math.sin(pulseTime * 2 * Math.PI);
        float easedPulse = easeInOutSine(sineValue);

        float minSize = size * (1.0f - pulseAmount);
        float maxSize = size * (1.0f + pulseAmount);
        currentAnimatedSize = minSize + (maxSize - minSize) * ((easedPulse + 1.0f) / 2.0f);
        currentAnimatedSize = Math.max(1.0f, currentAnimatedSize);
    }

    private void updateColorAnimation() {
        if (isColorAnimating && colorAnimationProgress < colorAnimationDuration) {
            colorAnimationProgress++;

            float progress = (float) colorAnimationProgress / colorAnimationDuration;
            float easedProgress = easeInOutCubic(progress);

            // Интерполяция между начальным и целевым цветом
            int interpolatedColor = interpolateColor(startColor, targetColor, easedProgress);
            this.color = interpolatedColor;

            if (colorAnimationProgress >= colorAnimationDuration) {
                this.color = targetColor;
                isColorAnimating = false;
            }
        }
    }

    private int interpolateColor(int startColor, int targetColor, float progress) {
        int startR = (startColor >> 16) & 0xFF;
        int startG = (startColor >> 8) & 0xFF;
        int startB = startColor & 0xFF;
        int startA = (startColor >> 24) & 0xFF;

        int targetR = (targetColor >> 16) & 0xFF;
        int targetG = (targetColor >> 8) & 0xFF;
        int targetB = targetColor & 0xFF;
        int targetA = (targetColor >> 24) & 0xFF;

        int r = (int) (startR + (targetR - startR) * progress);
        int g = (int) (startG + (targetG - startG) * progress);
        int b = (int) (startB + (targetB - startB) * progress);
        int a = (int) (startA + (targetA - startA) * progress);

        return (a << 24) | (r << 16) | (g << 8) | b;
    }

    private float easeInOutSine(float x) {
        return (float) (-(Math.cos(Math.PI * x) - 1) / 2);
    }

    private float easeInOutCubic(float x) {
        return x < 0.5 ? 4 * x * x * x : 1 - (float)Math.pow(-2 * x + 2, 3) / 2;
    }

    private void updatePositionWithModifiers() {
        this.x = baseX;
        this.y = baseY;
        this.z = baseZ;

        List<StarModifierManager.StarModifier> modifiers = StarModifierManager.getModifiers(name);
        for (StarModifierManager.StarModifier modifier : modifiers) {
            if (modifier.isSmoothMove() && modifier.getType().equals("smooth_move")) {
                float progress = modifier.getProgress();
                float easedProgress = calculateEasing(progress, modifier.getEasingType());

                this.x = modifier.getStartX() + (modifier.getTargetX() - modifier.getStartX()) * easedProgress;
                this.y = modifier.getStartY() + (modifier.getTargetY() - modifier.getStartY()) * easedProgress;
                this.z = modifier.getStartZ() + (modifier.getTargetZ() - modifier.getStartZ()) * easedProgress;
            } else if (modifier.getType().equals("offset")) {
                this.x += modifier.getX();
                this.y += modifier.getY();
                this.z += modifier.getZ();
            } else if (modifier.getType().equals("player_position")) {
                this.y = modifier.getY();
            }
        }

        this.activeModifiers.clear();
        this.activeModifiers.addAll(modifiers);
    }

    private float calculateEasing(float progress, String easingType) {
        switch (easingType) {
            case "easeInCubic": return easeInCubic(progress);
            case "easeOutCubic": return easeOutCubic(progress);
            case "easeInOutCubic": return easeInOutCubic(progress);
            case "easeInExpo": return easeInExpo(progress);
            case "easeOutExpo": return easeOutExpo(progress);
            case "easeInBack": return easeInBack(progress);
            case "easeOutBack": return easeOutBack(progress);
            case "easeInElastic": return easeInElastic(progress);
            case "easeOutElastic": return easeOutElastic(progress);
            case "linear":
            default: return progress;
        }
    }

    // Easing functions
    private float linear(float x) { return x; }
    private float easeInCubic(float x) { return x * x * x; }
    private float easeOutCubic(float x) { return (float) (1 - Math.pow(1 - x, 3)); }
    private float easeInExpo(float x) {
        return x == 0 ? 0 : (float)Math.pow(2, 10 * x - 10);
    }
    private float easeOutExpo(float x) {
        return x == 1 ? 1 : 1 - (float)Math.pow(2, -10 * x);
    }
    private float easeInBack(float x) {
        float c1 = 1.70158f;
        float c3 = c1 + 1;
        return c3 * x * x * x - c1 * x * x;
    }
    private float easeOutBack(float x) {
        float c1 = 1.70158f;
        float c3 = c1 + 1;
        return 1 + c3 * (float)Math.pow(x - 1, 3) + c1 * (float)Math.pow(x - 1, 2);
    }
    private float easeInElastic(float x) {
        float c4 = (2 * (float)Math.PI) / 3;
        return x == 0 ? 0 : x == 1 ? 1 :
                -(float)Math.pow(2, 10 * x - 10) * (float)Math.sin((x * 10 - 10.75) * c4);
    }
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

    public void applySmoothMovement(float startX, float startY, float startZ,
                                    float targetX, float targetY, float targetZ,
                                    int duration, Runnable onExpire, String easingType) {
        StarModifierManager.StarModifier modifier = new StarModifierManager.StarModifier(
                "smooth_move", startX, startY, startZ, targetX, targetY, targetZ, duration, onExpire, easingType);
        StarModifierManager.addModifier(name, modifier);
        updatePositionWithModifiers();
    }

    // Новый метод для плавного изменения цвета
    public void startColorAnimation(int targetColor, int durationTicks) {
        this.startColor = this.color;
        this.targetColor = targetColor;
        this.colorAnimationDuration = durationTicks;
        this.colorAnimationProgress = 0;
        this.isColorAnimating = true;
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
        com.example.storytell.init.HologramConfig.setStarVisibility(name, visible);
    }

    public void restoreVisibility() {
        this.visible = this.wasVisibleBeforeHiding;
        com.example.storytell.init.HologramConfig.setStarVisibility(name, visible);
    }

    public void resetToDefaultVisibility() {
        this.visible = this.defaultVisible;
        this.wasVisibleBeforeHiding = this.defaultVisible;
        com.example.storytell.init.HologramConfig.setStarVisibility(name, defaultVisible);
    }

    public boolean getDefaultVisible() {
        return defaultVisible;
    }

    private void saveStarSettings() {
        com.example.storytell.init.HologramConfig.StarSettings settings =
                new com.example.storytell.init.HologramConfig.StarSettings(
                        visible, baseX, baseY, baseZ, rightAscension, declination, distance
                );
        com.example.storytell.init.HologramConfig.setStarSettings(name, settings);
    }

    public void removeFromConfig() {
        com.example.storytell.init.HologramConfig.removeStar(name);
    }

    // Измененный метод рендеринга модели - убраны параметры цвета
    private boolean renderJsonModel(com.mojang.blaze3d.vertex.PoseStack poseStack, float currentSize) {
        try {
            BakedModel model = Minecraft.getInstance().getModelManager().getModel(modelLocation);

            if (model == null || model == Minecraft.getInstance().getModelManager().getMissingModel()) {
                System.out.println("Model not found: " + modelLocation);
                return false;
            }

            // Setup rendering
            RenderSystem.setShader(GameRenderer::getPositionTexShader);
            RenderSystem.setShaderTexture(0, TextureAtlas.LOCATION_BLOCKS);

            // Apply scaling
            poseStack.pushPose();
            float scale = currentSize / 16.0f;
            poseStack.scale(scale, scale, scale);
            poseStack.translate(-0.5f, -0.5f, -0.5f);

            // Render the model - цвет теперь применяется через RenderSystem.setShaderColor
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

                    // Цвет теперь берется из текущего состояния RenderSystem
                    bufferBuilder.vertex(poseStack.last().pose(), x, y, z)
                            .uv(u, v)
                            .color(1.0f, 1.0f, 1.0f, 1.0f) // Белый цвет, чтобы не перезаписывать установленный шейдером
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

    // Измененный метод рендеринга fallback quad - убраны параметры цвета
    private void renderFallbackQuad(com.mojang.blaze3d.vertex.PoseStack poseStack, float currentSize) {
        Tesselator tesselator = Tesselator.getInstance();
        BufferBuilder bufferBuilder = tesselator.getBuilder();

        bufferBuilder.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_TEX_COLOR);

        Matrix4f matrix = poseStack.last().pose();
        float halfSize = currentSize / 2.0f;

        // Build simple quad - цвет применяется через шейдер
        bufferBuilder.vertex(matrix, -halfSize, -halfSize, 0).uv(0.0f, 0.0f).color(1.0f, 1.0f, 1.0f, 1.0f).endVertex();
        bufferBuilder.vertex(matrix, -halfSize, halfSize, 0).uv(0.0f, 1.0f).color(1.0f, 1.0f, 1.0f, 1.0f).endVertex();
        bufferBuilder.vertex(matrix, halfSize, halfSize, 0).uv(1.0f, 1.0f).color(1.0f, 1.0f, 1.0f, 1.0f).endVertex();
        bufferBuilder.vertex(matrix, halfSize, -halfSize, 0).uv(1.0f, 0.0f).color(1.0f, 1.0f, 1.0f, 1.0f).endVertex();

        BufferUploader.drawWithShader(bufferBuilder.end());
    }

    // Методы для изменения цвета
    public void setColor(int newColor) {
        this.color = newColor;
        this.isColorAnimating = false; // Останавливаем анимацию при прямом изменении цвета
    }

    public int getColor() {
        return color;
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