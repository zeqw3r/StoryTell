// UpgradeTransmitterBlockEntity.java
package com.example.storytell.init.blocks;

import com.example.storytell.init.HologramConfig;
import com.example.storytell.init.ModEntities;
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

    // Поля для управления сменой голограммы
    private boolean isChangingText = false;
    private String pendingText = "";
    private int changeCooldown = 0;

    public UpgradeTransmitterBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.UPGRADE_TRANSMITTER_BLOCK_ENTITY_TYPE.get(), pos, state);
    }

    public static void tick(Level level, BlockPos pos, BlockState state, UpgradeTransmitterBlockEntity be) {
        if (level == null || level.isClientSide) return;
        be.tickServer();
    }

    public void tickServer() {
        if (this.level == null || this.level.isClientSide) return;

        // Обработка смены текстуры
        if (isChangingText) {
            if (changeCooldown > 0) {
                changeCooldown--;
                return;
            } else {
                HologramConfig.setHologramText(pendingText);
                HologramConfig.setHologramTexture("storytell:textures/entity/default.png");
                isChangingText = false;
                pendingText = "";

                if ((hasEnergy && energyStored >= ENERGY_PER_TICK) || !HologramConfig.isEnergyRequired()) {
                    spawnNewHologram();
                }
            }
        }

        boolean energyRequired = HologramConfig.isEnergyRequired();

        if (energyRequired) {
            LazyOptional<IEnergyStorage> capability = this.getCapability(ForgeCapabilities.ENERGY, null);
            if (capability.isPresent()) {
                IEnergyStorage energy = capability.orElse(null);
                if (energy != null) {
                    if (energy.getEnergyStored() >= ENERGY_PER_TICK) {
                        energy.extractEnergy(ENERGY_PER_TICK, false);
                        hasEnergy = true;
                        energyStored = energy.getEnergyStored();

                        if (!isChangingText) {
                            spawnOrUpdateHologram();
                        }
                    } else {
                        hasEnergy = false;
                        energyStored = energy.getEnergyStored();
                        startHologramDisappearing();
                    }
                } else {
                    hasEnergy = false;
                    energyStored = 0;
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

            if (!isChangingText) {
                spawnOrUpdateHologram();
            }
        }
    }

    private void spawnOrUpdateHologram() {
        boolean canShowHologram = !HologramConfig.isEnergyRequired() || (hasEnergy && energyStored >= ENERGY_PER_TICK);

        if (!canShowHologram) {
            startHologramDisappearing();
            return;
        }

        if (this.level == null || this.level.isClientSide) return;
        if (!(this.level instanceof ServerLevel)) return;
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
        if (this.level == null || this.level.isClientSide) return;
        if (!(this.level instanceof ServerLevel)) return;
        ServerLevel server = (ServerLevel) this.level;

        try {
            HologramEntity holo = new HologramEntity(ModEntities.HOLOGRAM_ENTITY.get(), level);
            BlockPos pos = getBlockPos();
            holo.moveTo(pos.getX() + 0.5, pos.getY() + 1.0, pos.getZ() + 0.5, 0.0F, 0.0F);

            server.addFreshEntity(holo);
            hologramUUID = holo.getUUID();
            setChanged();
        } catch (Exception e) {
            System.err.println("Failed to spawn hologram: " + e.getMessage());
        }
    }

    private void startHologramDisappearing() {
        if (this.level == null || this.level.isClientSide) return;
        if (!(this.level instanceof ServerLevel)) return;
        ServerLevel server = (ServerLevel) this.level;

        if (hologramUUID != null) {
            Entity e = server.getEntity(hologramUUID);
            if (e instanceof HologramEntity) {
                ((HologramEntity) e).startDisappearing();
            }
        }
    }

    public void removeHologram() {
        if (this.level == null || this.level.isClientSide) return;
        if (!(this.level instanceof ServerLevel)) return;
        ServerLevel server = (ServerLevel) this.level;

        if (hologramUUID != null) {
            Entity e = server.getEntity(hologramUUID);
            if (e != null) {
                e.discard();
            }
            hologramUUID = null;
            setChanged();
        }
    }

    public boolean isShowingHologram() {
        boolean energyCondition = HologramConfig.isEnergyRequired() ?
                (hasEnergy && energyStored >= ENERGY_PER_TICK) : true;

        return energyCondition && hologramUUID != null && !isChangingText;
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
        isChangingText = tag.getBoolean("IsChangingTexture");
        pendingText = tag.getString("PendingText");
        changeCooldown = tag.getInt("ChangeCooldown");
    }

    @Override
    public void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
        if (hologramUUID != null) {
            tag.putString("HologramUUID", hologramUUID.toString());
        }
        tag.putBoolean("HasEnergy", hasEnergy);
        tag.putInt("EnergyStored", energyStored);
        tag.putBoolean("IsChangingTexture", isChangingText);
        tag.putString("PendingText", pendingText);
        tag.putInt("ChangeCooldown", changeCooldown);
    }

    public void processCommand(String command) {
        if (this.level == null || this.level.isClientSide) return;

        if (HologramConfig.isHologramLocked()) {
            return;
        }

        String textResponse = HologramManager.processCommand(command);

        if (textResponse != null) {
            startTextChange(textResponse);
        }
    }

    private void startTextChange(String newText) {
        this.isChangingText = true;
        this.pendingText = newText;
        startHologramDisappearingForAll();
        this.changeCooldown = 30;
        setChanged();
    }

    private void startHologramDisappearingForAll() {
        if (this.level instanceof ServerLevel serverLevel) {
            for (Entity e : serverLevel.getAllEntities()) {
                if (e instanceof HologramEntity) {
                    ((HologramEntity) e).startDisappearing();
                }
            }
        }
    }

    private final LazyOptional<IEnergyStorage> energyHandler = LazyOptional.of(() -> new IEnergyStorage() {
        @Override
        public int receiveEnergy(int maxReceive, boolean simulate) {
            int energyReceived = Math.min(MAX_ENERGY - energyStored, maxReceive);
            if (!simulate) {
                energyStored += energyReceived;
                setChanged();
            }
            return energyReceived;
        }

        @Override
        public int extractEnergy(int maxExtract, boolean simulate) {
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

        @Override
        public int getEnergyStored() {
            return energyStored;
        }

        @Override
        public int getMaxEnergyStored() {
            return MAX_ENERGY;
        }

        @Override
        public boolean canExtract() {
            return true;
        }

        @Override
        public boolean canReceive() {
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