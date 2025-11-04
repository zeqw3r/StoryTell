package com.example.storytell.init.radio;

import com.example.storytell.init.HologramConfig;
import com.example.storytell.init.ModSounds;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.registries.ForgeRegistries;

public class RadioBlockEntity extends BlockEntity {
    private boolean enabled = false;
    private int soundCooldown = 0;
    private boolean isBroken = false;
    private String currentPlayingSound = ""; // Текущий воспроизводимый звук

    // Интервалы воспроизведения звуков (в тиках)
    private static final int STATIC_INTERVAL = 20; // 1 секунда для статики
    private static final int OTHER_SOUND_INTERVAL = 6000; // 5 минут (300 секунд) для других звуков

    // Громкость звуков
    private static final float STATIC_VOLUME = 0.5f; // Тише обычного
    private static final float OTHER_VOLUME = 1.0f; // Обычная громкость
    private static final float ACTION_VOLUME = 1.0f; // Громкость для звуков включения/выключения

    public RadioBlockEntity(BlockPos pos, BlockState state) {
        super(ModRadioBlockEntities.RADIO_BLOCK_ENTITY_TYPE.get(), pos, state);
    }

    public static void tick(Level level, BlockPos pos, BlockState state, RadioBlockEntity be) {
        if (level == null) return;

        if (!level.isClientSide) {
            be.tickServer();
        }
    }

    public void tickServer() {
        // Если радио сломано или выключено - не воспроизводим звуки
        if (isBroken || !enabled) return;

        if (soundCooldown <= 0) {
            playRadioSound();

            // Устанавливаем следующий интервал в зависимости от типа звука
            String currentSound = HologramConfig.getRadioSound();
            if ("storytell:radio_static".equals(currentSound)) {
                soundCooldown = STATIC_INTERVAL; // Непрерывное воспроизведение каждую секунду
            } else {
                soundCooldown = OTHER_SOUND_INTERVAL; // Раз в 5 минут для других звуков
            }
        } else {
            soundCooldown--;
        }
    }

    public void playRadioSound() {
        if (level == null || isBroken) return;

        String soundName = HologramConfig.getRadioSound();
        SoundEvent soundEvent = null;
        float volume = OTHER_VOLUME;

        // Безопасный способ получения звуков
        try {
            // Сначала пробуем получить звук из реестра напрямую
            soundEvent = ForgeRegistries.SOUND_EVENTS.getValue(new net.minecraft.resources.ResourceLocation(soundName));

            // Если звук не найден, пробуем использовать наши зарегистрированные звуки
            if (soundEvent == null) {
                if ("storytell:radio_static".equals(soundName)) {
                    if (ModSounds.RADIO_STATIC != null) {
                        soundEvent = ModSounds.RADIO_STATIC.get();
                        volume = STATIC_VOLUME; // Устанавливаем меньшую громкость для статики
                    }
                }
            } else {
                // Если звук найден в реестре, проверяем, не статический ли это
                if ("storytell:radio_static".equals(soundName)) {
                    volume = STATIC_VOLUME;
                }
            }
        } catch (Exception e) {
            System.err.println("Error getting radio sound: " + e.getMessage());
        }

        if (soundEvent != null) {
            level.playSound(null, worldPosition, soundEvent, SoundSource.RECORDS, volume, 1.0f);
            currentPlayingSound = soundName; // Запоминаем текущий звук
        } else {
            System.err.println("Radio sound not found: " + soundName);
            // Пробуем использовать звук по умолчанию
            try {
                SoundEvent defaultSound = ForgeRegistries.SOUND_EVENTS.getValue(new net.minecraft.resources.ResourceLocation("storytell:radio_static"));
                if (defaultSound != null) {
                    level.playSound(null, worldPosition, defaultSound, SoundSource.RECORDS, STATIC_VOLUME, 1.0f);
                    currentPlayingSound = "storytell:radio_static";
                }
            } catch (Exception e) {
                System.err.println("Could not play default radio sound either");
            }
        }
    }

    // Метод для остановки звука
    private void stopCurrentSound() {
        if (level == null || level.isClientSide || currentPlayingSound.isEmpty()) return;

        // Формируем и выполняем команду stopsound
        String command = "stopsound @a * " + currentPlayingSound;
        level.getServer().getCommands().performPrefixedCommand(
                level.getServer().createCommandSourceStack().withSuppressedOutput().withPermission(4),
                command
        );

        currentPlayingSound = ""; // Сбрасываем текущий звук
    }

    public void setEnabled(boolean enabled) {
        if (this.enabled == enabled) return; // Если состояние не изменилось, ничего не делаем

        boolean wasEnabled = this.enabled;
        this.enabled = enabled;
        setChanged();

        if (level != null && !level.isClientSide && !isBroken) {
            if (enabled && !wasEnabled) {
                // Воспроизводим звук включения
                level.playSound(null, worldPosition, ModSounds.RADIO_ENABLE.get(), SoundSource.RECORDS, ACTION_VOLUME, 1.0f);
                // Сбрасываем таймер, чтобы звук начал воспроизводиться сразу
                soundCooldown = 0;
            } else if (!enabled && wasEnabled) {
                // Воспроизводим звук выключения
                level.playSound(null, worldPosition, ModSounds.RADIO_DISABLE.get(), SoundSource.RECORDS, ACTION_VOLUME, 1.0f);
                // Останавливаем текущий звук радио
                stopCurrentSound();
                soundCooldown = 0;
            }
        }

        if (!enabled) {
            soundCooldown = 0;
        }
    }

    public void setBroken(boolean broken) {
        this.isBroken = broken;
        setChanged();

        if (broken && enabled) {
            // Если радио сломано во время работы, воспроизводим звук выключения
            if (level != null && !level.isClientSide) {
                level.playSound(null, worldPosition, ModSounds.RADIO_DISABLE.get(), SoundSource.RECORDS, ACTION_VOLUME, 1.0f);
                // Останавливаем текущий звук радио
                stopCurrentSound();
            }
            enabled = false;
            soundCooldown = 0;
        }
    }

    public boolean isEnabled() {
        return enabled && !isBroken;
    }

    public boolean isBroken() {
        return isBroken;
    }

    @Override
    protected void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
        tag.putBoolean("Enabled", enabled);
        tag.putBoolean("IsBroken", isBroken);
        tag.putInt("SoundCooldown", soundCooldown);
        tag.putString("CurrentPlayingSound", currentPlayingSound);
    }

    @Override
    public void load(CompoundTag tag) {
        super.load(tag);
        enabled = tag.getBoolean("Enabled");
        isBroken = tag.getBoolean("IsBroken");
        soundCooldown = tag.getInt("SoundCooldown");
        currentPlayingSound = tag.getString("CurrentPlayingSound");
    }
}