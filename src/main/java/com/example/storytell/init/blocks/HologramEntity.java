// HologramEntity.java
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
    private static final int AMBIENT_SOUND_INTERVAL = 20;

    // Поля для эффекта мерцания
    private float flickerIntensity = 0.0f;
    private int flickerTimer = 0;
    private static final int FLICKER_INTERVAL = 5;

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
        if (compound.contains("Texture")) {
            setTextureFromString(compound.getString("Texture"));
        } else {
            setTextureFromConfig();
        }
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag compound) {
        compound.putString("Texture", this.entityData.get(DATA_TEXTURE));
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
        this.hasPlayedDisappearSound = false;
        this.entityData.set(DATA_IS_APPEARING, false);
        ambientSoundTimer = 0;
        isAmbientSoundPlaying = false;
    }

    private void playHologramSound(SoundEvent sound, float volume) {
        Level level = this.level();
        if (level != null && !level.isClientSide()) {
            if (sound == null) {
                LOGGER.warn("Attempted to play null sound event");
                return;
            }

            level.playSound(null, this.getX(), this.getY(), this.getZ(), sound, SoundSource.BLOCKS, volume, 1.0F);
        }
    }

    private SoundEvent getSafeSoundEvent(RegistryObject<SoundEvent> soundEvent) {
        try {
            return soundEvent != null ? soundEvent.get() : null;
        } catch (IllegalStateException e) {
            LOGGER.warn("Sound event not ready: " + e.getMessage());
            return null;
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
            }
        }

        // Воспроизведение ambient sound с безопасной проверкой
        if (isAmbientSoundPlaying && currentProgress >= 1.0F && isAppearing()) {
            ambientSoundTimer++;
            if (ambientSoundTimer >= AMBIENT_SOUND_INTERVAL) {
                SoundEvent ambientSound = HologramConfig.getHologramAmbientSound();
                if (ambientSound == null) {
                    // Fallback на стандартный ambient sound
                    ambientSound = getSafeSoundEvent(ModSounds.HOLOGRAM_AMBIENT);
                }

                if (ambientSound != null) {
                    playHologramSound(ambientSound, 0.12F);
                }
                ambientSoundTimer = 0;
            }
        } else if (!isAppearing()) {
            isAmbientSoundPlaying = false;
            ambientSoundTimer = 0;
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
}