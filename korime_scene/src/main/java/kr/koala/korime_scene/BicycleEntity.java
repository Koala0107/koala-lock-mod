package kr.koala.korime_scene;

import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.MovementType;
import net.minecraft.entity.attribute.DefaultAttributeContainer;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.entity.passive.AnimalEntity;
import net.minecraft.entity.passive.PassiveEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;

public final class BicycleEntity extends AnimalEntity {
    private static final double FORWARD_SPEED = 0.34;
    private static final double REVERSE_SPEED = 0.16;
    private static final float TURN_SPEED = 3.8F;

    public BicycleEntity(EntityType<? extends AnimalEntity> type, World world) {
        super(type, world);
        setPersistent();
    }

    public static DefaultAttributeContainer.Builder createBicycleAttributes() {
        return MobEntity.createMobAttributes()
                .add(EntityAttributes.GENERIC_MAX_HEALTH, 20.0)
                .add(EntityAttributes.GENERIC_MOVEMENT_SPEED, 0.30)
                .add(EntityAttributes.GENERIC_KNOCKBACK_RESISTANCE, 1.0);
    }

    @Override
    protected void initGoals() {
        // A bicycle has no AI; it only moves while ridden.
    }

    @Override
    public ActionResult interactMob(PlayerEntity player, Hand hand) {
        if (player.isSneaking()) {
            if (!getWorld().isClient) {
                ItemStack stack = new ItemStack(BicycleMod.BICYCLE_ITEM);
                if (!player.getInventory().insertStack(stack)) player.dropItem(stack, false);
                discard();
            }
            return ActionResult.success(getWorld().isClient);
        }

        if (!hasPassengers()) {
            if (!getWorld().isClient) player.startRiding(this);
            return ActionResult.success(getWorld().isClient);
        }
        return ActionResult.PASS;
    }

    @Override
    public void travel(Vec3d movementInput) {
        LivingEntity rider = getControllingPassenger();
        if (isAlive() && rider != null) {
            float forwardInput = rider.forwardSpeed;
            float turnInput = rider.sidewaysSpeed;

            float direction = forwardInput < 0.0F ? -1.0F : 1.0F;
            float turnAmount = -turnInput * TURN_SPEED * (Math.abs(forwardInput) > 0.01F ? direction : 0.45F);
            setYaw(MathHelper.wrapDegrees(getYaw() + turnAmount));
            setBodyYaw(getYaw());
            setHeadYaw(getYaw());

            double targetSpeed = 0.0;
            if (forwardInput > 0.01F) targetSpeed = FORWARD_SPEED * Math.min(1.0F, forwardInput);
            else if (forwardInput < -0.01F) targetSpeed = REVERSE_SPEED * Math.max(-1.0F, forwardInput);

            double radians = Math.toRadians(getYaw());
            double targetX = -Math.sin(radians) * targetSpeed;
            double targetZ = Math.cos(radians) * targetSpeed;

            Vec3d current = getVelocity();
            double horizontalLerp = Math.abs(forwardInput) > 0.01F ? 0.32 : 0.18;
            double vx = current.x + (targetX - current.x) * horizontalLerp;
            double vz = current.z + (targetZ - current.z) * horizontalLerp;
            if (Math.abs(forwardInput) <= 0.01F) {
                vx *= 0.78;
                vz *= 0.78;
            }

            double vy = current.y;
            if (isOnGround() && vy < 0.0) vy = 0.0;
            if (!hasNoGravity() && !isOnGround()) vy -= 0.08;

            setVelocity(vx, vy, vz);
            move(MovementType.SELF, getVelocity());
            setVelocity(getVelocity().multiply(0.98, 0.98, 0.98));
            return;
        }

        super.travel(Vec3d.ZERO);
    }

    public void jumpBicycle() {
        if (!isAlive() || !hasPassengers() || !isOnGround()) return;
        Vec3d velocity = getVelocity();
        setVelocity(velocity.x, 0.42, velocity.z);
        velocityDirty = true;
    }

    @Override
    protected void updatePassengerPosition(Entity passenger, Entity.PositionUpdater positionUpdater) {
        if (hasPassenger(passenger)) {
            passenger.setPosition(getX(), getY() + 0.78 + passenger.getHeightOffset(), getZ());
            if (passenger instanceof LivingEntity living) living.setBodyYaw(getYaw());
        }
    }

    @Nullable
    @Override
    public LivingEntity getControllingPassenger() {
        Entity first = getFirstPassenger();
        return first instanceof LivingEntity living ? living : null;
    }

    @Override
    protected boolean canAddPassenger(Entity passenger) {
        return getPassengerList().isEmpty();
    }

    @Override
    public boolean isLogicalSideForUpdatingMovement() {
        return true;
    }

    @Nullable
    @Override
    public PassiveEntity createChild(ServerWorld world, PassiveEntity entity) {
        return null;
    }
}
