// HologramEntity.java
package com.example.storytell.init.blocks;

import com.example.storytell.init.HologramConfig;
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
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class HologramEntity extends Entity {
    private static final Logger LOGGER = LogManager.getLogger();

    private static final EntityDataAccessor<String> DATA_TEXTURE =
            SynchedEntityData.defineId(HologramEntity.class, EntityDataSerializers.STRING);
    private static final EntityDataAccessor<Float> DATA_ANIMATION_PROGRESS =
            SynchedEntityData.defineId(HologramEntity.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Boolean> DATA_IS_APPEARING =
            SynchedEntityData.defineId(HologramEntity.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Float> DATA_VERTICAL_OFFSET =
            SynchedEntityData.defineId(HologramEntity.class, EntityDataSerializers.FLOAT);

    private static final float ANIMATION_SPEED = 0.2F;
    private static final float VERTICAL_TRAVEL_DISTANCE = 1.0F;

    private boolean hasPlayedAppearSound = false;
    private boolean hasPlayedDisappearSound = false;
    private int ambientSoundTimer = 0;
    private boolean isAmbientSoundPlaying = false;
    private static final int AMBIENT_SOUND_INTERVAL = 20; // 1 секунда (20 тиков)

    public HologramEntity(EntityType<?> type, Level world) {
        super(type, world);
        this.setInvulnerable(true);
        this.setNoGravity(true);
        this.entityData.set(DATA_ANIMATION_PROGRESS, 0.0F);
        this.entityData.set(DATA_IS_APPEARING, true);
        this.entityData.set(DATA_VERTICAL_OFFSET, VERTICAL_TRAVEL_DISTANCE);

        setTextureFromConfig();
    }

    @Override
    protected void defineSynchedData() {
        this.entityData.define(DATA_TEXTURE, HologramConfig.getHologramTexture().toString());
        this.entityData.define(DATA_ANIMATION_PROGRESS, 0.0F);
        this.entityData.define(DATA_IS_APPEARING, true);
        this.entityData.define(DATA_VERTICAL_OFFSET, VERTICAL_TRAVEL_DISTANCE);
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag compound) {
        setTextureFromConfig();
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag compound) {
        // Не сохраняем текстуру в NBT, всегда используем из конфига
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
    }

    public void setTextureFromString(String texture) {
        try {
            setTexture(new ResourceLocation(texture));
        } catch (Exception e) {
            setTextureFromConfig();
        }
    }

    public ResourceLocation getTexture() {
        try {
            return new ResourceLocation(this.entityData.get(DATA_TEXTURE));
        } catch (Exception e) {
            return HologramConfig.getHologramTexture();
        }
    }

    public void setTextureFromConfig() {
        setTexture(HologramConfig.getHologramTexture());
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
        this.entityData.set(DATA_IS_APPEARING, false);
        // Сбрасываем таймер эмбиент-звука при начале исчезновения
        ambientSoundTimer = 0;
        isAmbientSoundPlaying = false;
    }

    private void playHologramSound(SoundEvent sound, float volume) {
        Level level = this.level();
        if (level != null && !level.isClientSide()) {
            // Проверяем, что звук не null
            if (sound == null) {
                LOGGER.error("Custom SoundEvent is null! Check ModSounds registration.");
                return;
            }

            // Воспроизводим кастомный звук
            level.playSound(null, this.getX(), this.getY(), this.getZ(), sound, SoundSource.BLOCKS, volume, 1.0F);
        }
    }

    @Override
    public void tick() {
        super.tick();
        this.setDeltaMovement(0, 0, 0);

        float currentProgress = getAnimationProgress();
        float currentOffset = getVerticalOffset();

        // Воспроизведение звука появления - КАСТОМНЫЙ ЗВУК
        if (isAppearing() && !hasPlayedAppearSound && currentProgress > 0.3F) {
            playHologramSound(ModSounds.HOLOGRAM_APPEAR.get(), 0.8F);
            hasPlayedAppearSound = true;

            // Сразу запускаем эмбиент-звук после звука появления
            ambientSoundTimer = AMBIENT_SOUND_INTERVAL - 5; // Почти сразу воспроизведем
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
            // Воспроизведение звука исчезновения - КАСТОМНЫЙ ЗВУК
            if (!hasPlayedDisappearSound && currentProgress > 0.5F) {
                playHologramSound(ModSounds.HOLOGRAM_DISAPPEAR.get(), 0.8F);
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
            }
        }

        // Воспроизведение постоянного звука работы - КАСТОМНЫЙ ЗВУК
        // Звук воспроизводится бесшовно сразу после появления
        if (isAmbientSoundPlaying && currentProgress >= 1.0F && isAppearing()) {
            ambientSoundTimer++;
            if (ambientSoundTimer >= AMBIENT_SOUND_INTERVAL) {
                playHologramSound(ModSounds.HOLOGRAM_AMBIENT.get(), 0.12F); // Еще тише
                ambientSoundTimer = 0;
            }
        } else if (!isAppearing()) {
            // Если голограмма начинает исчезать, останавливаем эмбиент-звук
            isAmbientSoundPlaying = false;
            ambientSoundTimer = 0;
        }

        // Периодически обновляем текстуру из конфига (раз в секунду)
        if (this.tickCount % 20 == 0) {
            setTextureFromConfig();
        }
    }
}