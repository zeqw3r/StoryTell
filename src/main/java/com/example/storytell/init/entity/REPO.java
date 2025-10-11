// REPO.java
package com.example.storytell.init.entity;

import com.example.storytell.init.ModItems;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.phys.Vec3;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.util.Mth;

import javax.annotation.Nullable;
import java.util.UUID;

public class REPO extends PathfinderMob {
    private static final EntityDataAccessor<String> DATA_TARGET_PLAYER = SynchedEntityData.defineId(REPO.class, EntityDataSerializers.STRING);
    private static final EntityDataAccessor<Boolean> DATA_HAS_BEEN_SEEN = SynchedEntityData.defineId(REPO.class, EntityDataSerializers.BOOLEAN);

    private int spawnTime = 0;
    private static final int MAX_LIFETIME = 5 * 60 * 20; // 5 minutes in ticks
    private UUID targetPlayerUUID;

    public REPO(EntityType<? extends PathfinderMob> entityType, Level level) {
        super(entityType, level);
        this.noCulling = true;
        this.setNoGravity(true);
        // Убрали setInvulnerable(true), чтобы моб мог получать урон
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Mob.createMobAttributes()
                .add(Attributes.MAX_HEALTH, 1.0D)
                .add(Attributes.MOVEMENT_SPEED, 0.0D)
                .add(Attributes.KNOCKBACK_RESISTANCE, 1.0D)
                .add(Attributes.ATTACK_DAMAGE, 0.0D)
                .add(Attributes.FOLLOW_RANGE, 200.0D); // Increased to 200 blocks
    }

    @Override
    protected void defineSynchedData() {
        super.defineSynchedData();
        this.entityData.define(DATA_TARGET_PLAYER, "");
        this.entityData.define(DATA_HAS_BEEN_SEEN, false);
    }

    @Override
    protected void registerGoals() {
        // No goals needed
    }

    @Override
    public void tick() {
        super.tick();
        spawnTime++;

        // Проверяем, есть ли игроки в радиусе 20 блоков
        if (!this.level().isClientSide() && this.tickCount % 10 == 0) {
            checkForNearbyPlayers();
        }

        // Check disappearance conditions
        if (spawnTime >= MAX_LIFETIME || hasBeenSeen()) {
            spawnParticles(); // Добавляем частицы при исчезновении
            REPOSpawnManager.setActiveRepo(null);
            this.discard();
            return;
        }

        // Manually control looking at target player
        if (!this.level().isClientSide() && targetPlayerUUID != null) {
            Player targetPlayer = ((ServerLevel) this.level()).getPlayerByUUID(targetPlayerUUID);
            if (targetPlayer != null) {
                lookAtPlayer(targetPlayer);

                // Enhanced rotation synchronization
                this.yBodyRot = this.getYRot();
                this.yHeadRot = this.getYRot();
                this.yBodyRotO = this.yBodyRot;
                this.yHeadRotO = this.yHeadRot;
            }
        }

        // Check if any player is looking at REPO
        if (!this.level().isClientSide() && this.tickCount % 5 == 0) {
            checkIfSeenByPlayers();
        }
    }

    // Новый метод: создание фиолетовых частиц
    private void spawnParticles() {
        if (this.level() instanceof ServerLevel serverLevel) {
            // Используем частицы портала (фиолетовые, как у эндермена)
            double x = this.getX();
            double y = this.getY() + 1.0; // Немного выше позиции REPO
            double z = this.getZ();

            // Создаем много частиц для эффектного исчезновения/появления
            for (int i = 0; i < 30; i++) {
                double offsetX = (this.random.nextDouble() - 0.5) * 2.0;
                double offsetY = this.random.nextDouble() * 2.0;
                double offsetZ = (this.random.nextDouble() - 0.5) * 2.0;

                serverLevel.sendParticles(net.minecraft.core.particles.ParticleTypes.PORTAL,
                        x + offsetX, y + offsetY, z + offsetZ,
                        1, 0, 0, 0, 0.1);
            }
        }
    }

    // Новый метод: проверка наличия игроков в радиусе 20 блоков
    private void checkForNearbyPlayers() {
        if (this.level().isClientSide()) return;

        for (Player player : this.level().players()) {
            double distance = player.distanceTo(this);
            if (distance < 20.0D) {
                System.out.println("REPO исчез из-за близости игрока: " + player.getName().getString() + " (расстояние: " + distance + ")");
                spawnParticles(); // Добавляем частицы при исчезновении из-за близости
                REPOSpawnManager.setActiveRepo(null);
                this.discard();
                return;
            }
        }
    }

    private void lookAtPlayer(Player player) {
        // Calculate direction to player
        double deltaX = player.getX() - this.getX();
        double deltaY = (player.getY() + player.getEyeHeight()) - (this.getY() + this.getEyeHeight());
        double deltaZ = player.getZ() - this.getZ();

        // Calculate horizontal distance
        double horizontalDistance = Math.sqrt(deltaX * deltaX + deltaZ * deltaZ);

        // Calculate rotation angles
        float yaw = (float)(Math.atan2(deltaZ, deltaX) * (180.0F / Math.PI)) - 90.0F;
        float pitch = (float)(-(Math.atan2(deltaY, horizontalDistance) * (180.0F / Math.PI)));

        // Set rotation with smoothing
        this.setYRot(rotateTowards(this.getYRot(), yaw, 10.0F));
        this.setXRot(rotateTowards(this.getXRot(), pitch, 10.0F));
    }

    private static float rotateTowards(float current, float target, float maxDelta) {
        float delta = Mth.wrapDegrees(target - current);

        if (delta > maxDelta) {
            delta = maxDelta;
        }

        if (delta < -maxDelta) {
            delta = -maxDelta;
        }

        return current + delta;
    }

    private void checkIfSeenByPlayers() {
        if (this.level().isClientSide()) return;

        for (Player player : this.level().players()) {
            if (isPlayerLookingAt(player)) {
                setHasBeenSeen(true);
                System.out.println("REPO detected by player: " + player.getName().getString());
                break;
            }
        }
    }

    private boolean isPlayerLookingAt(Player player) {
        Vec3 eyePos = player.getEyePosition(1.0F);
        Vec3 lookVec = player.getViewVector(1.0F);
        Vec3 toRepo = this.position().subtract(eyePos);
        double distance = toRepo.length();

        // FIXED: Increased detection range to 200 blocks
        if (distance > 200.0D) return false;

        Vec3 toRepoNormalized = toRepo.normalize();
        double dot = lookVec.dot(toRepoNormalized);

        // УВЕЛИЧЕН УГОЛ ОБНАРУЖЕНИЯ: менее строгие пороги
        // Было: 0.9998 (примерно 1-2 градуса), стало: 0.95 (примерно 18 градусов)
        double angleThreshold = 0.95; // Значительно увеличен угол

        // Для близких расстояний также увеличиваем угол
        if (distance < 20.0D) {
            angleThreshold = 0.97; // Было: 0.9999, стало: 0.97 (примерно 14 градусов)
        }

        if (dot > angleThreshold) {
            // Improved obstruction check with distance limit
            return !isObstructed(eyePos, this.position(), player, Math.min(distance, 200.0D));
        }

        return false;
    }

    private boolean isObstructed(Vec3 start, Vec3 end, Player player, double maxDistance) {
        // Limit raycast distance to prevent performance issues
        Vec3 limitedEnd = start.add(end.subtract(start).normalize().scale(maxDistance));

        net.minecraft.world.phys.HitResult hitResult = this.level().clip(
                new net.minecraft.world.level.ClipContext(
                        start,
                        limitedEnd,
                        net.minecraft.world.level.ClipContext.Block.COLLIDER,
                        net.minecraft.world.level.ClipContext.Fluid.NONE,
                        player
                )
        );

        return hitResult.getType() == net.minecraft.world.phys.HitResult.Type.BLOCK;
    }

    // Custom rotation methods for model compatibility
    public float getBodyYRot() {
        return this.yBodyRot;
    }

    public float getYHeadRot() {
        return this.yHeadRot;
    }

    public void setYBodyRot(float bodyYaw) {
        this.yBodyRot = bodyYaw;
    }

    public void setYHeadRot(float headYaw) {
        this.yHeadRot = headYaw;
    }

    public void setTargetPlayer(UUID playerUUID) {
        this.targetPlayerUUID = playerUUID;
        this.entityData.set(DATA_TARGET_PLAYER, playerUUID.toString());
    }

    @Nullable
    public UUID getTargetPlayerUUID() {
        return targetPlayerUUID;
    }

    public boolean hasBeenSeen() {
        return this.entityData.get(DATA_HAS_BEEN_SEEN);
    }

    public void setHasBeenSeen(boolean seen) {
        this.entityData.set(DATA_HAS_BEEN_SEEN, seen);
    }

    @Override
    public void addAdditionalSaveData(CompoundTag compound) {
        super.addAdditionalSaveData(compound);
        compound.putInt("SpawnTime", spawnTime);
        if (targetPlayerUUID != null) {
            compound.putUUID("TargetPlayer", targetPlayerUUID);
        }
        compound.putBoolean("HasBeenSeen", hasBeenSeen());
    }

    @Override
    public void readAdditionalSaveData(CompoundTag compound) {
        super.readAdditionalSaveData(compound);
        spawnTime = compound.getInt("SpawnTime");
        if (compound.hasUUID("TargetPlayer")) {
            targetPlayerUUID = compound.getUUID("TargetPlayer");
        }
        setHasBeenSeen(compound.getBoolean("HasBeenSeen"));
    }

    @Override
    public boolean isSilent() {
        return true;
    }

    @Nullable
    @Override
    protected SoundEvent getAmbientSound() {
        return null;
    }

    @Override
    public boolean removeWhenFarAway(double distanceToClosestPlayer) {
        return false;
    }

    // Добавляем метод для обработки урона и выпадения предмета
    @Override
    public boolean hurt(net.minecraft.world.damagesource.DamageSource source, float amount) {
        if (!this.level().isClientSide() && source.getEntity() instanceof Player) {
            // Создаем предмет осколка робота при "убийстве"
            ItemStack shardStack = new ItemStack(ModItems.ROBOT_SHARD.get(), 1);
            this.spawnAtLocation(shardStack);

            // Добавляем частицы при "убийстве"
            spawnParticles();

            // Удаляем REPO
            REPOSpawnManager.setActiveRepo(null);
            this.discard();
            return true;
        }
        return false;
    }

    // Добавляем обработку удаления entity
    @Override
    public void remove(net.minecraft.world.entity.Entity.RemovalReason reason) {
        this.targetPlayerUUID = null; // Очищаем целевого игрока
        REPOSpawnManager.setActiveRepo(null);
        super.remove(reason);
    }
}