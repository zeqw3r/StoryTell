// UpgradeTransmitterBlockEntity.java
package com.example.storytell.init.blocks;

import com.example.storytell.init.HologramConfig;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.entity.Entity;
import net.minecraft.server.level.ServerLevel;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.energy.IEnergyStorage;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraft.core.Direction;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.UUID;

public class UpgradeTransmitterBlockEntity extends BlockEntity {

    private static final int ENERGY_PER_TICK = 400;
    private UUID hologramUUID = null;
    private boolean hasEnergy = false;
    private int energyStored = 0;
    private static final int MAX_ENERGY = ENERGY_PER_TICK * 100;

    // Поля для отслеживания изменений конфига
    private String lastHologramText = "";
    private String lastHologramTexture = "";
    private boolean configChanged = false;

    private boolean lastEnergyRequiredState = HologramConfig.isEnergyRequired();
    private int tickCounter = 0;
    private static final int ENERGY_CHECK_INTERVAL = 10;

    public UpgradeTransmitterBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.UPGRADE_TRANSMITTER_BLOCK_ENTITY_TYPE.get(), pos, state);
        // Инициализируем значения из конфига
        this.lastHologramText = HologramConfig.getHologramText();
        this.lastHologramTexture = HologramConfig.getHologramTexture().toString();
    }

    public static void tick(Level level, BlockPos pos, BlockState state, UpgradeTransmitterBlockEntity be) {
        if (level == null || level.isClientSide) return;
        be.tickServer();
    }

    public void tickServer() {
        if (this.level == null || this.level.isClientSide) return;

        tickCounter++;
        boolean checkEnergyThisTick = (tickCounter % ENERGY_CHECK_INTERVAL == 0);

        // Проверяем изменение конфига
        checkConfigChanges();

        boolean currentEnergyRequired = HologramConfig.isEnergyRequired();
        if (currentEnergyRequired != lastEnergyRequiredState) {
            lastEnergyRequiredState = currentEnergyRequired;
            checkEnergyThisTick = true;
        }

        if (configChanged) {
            updateHologramForConfigChange();
            configChanged = false;
        }

        if (!checkEnergyThisTick) return;

        if (currentEnergyRequired) {
            LazyOptional<IEnergyStorage> capability = this.getCapability(ForgeCapabilities.ENERGY, null);
            if (capability.isPresent()) {
                IEnergyStorage energy = capability.orElse(null);
                if (energy != null && energy.getEnergyStored() >= ENERGY_PER_TICK) {
                    energy.extractEnergy(ENERGY_PER_TICK, false);
                    hasEnergy = true;
                    energyStored = energy.getEnergyStored();
                    spawnOrUpdateHologram();
                } else {
                    hasEnergy = false;
                    energyStored = energy != null ? energy.getEnergyStored() : 0;
                    startHologramDisappearing();
                }
            } else {
                hasEnergy = false;
                energyStored = 0;
                startHologramDisappearing();
            }
        } else {
            hasEnergy = true;
            energyStored = MAX_ENERGY;
            spawnOrUpdateHologram();
        }
    }

    private void checkConfigChanges() {
        String currentText = HologramConfig.getHologramText();
        String currentTexture = HologramConfig.getHologramTexture().toString();

        if (!currentText.equals(lastHologramText) || !currentTexture.equals(lastHologramTexture)) {
            configChanged = true;
            lastHologramText = currentText;
            lastHologramTexture = currentTexture;
        }
    }

    private void updateHologramForConfigChange() {
        // Запускаем анимацию исчезновения текущей голограммы
        startHologramDisappearing();
        hologramUUID = null;

        // Если есть энергия, создаем новую голограмму с обновленными настройками
        if (hasEnergy || !HologramConfig.isEnergyRequired()) {
            spawnNewHologram();
        }
    }

    private void spawnOrUpdateHologram() {
        boolean canShowHologram = !HologramConfig.isEnergyRequired() || (hasEnergy && energyStored >= ENERGY_PER_TICK);

        if (!canShowHologram) {
            startHologramDisappearing();
            return;
        }

        if (this.level == null || this.level.isClientSide || !(this.level instanceof ServerLevel)) return;
        ServerLevel server = (ServerLevel) this.level;

        if (hologramUUID != null) {
            Entity existing = server.getEntity(hologramUUID);
            if (existing != null && existing.isAlive()) {
                return;
            } else {
                hologramUUID = null;
            }
        }

        spawnNewHologram();
    }

    private void spawnNewHologram() {
        if (this.level == null || this.level.isClientSide || !(this.level instanceof ServerLevel)) return;
        ServerLevel server = (ServerLevel) this.level;

        try {
            HologramEntity holo = new HologramEntity(com.example.storytell.init.ModEntities.HOLOGRAM_ENTITY.get(), level);
            BlockPos pos = getBlockPos();
            holo.moveTo(pos.getX() + 0.5, pos.getY() + 1.0, pos.getZ() + 0.5, 0.0F, 0.0F);

            // Устанавливаем текущие настройки из конфига
            holo.setDisplayText(lastHologramText);
            holo.setTextureFromString(lastHologramTexture);

            server.addFreshEntity(holo);
            hologramUUID = holo.getUUID();
            setChanged();
        } catch (Exception e) {
            System.err.println("Failed to spawn hologram: " + e.getMessage());
        }
    }

    private void startHologramDisappearing() {
        if (this.level == null || this.level.isClientSide || !(this.level instanceof ServerLevel)) return;
        ServerLevel server = (ServerLevel) this.level;

        if (hologramUUID != null) {
            Entity e = server.getEntity(hologramUUID);
            if (e instanceof HologramEntity) {
                ((HologramEntity) e).startDisappearing();
            }
        }
    }

    public void removeHologram() {
        if (this.level == null || this.level.isClientSide || !(this.level instanceof ServerLevel)) return;
        ServerLevel server = (ServerLevel) this.level;

        if (hologramUUID != null) {
            Entity e = server.getEntity(hologramUUID);
            if (e != null) e.discard();
            hologramUUID = null;
            setChanged();
        }
    }

    public void forceSpawnHologram() {
        if (this.level == null || this.level.isClientSide) return;
        removeHologram();
        if ((hasEnergy && energyStored >= ENERGY_PER_TICK) || !HologramConfig.isEnergyRequired()) {
            spawnNewHologram();
        }
    }

    public boolean isShowingHologram() {
        boolean energyCondition = !HologramConfig.isEnergyRequired() || (hasEnergy && energyStored >= ENERGY_PER_TICK);
        return energyCondition && hologramUUID != null;
    }

    @Override
    public void setRemoved() {
        removeHologram();
        super.setRemoved();
    }

    @Override
    public void load(CompoundTag tag) {
        super.load(tag);
        if (tag.contains("HologramUUID")) {
            try {
                hologramUUID = UUID.fromString(tag.getString("HologramUUID"));
            } catch (Exception ex) {
                hologramUUID = null;
            }
        }
        hasEnergy = tag.getBoolean("HasEnergy");
        energyStored = tag.getInt("EnergyStored");
        lastEnergyRequiredState = HologramConfig.isEnergyRequired();

        // Загружаем last-значения
        if (tag.contains("LastHologramText")) {
            lastHologramText = tag.getString("LastHologramText");
        }
        if (tag.contains("LastHologramTexture")) {
            lastHologramTexture = tag.getString("LastHologramTexture");
        }
    }

    @Override
    public void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
        if (hologramUUID != null) {
            tag.putString("HologramUUID", hologramUUID.toString());
        }
        tag.putBoolean("HasEnergy", hasEnergy);
        tag.putInt("EnergyStored", energyStored);

        // Сохраняем last-значения
        tag.putString("LastHologramText", lastHologramText);
        tag.putString("LastHologramTexture", lastHologramTexture);
    }

    public void processCommand(String command) {
        if (this.level == null || this.level.isClientSide) return;
        if (HologramConfig.isHologramLocked()) return;

        String textResponse = HologramManager.processCommand(command);
        if (textResponse != null) {
            // Обновляем конфиг
            HologramConfig.setHologramTexture("storytell:textures/entity/default_hologram.png");
            HologramConfig.setHologramText(textResponse);

            // Синхронизируем с клиентами
            syncConfigWithClients();
        }
    }

    private void syncConfigWithClients() {
        com.example.storytell.init.network.SyncHologramTexturePacket texturePacket =
                new com.example.storytell.init.network.SyncHologramTexturePacket(HologramConfig.getHologramTexture().toString());
        com.example.storytell.init.network.SyncHologramTextPacket textPacket =
                new com.example.storytell.init.network.SyncHologramTextPacket(HologramConfig.getHologramText());
        com.example.storytell.init.network.SyncHologramAmbientSoundPacket soundPacket =
                new com.example.storytell.init.network.SyncHologramAmbientSoundPacket(HologramConfig.getHologramAmbientSoundLocation());
        com.example.storytell.init.network.SyncHologramLockPacket lockPacket =
                new com.example.storytell.init.network.SyncHologramLockPacket(HologramConfig.isHologramLocked());

        com.example.storytell.init.network.NetworkHandler.INSTANCE.send(
                net.minecraftforge.network.PacketDistributor.ALL.noArg(), texturePacket);
        com.example.storytell.init.network.NetworkHandler.INSTANCE.send(
                net.minecraftforge.network.PacketDistributor.ALL.noArg(), textPacket);
        com.example.storytell.init.network.NetworkHandler.INSTANCE.send(
                net.minecraftforge.network.PacketDistributor.ALL.noArg(), soundPacket);
        com.example.storytell.init.network.NetworkHandler.INSTANCE.send(
                net.minecraftforge.network.PacketDistributor.ALL.noArg(), lockPacket);
    }

    private final LazyOptional<IEnergyStorage> energyHandler = LazyOptional.of(() -> new IEnergyStorage() {
        @Override public int receiveEnergy(int maxReceive, boolean simulate) {
            int energyReceived = Math.min(MAX_ENERGY - energyStored, maxReceive);
            if (!simulate) {
                energyStored += energyReceived;
                setChanged();
            }
            return energyReceived;
        }

        @Override public int extractEnergy(int maxExtract, boolean simulate) {
            if (!HologramConfig.isEnergyRequired()) {
                return Math.min(maxExtract, ENERGY_PER_TICK);
            }
            int energyExtracted = Math.min(energyStored, Math.min(maxExtract, ENERGY_PER_TICK));
            if (!simulate) {
                energyStored -= energyExtracted;
                setChanged();
            }
            return energyExtracted;
        }

        @Override public int getEnergyStored() {
            return energyStored;
        }

        @Override public int getMaxEnergyStored() {
            return MAX_ENERGY;
        }

        @Override public boolean canExtract() {
            return true;
        }

        @Override public boolean canReceive() {
            return true;
        }
    });

    @Nonnull
    @Override
    public <T> LazyOptional<T> getCapability(@Nonnull Capability<T> cap, @Nullable Direction side) {
        if (cap == ForgeCapabilities.ENERGY) {
            return energyHandler.cast();
        }
        return super.getCapability(cap, side);
    }

    @Override
    public void invalidateCaps() {
        super.invalidateCaps();
        energyHandler.invalidate();
    }
}