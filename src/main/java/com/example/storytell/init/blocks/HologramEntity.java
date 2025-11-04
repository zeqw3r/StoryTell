package com.example.storytell.init.blocks;

import com.example.storytell.init.HologramConfig;
import com.example.storytell.init.ModSounds;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;
import net.minecraftforge.network.NetworkHooks;
import net.minecraftforge.registries.RegistryObject;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class HologramEntity extends Entity {
    private static final Logger LOGGER = LogManager.getLogger();

    private static final EntityDataAccessor<String> DATA_TEXTURE =
            SynchedEntityData.defineId(HologramEntity.class, EntityDataSerializers.STRING);
    private static final EntityDataAccessor<String> DATA_DISPLAY_TEXT =
            SynchedEntityData.defineId(HologramEntity.class, EntityDataSerializers.STRING);
    private static final EntityDataAccessor<Float> DATA_ANIMATION_PROGRESS =
            SynchedEntityData.defineId(HologramEntity.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Boolean> DATA_IS_APPEARING =
            SynchedEntityData.defineId(HologramEntity.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Float> DATA_VERTICAL_OFFSET =
            SynchedEntityData.defineId(HologramEntity.class, EntityDataSerializers.FLOAT);

    private static final float ANIMATION_SPEED = 0.1F;
    private static final float VERTICAL_TRAVEL_DISTANCE = 1.0F;

    // Кэшированные значения для оптимизации
    private ResourceLocation cachedTexture;
    private String cachedText;
    private boolean configDirty = true;

    private boolean hasPlayedAppearSound = false;
    private boolean hasPlayedDisappearSound = false;
    private int ambientSoundTimer = 0;
    private boolean isAmbientSoundPlaying = false;
    private static final int AMBIENT_SOUND_INTERVAL = 20;

    // Поля для эффекта мерцания
    private float flickerIntensity = 0.0f;
    private int flickerTimer = 0;
    private static final int FLICKER_INTERVAL = 5;

    // Кэш для проверки изменений конфига
    private int lastConfigCheckTick = 0;
    private static final int CONFIG_CHECK_INTERVAL = 200; // Проверяем каждые 10 секунд вместо 100 тиков

    public HologramEntity(EntityType<?> type, Level world) {
        super(type, world);
        this.setInvulnerable(true);
        this.setNoGravity(true);
        this.entityData.set(DATA_ANIMATION_PROGRESS, 0.0F);
        this.entityData.set(DATA_IS_APPEARING, true);
        this.entityData.set(DATA_VERTICAL_OFFSET, VERTICAL_TRAVEL_DISTANCE);

        // Инициализация кэша
        cacheConfigValues();
    }

    @Override
    protected void defineSynchedData() {
        this.entityData.define(DATA_TEXTURE, getDefaultTexture().toString());
        this.entityData.define(DATA_DISPLAY_TEXT, "");
        this.entityData.define(DATA_ANIMATION_PROGRESS, 0.0F);
        this.entityData.define(DATA_IS_APPEARING, true);
        this.entityData.define(DATA_VERTICAL_OFFSET, VERTICAL_TRAVEL_DISTANCE);
    }

    private ResourceLocation getDefaultTexture() {
        return new ResourceLocation("storytell:textures/entity/default_hologram.png");
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag compound) {
        // Читаем базовые данные анимации
        if (compound.contains("AnimationProgress")) {
            setAnimationProgress(compound.getFloat("AnimationProgress"));
        }
        if (compound.contains("IsAppearing")) {
            setAppearing(compound.getBoolean("IsAppearing"));
        }
        if (compound.contains("VerticalOffset")) {
            setVerticalOffset(compound.getFloat("VerticalOffset"));
        }
        if (compound.contains("Texture")) {
            setTextureFromString(compound.getString("Texture"));
        } else {
            setTextureFromConfig();
        }
        if (compound.contains("Text")) {
            setDisplayText(compound.getString("Text"));
        } else {
            setTextFromConfig();
        }

        cacheConfigValues();
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag compound) {
        // Сохраняем базовые данные анимации
        compound.putFloat("AnimationProgress", getAnimationProgress());
        compound.putBoolean("IsAppearing", isAppearing());
        compound.putFloat("VerticalOffset", getVerticalOffset());
        compound.putString("Texture", this.entityData.get(DATA_TEXTURE));
        compound.putString("Text", this.entityData.get(DATA_DISPLAY_TEXT));
    }

    @Override
    public Packet<ClientGamePacketListener> getAddEntityPacket() {
        return NetworkHooks.getEntitySpawningPacket(this);
    }

    @Override
    public boolean isInvulnerableTo(net.minecraft.world.damagesource.DamageSource source) {
        return true;
    }

    @Override
    public boolean isPushable() {
        return false;
    }

    public void setTexture(ResourceLocation tex) {
        this.entityData.set(DATA_TEXTURE, tex.toString());
        this.cachedTexture = tex;
    }

    public void setTextureFromString(String texture) {
        try {
            setTexture(new ResourceLocation(texture));
        } catch (Exception e) {
            setTextureFromConfig();
        }
    }

    public ResourceLocation getTexture() {
        if (cachedTexture == null) {
            cacheConfigValues();
        }
        return cachedTexture;
    }

    private ResourceLocation getTextureFromConfig() {
        if (this.level() != null && this.level().isClientSide()) {
            return HologramConfig.getHologramTextureClient();
        } else {
            return HologramConfig.getHologramTexture();
        }
    }

    public void setTextureFromConfig() {
        setTexture(getTextureFromConfig());
    }

    public void setDisplayText(String text) {
        this.entityData.set(DATA_DISPLAY_TEXT, text);
        this.cachedText = text;
    }

    public void setTextFromConfig() {
        setDisplayText(getTextFromConfig());
    }

    private String getTextFromConfig() {
        if (this.level() != null && this.level().isClientSide()) {
            return HologramConfig.getHologramTextClient();
        } else {
            return HologramConfig.getHologramText();
        }
    }

    public String getDisplayText() {
        if (cachedText == null) {
            cacheConfigValues();
        }
        return cachedText != null ? cachedText : "";
    }

    public float getAnimationProgress() {
        return this.entityData.get(DATA_ANIMATION_PROGRESS);
    }

    public void setAnimationProgress(float progress) {
        this.entityData.set(DATA_ANIMATION_PROGRESS, progress);
    }

    public float getVerticalOffset() {
        return this.entityData.get(DATA_VERTICAL_OFFSET);
    }

    public void setVerticalOffset(float offset) {
        this.entityData.set(DATA_VERTICAL_OFFSET, offset);
    }

    public boolean isAppearing() {
        return this.entityData.get(DATA_IS_APPEARING);
    }

    public void setAppearing(boolean appearing) {
        this.entityData.set(DATA_IS_APPEARING, appearing);
    }

    public void startDisappearing() {
        this.hasPlayedDisappearSound = false;
        this.entityData.set(DATA_IS_APPEARING, false);
        ambientSoundTimer = 0;
        isAmbientSoundPlaying = false;
    }

    private void playHologramSound(SoundEvent sound, float volume) {
        Level level = this.level();
        if (level != null && !level.isClientSide()) {
            if (sound == null) {
                return;
            }

            level.playSound(null, this.getX(), this.getY(), this.getZ(), sound, SoundSource.BLOCKS, volume, 1.0F);
        }
    }

    private SoundEvent getSafeSoundEvent(RegistryObject<SoundEvent> soundEvent) {
        try {
            return soundEvent != null ? soundEvent.get() : null;
        } catch (IllegalStateException e) {
            return null;
        }
    }

    private SoundEvent getAmbientSound() {
        if (this.level() != null && this.level().isClientSide()) {
            return HologramConfig.getHologramAmbientSoundClient();
        } else {
            return HologramConfig.getHologramAmbientSound();
        }
    }

    @Override
    public void tick() {
        super.tick();
        this.setDeltaMovement(0, 0, 0);

        float currentProgress = getAnimationProgress();
        float currentOffset = getVerticalOffset();

        updateFlickerEffect();

        // Воспроизведение звука появления
        if (isAppearing() && !hasPlayedAppearSound && currentProgress > 0.3F) {
            SoundEvent appearSound = getSafeSoundEvent(ModSounds.HOLOGRAM_APPEAR);
            if (appearSound != null) {
                playHologramSound(appearSound, 0.8F);
            }
            hasPlayedAppearSound = true;
            ambientSoundTimer = AMBIENT_SOUND_INTERVAL - 5;
            isAmbientSoundPlaying = true;
        }

        if (isAppearing()) {
            if (currentProgress < 1.0F) {
                setAnimationProgress(currentProgress + ANIMATION_SPEED);
            } else {
                setAnimationProgress(1.0F);
            }

            if (currentOffset > 0.0F) {
                setVerticalOffset(Math.max(0.0F, currentOffset - ANIMATION_SPEED * VERTICAL_TRAVEL_DISTANCE));
            } else {
                setVerticalOffset(0.0F);
            }
        } else {
            // Воспроизведение звука исчезновения
            if (!hasPlayedDisappearSound && currentProgress > 0.5F) {
                SoundEvent disappearSound = getSafeSoundEvent(ModSounds.HOLOGRAM_DISAPPEAR);
                if (disappearSound != null) {
                    playHologramSound(disappearSound, 0.8F);
                }
                hasPlayedDisappearSound = true;
            }

            if (currentProgress > 0.0F) {
                setAnimationProgress(currentProgress - ANIMATION_SPEED);
            } else {
                setAnimationProgress(0.0F);
            }

            if (currentOffset < VERTICAL_TRAVEL_DISTANCE) {
                setVerticalOffset(Math.min(VERTICAL_TRAVEL_DISTANCE, currentOffset + ANIMATION_SPEED * VERTICAL_TRAVEL_DISTANCE));
            } else {
                setVerticalOffset(VERTICAL_TRAVEL_DISTANCE);
            }

            if (currentProgress <= 0.0F && currentOffset >= VERTICAL_TRAVEL_DISTANCE) {
                this.discard();
                return; // Прерываем выполнение после удаления
            }
        }

        // Воспроизведение ambient sound с безопасной проверкой
        if (isAmbientSoundPlaying && currentProgress >= 1.0F && isAppearing()) {
            ambientSoundTimer++;
            if (ambientSoundTimer >= AMBIENT_SOUND_INTERVAL) {
                SoundEvent ambientSound = getAmbientSound();
                if (ambientSound != null) {
                    playHologramSound(ambientSound, 0.12F);
                }
                ambientSoundTimer = 0;
            }
        } else if (!isAppearing()) {
            isAmbientSoundPlaying = false;
            ambientSoundTimer = 0;
        }

        // Оптимизированная проверка конфига - реже и только если помечен как грязный
        if (configDirty || (this.tickCount - lastConfigCheckTick >= CONFIG_CHECK_INTERVAL)) {
            synchronizeWithConfig();
            lastConfigCheckTick = this.tickCount;
            configDirty = false;
        }
    }

    private void cacheConfigValues() {
        this.cachedTexture = getTextureFromConfig();
        this.cachedText = getTextFromConfig();
    }

    private void synchronizeWithConfig() {
        ResourceLocation currentTexture = getTexture();
        ResourceLocation configTexture = getTextureFromConfig();
        String currentText = getDisplayText();
        String configText = getTextFromConfig();

        // Если текстура не совпадает с конфигом, обновляем ее
        if (!currentTexture.equals(configTexture)) {
            setTextureFromConfig();
        }

        // Если текст не совпадает с конфигом, обновляем его
        if (!currentText.equals(configText)) {
            setTextFromConfig();
        }
    }

    private void updateFlickerEffect() {
        flickerTimer++;
        if (flickerTimer >= FLICKER_INTERVAL) {
            flickerIntensity = (float) (Math.random() * 0.1f);
            flickerTimer = 0;
        }
    }

    public float getFlickerIntensity() {
        return flickerIntensity;
    }

    // Метод для принудительного обновления из конфига
    public void forceConfigUpdate() {
        configDirty = true;
        cacheConfigValues();
        setTextureFromConfig();
        setTextFromConfig();
    }

    // Метод для проверки состояния блокировки
    public boolean isLocked() {
        if (this.level() != null && this.level().isClientSide()) {
            return HologramConfig.isHologramLockedClient();
        } else {
            return HologramConfig.isHologramLocked();
        }
    }

}