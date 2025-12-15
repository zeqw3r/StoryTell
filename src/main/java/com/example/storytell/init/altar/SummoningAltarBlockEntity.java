package com.example.storytell.init.altar;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraftforge.registries.ForgeRegistries;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class SummoningAltarBlockEntity extends BlockEntity implements MenuProvider {

    private String selectedEntity = "";
    private static final int SUMMON_RADIUS = 150;

    public SummoningAltarBlockEntity(BlockPos pos, BlockState state) {
        super(ModAltarBlocks.SUMMONING_ALTAR_BLOCK_ENTITY_TYPE.get(), pos, state);
    }

    public boolean performSummon(Level level, BlockPos pos, Player player) {
        if (selectedEntity == null || selectedEntity.isEmpty()) {
            player.displayClientMessage(net.minecraft.network.chat.Component.translatable("message.storytell.altar.no_entity"), true);
            return false;
        }

        if (!(level instanceof ServerLevel serverLevel)) {
            player.displayClientMessage(net.minecraft.network.chat.Component.translatable("message.storytell.altar.client_side"), true);
            return false;
        }

        int destroyedCount = destroyAllBossEntities(level, pos);
        boolean success = summonSelectedEntity(serverLevel, pos);

        if (success) {
            player.displayClientMessage(net.minecraft.network.chat.Component.translatable("message.storytell.altar.summoned",
                    getEntityDisplayName(selectedEntity), destroyedCount), true);
        } else {
            player.displayClientMessage(net.minecraft.network.chat.Component.translatable("message.storytell.altar.failed"), true);
        }
        return success;
    }

    private int destroyAllBossEntities(Level level, BlockPos pos) {
        AABB area = new AABB(
                pos.getX() - SUMMON_RADIUS, pos.getY() - SUMMON_RADIUS, pos.getZ() - SUMMON_RADIUS,
                pos.getX() + SUMMON_RADIUS, pos.getY() + SUMMON_RADIUS, pos.getZ() + SUMMON_RADIUS
        );

        List<Entity> entities = level.getEntities(null, area);
        List<String> bossList = com.example.storytell.init.HologramConfig.getBossList();
        int destroyedCount = 0;

        for (Entity entity : entities) {
            ResourceLocation entityId = ForgeRegistries.ENTITY_TYPES.getKey(entity.getType());
            if (entityId != null && bossList.contains(entityId.toString())) {
                entity.discard();
                destroyedCount++;
            }
        }
        return destroyedCount;
    }

    private boolean summonSelectedEntity(ServerLevel level, BlockPos pos) {
        try {
            ResourceLocation entityId = new ResourceLocation(selectedEntity);
            EntityType<?> entityType = ForgeRegistries.ENTITY_TYPES.getValue(entityId);

            if (entityType != null) {
                Entity entity = entityType.create(level);
                if (entity != null) {
                    entity.moveTo(
                            pos.getX() + 0.5,
                            pos.getY() + 1.0,
                            pos.getZ() + 0.5,
                            0.0F,
                            0.0F
                    );

                    if (entity instanceof net.minecraft.world.entity.Mob) {
                        ((net.minecraft.world.entity.Mob) entity).finalizeSpawn(
                                level,
                                level.getCurrentDifficultyAt(pos),
                                MobSpawnType.SPAWNER,
                                null,
                                null
                        );
                    }

                    level.addFreshEntity(entity);
                    return true;
                }
            }
        } catch (Exception e) {
            System.err.println("Failed to summon entity: " + e.getMessage());
        }
        return false;
    }

    private String getEntityDisplayName(String entityId) {
        try {
            ResourceLocation resource = new ResourceLocation(entityId);
            var entityType = ForgeRegistries.ENTITY_TYPES.getValue(resource);
            if (entityType != null) {
                return entityType.getDescription().getString();
            }
        } catch (Exception e) {
            // Используем ID если не удалось получить имя
        }
        return entityId;
    }

    public void setSelectedEntity(String entityId) {
        this.selectedEntity = entityId;
        setChanged();
    }

    public String getSelectedEntity() {
        return selectedEntity;
    }

    @Override
    public void load(CompoundTag tag) {
        super.load(tag);
        if (tag.contains("SelectedEntity")) {
            selectedEntity = tag.getString("SelectedEntity");
        }
    }

    @Override
    public void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
        if (selectedEntity != null) {
            tag.putString("SelectedEntity", selectedEntity);
        }
    }

    // ===== MenuProvider Implementation =====
    @Override
    public Component getDisplayName() {
        return Component.translatable("screen.storytell.summoning_altar");
    }

    @Nullable
    @Override
    public AbstractContainerMenu createMenu(int containerId, Inventory playerInventory, Player player) {
        return new SummoningAltarContainer(containerId, playerInventory, this);
    }
}