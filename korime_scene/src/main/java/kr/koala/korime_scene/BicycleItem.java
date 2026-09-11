package kr.koala.korime_scene;

import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemUsageContext;
import net.minecraft.util.ActionResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

public final class BicycleItem extends Item {
    public BicycleItem(Settings settings) {
        super(settings);
    }

    @Override
    public ActionResult useOnBlock(ItemUsageContext context) {
        World world = context.getWorld();
        BlockPos pos = context.getBlockPos().offset(context.getSide());
        PlayerEntity player = context.getPlayer();

        // A bicycle is a land vehicle. Refuse placement inside water/lava so it
        // cannot immediately enter BoatEntity's water physics state.
        if (!world.getFluidState(pos).isEmpty()) {
            return ActionResult.FAIL;
        }

        BicycleEntity bicycle = new BicycleEntity(BicycleMod.BICYCLE, world);
        float yaw = player == null ? 0.0F : player.getYaw();
        bicycle.refreshPositionAndAngles(pos.getX() + 0.5D, pos.getY(), pos.getZ() + 0.5D, yaw, 0.0F);

        if (!world.isSpaceEmpty(bicycle, bicycle.getBoundingBox())) {
            return ActionResult.FAIL;
        }

        if (!world.isClient) {
            // Only consume the item after the server has accepted the spawn.
            if (!world.spawnEntity(bicycle)) {
                return ActionResult.FAIL;
            }
            if (player == null || !player.getAbilities().creativeMode) {
                context.getStack().decrement(1);
            }
        }

        return ActionResult.success(world.isClient);
    }
}
