package kr.koala.korime_scene;

import net.minecraft.entity.EntityType;
import net.minecraft.entity.vehicle.BoatEntity;
import net.minecraft.item.Item;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;

public final class BicycleEntity extends BoatEntity {
    private static final double MAX_GROUND_SPEED = 0.48D;

    public BicycleEntity(EntityType<? extends BoatEntity> entityType, World world) {
        super(entityType, world);
    }

    @Override
    public Item asItem() {
        return BicycleMod.BICYCLE_ITEM;
    }

    @Override
    public void tick() {
        super.tick();

        if (hasPassengers() && isOnGround()) {
            Vec3d velocity = getVelocity();
            double horizontal = Math.sqrt(velocity.x * velocity.x + velocity.z * velocity.z);
            if (horizontal > 0.01D) {
                double target = Math.min(MAX_GROUND_SPEED, horizontal * 1.22D + 0.01D);
                double scale = target / horizontal;
                setVelocity(velocity.x * scale, velocity.y, velocity.z * scale);
            }
        }
    }
}
