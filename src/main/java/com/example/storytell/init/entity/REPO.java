package com.example.storytell.init.entity;

import com.example.storytell.init.ModItems;
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
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.annotation.Nullable;
import java.util.UUID;

public class REPO extends PathfinderMob {
    private static final Logger LOGGER = LogManager.getLogger();

    private static final EntityDataAccessor<String> DATA_TARGET_PLAYER = SynchedEntityData.defineId(REPO.class, EntityDataSerializers.STRING);
    private static final EntityDataAccessor<Boolean> DATA_HAS_BEEN_SEEN = SynchedEntityData.defineId(REPO.class, EntityDataSerializers.BOOLEAN);

    private int spawnTime = 0;
    private static final int MAX_LIFETIME = 5 * 60 * 20; // 5 minutes in ticks
    private UUID targetPlayerUUID;

    // Оптимизация: кэшированные значения для проверок
    private int lastPlayerCheckTick = 0;
    private int lastLookCheckTick = 0;
    private static final int PLAYER_CHECK_INTERVAL = 20; // Проверять игроков каждую секунду
    private static final int LOOK_CHECK_INTERVAL = 10; // Проверять взгляд каждые 0.5 секунды

    // Кэш для вычислений направления
    private Vec3 cachedLookVec;
    private long lastLookVecUpdate = 0;

    public REPO(EntityType<? extends PathfinderMob> entityType, Level level) {
        super(entityType, level);
        this.noCulling = true;
        this.setNoGravity(true);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Mob.createMobAttributes()
                .add(Attributes.MAX_HEALTH, 1.0D)
                .add(Attributes.MOVEMENT_SPEED, 0.0D)
                .add(Attributes.KNOCKBACK_RESISTANCE, 1.0D)
                .add(Attributes.ATTACK_DAMAGE, 0.0D)
                .add(Attributes.FOLLOW_RANGE, 200.0D);
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

        // Оптимизированные проверки с интервалами
        if (!this.level().isClientSide()) {
            int currentTick = this.tickCount;

            // Проверяем игроков в радиусе реже
            if (currentTick - lastPlayerCheckTick >= PLAYER_CHECK_INTERVAL) {
                checkForNearbyPlayers();
                lastPlayerCheckTick = currentTick;
            }

            // Проверяем взгляд игроков реже
            if (currentTick - lastLookCheckTick >= LOOK_CHECK_INTERVAL) {
                checkIfSeenByPlayers();
                lastLookCheckTick = currentTick;
            }
        }

        // Check disappearance conditions
        if (spawnTime >= MAX_LIFETIME || hasBeenSeen()) {
            spawnParticles();
            REPOSpawnManager.setActiveRepo(null);
            this.discard();
            return;
        }

        // Оптимизированное управление вращением
        if (!this.level().isClientSide() && targetPlayerUUID != null) {
            updateRotation();
        }
    }

    private void updateRotation() {
        Player targetPlayer = ((ServerLevel) this.level()).getPlayerByUUID(targetPlayerUUID);
        if (targetPlayer != null) {
            lookAtPlayer(targetPlayer);

            // Оптимизированная синхронизация вращения
            this.yBodyRot = this.getYRot();
            this.yHeadRot = this.getYRot();
            this.yBodyRotO = this.yBodyRot;
            this.yHeadRotO = this.yHeadRot;
        }
    }

    private void spawnParticles() {
        if (this.level() instanceof ServerLevel serverLevel) {
            double x = this.getX();
            double y = this.getY() + 1.0;
            double z = this.getZ();

            // Оптимизированное создание частиц - меньше частиц
            for (int i = 0; i < 15; i++) { // Уменьшено с 30 до 15
                double offsetX = (this.random.nextDouble() - 0.5) * 2.0;
                double offsetY = this.random.nextDouble() * 2.0;
                double offsetZ = (this.random.nextDouble() - 0.5) * 2.0;

                serverLevel.sendParticles(net.minecraft.core.particles.ParticleTypes.PORTAL,
                        x + offsetX, y + offsetY, z + offsetZ,
                        1, 0, 0, 0, 0.1);
            }
        }
    }

    private void checkForNearbyPlayers() {
        if (this.level().isClientSide()) return;

        // Оптимизированный поиск игроков в радиусе
        for (Player player : this.level().getEntitiesOfClass(Player.class, this.getBoundingBox().inflate(20.0D))) {
            double distance = player.distanceTo(this);
            if (distance < 20.0D) {
                LOGGER.debug("REPO исчез из-за близости игрока: {} (расстояние: {})",
                        player.getName().getString(), distance);
                spawnParticles();
                REPOSpawnManager.setActiveRepo(null);
                this.discard();
                return;
            }
        }
    }

    private void lookAtPlayer(Player player) {
        // Кэшируем вычисления
        double deltaX = player.getX() - this.getX();
        double deltaY = (player.getY() + player.getEyeHeight()) - (this.getY() + this.getEyeHeight());
        double deltaZ = player.getZ() - this.getZ();

        double horizontalDistance = Math.sqrt(deltaX * deltaX + deltaZ * deltaZ);

        float yaw = (float)(Math.atan2(deltaZ, deltaX) * (180.0F / Math.PI)) - 90.0F;
        float pitch = (float)(-(Math.atan2(deltaY, horizontalDistance) * (180.0F / Math.PI)));

        this.setYRot(rotateTowards(this.getYRot(), yaw, 10.0F));
        this.setXRot(rotateTowards(this.getXRot(), pitch, 10.0F));
    }

    private static float rotateTowards(float current, float target, float maxDelta) {
        float delta = Mth.wrapDegrees(target - current);
        delta = Mth.clamp(delta, -maxDelta, maxDelta);
        return current + delta;
    }

    private void checkIfSeenByPlayers() {
        if (this.level().isClientSide()) return;

        // Оптимизированный поиск только ближайших игроков
        for (Player player : this.level().getEntitiesOfClass(Player.class, this.getBoundingBox().inflate(200.0D))) {
            if (isPlayerLookingAt(player)) {
                setHasBeenSeen(true);
                LOGGER.debug("REPO detected by player: {}", player.getName().getString());
                break;
            }
        }
    }

    private boolean isPlayerLookingAt(Player player) {
        Vec3 eyePos = player.getEyePosition(1.0F);
        Vec3 toRepo = this.position().subtract(eyePos);
        double distance = toRepo.length();

        if (distance > 200.0D) return false;

        // Кэшируем вектор взгляда для оптимизации
        Vec3 lookVec = getCachedLookVector(player);
        Vec3 toRepoNormalized = toRepo.normalize();
        double dot = lookVec.dot(toRepoNormalized);

        double angleThreshold = distance < 20.0D ? 0.97 : 0.95;

        if (dot > angleThreshold) {
            return !isObstructed(eyePos, this.position(), player, Math.min(distance, 200.0D));
        }

        return false;
    }

    private Vec3 getCachedLookVector(Player player) {
        // Кэшируем вектор взгляда на короткое время
        long currentTime = System.currentTimeMillis();
        if (cachedLookVec == null || currentTime - lastLookVecUpdate > 50) { // 50ms кэш
            cachedLookVec = player.getViewVector(1.0F);
            lastLookVecUpdate = currentTime;
        }
        return cachedLookVec;
    }

    private boolean isObstructed(Vec3 start, Vec3 end, Player player, double maxDistance) {
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

    // Custom rotation methods
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

    @Override
    public boolean hurt(net.minecraft.world.damagesource.DamageSource source, float amount) {
        if (!this.level().isClientSide() && source.getEntity() instanceof Player) {
            ItemStack shardStack = new ItemStack(ModItems.ROBOT_SHARD.get(), 1);
            this.spawnAtLocation(shardStack);
            spawnParticles();
            REPOSpawnManager.setActiveRepo(null);
            this.discard();
            return true;
        }
        return false;
    }

    @Override
    public void remove(net.minecraft.world.entity.Entity.RemovalReason reason) {
        this.targetPlayerUUID = null;
        REPOSpawnManager.setActiveRepo(null);
        super.remove(reason);
    }
}