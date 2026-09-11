package kr.koala.korime_scene;

import net.minecraft.entity.EntityType;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.mob.SkeletonHorseEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.world.World;

public final class BicycleEntity extends SkeletonHorseEntity {
    private static final double BICYCLE_SPEED = 0.42D;

    public BicycleEntity(EntityType<? extends SkeletonHorseEntity> type, World world) {
        super(type, world);
        setSilent(true);
        setTame(true);
        setTrapped(false);
        if (getAttributeInstance(EntityAttributes.GENERIC_MOVEMENT_SPEED) != null) {
            getAttributeInstance(EntityAttributes.GENERIC_MOVEMENT_SPEED).setBaseValue(BICYCLE_SPEED);
        }
    }

    @Override
    public boolean isSaddled() {
        return true;
    }

    @Override
    public boolean canBeSaddled() {
        return false;
    }

    @Override
    public boolean canJump() {
        return false;
    }

    @Override
    public ActionResult interactMob(PlayerEntity player, Hand hand) {
        if (!getWorld().isClient) {
            player.startRiding(this, true);
        }
        return ActionResult.success(getWorld().isClient);
    }

    @Override
    protected void dropLoot(DamageSource source, boolean causedByPlayer) {
        dropItem(BicycleMod.BICYCLE_ITEM);
    }

    @Override
    protected void dropXp() {
        // A bicycle is not an animal for gameplay purposes.
    }

    @Override
    public void tick() {
        if (!isSilent()) {
            setSilent(true);
        }
        super.tick();
    }
}
