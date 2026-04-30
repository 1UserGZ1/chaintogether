package com.evailcodes.chaintogether.entity;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;

public class ChainProxyEntity extends Entity {
    private static final EntityDataAccessor<Integer> DATA_CHAINED_PLAYER =
            SynchedEntityData.defineId(ChainProxyEntity.class, EntityDataSerializers.INT);

    public ChainProxyEntity(EntityType<? extends Entity> type, Level level) {
        super(type, level);
        this.noCulling = true;
        this.setInvulnerable(true);
        this.setNoGravity(true);
        this.setInvisible(true);
    }

    

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        builder.define(DATA_CHAINED_PLAYER, -1);
    }

    public void setChainedPlayerId(int id) {
        this.entityData.set(DATA_CHAINED_PLAYER, id);
    }

    public int getChainedPlayerId() {
        return this.entityData.get(DATA_CHAINED_PLAYER);
    }

    @Override
    public void tick() {
        super.tick();
        if (!this.level().isClientSide) {
            // 定期检查并更新位置
            if (this.tickCount % 20 == 0) {
                this.checkValidity();
            }
        }
    }

    private void checkValidity() {
        // 检查关联玩家是否存在
        if (this.level().getEntity(getChainedPlayerId()) == null) {
            this.discard();
        }
    }

    @Override
    public boolean isAttackable() {
        return false;
    }

    @Override
    public boolean isPickable() {
        return false;
    }

    @Override
    public boolean isPushable() {
        return false;
    }

    

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        tag.putInt("ChainedPlayer", getChainedPlayerId());
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        if (tag.contains("ChainedPlayer")) {
            setChainedPlayerId(tag.getInt("ChainedPlayer"));
        }
    }
}