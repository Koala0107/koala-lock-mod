package kr.koala.korime_scene;

import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ItemUsageContext;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.math.Vec3d;

public final class BicycleItem extends Item {
    public BicycleItem(Settings settings) {
        super(settings);
    }

    @Override
    public Text getName(ItemStack stack) {
        return Text.literal("자전거");
    }

    @Override
    public ActionResult useOnBlock(ItemUsageContext context) {
        if (context.getWorld().isClient) return ActionResult.SUCCESS;

        BicycleEntity bicycle = BicycleMod.BICYCLE_ENTITY.create(context.getWorld());
        if (bicycle == null) return ActionResult.FAIL;

        Vec3d p = context.getHitPos().add(Vec3d.of(context.getSide().getVector()).multiply(0.05));
        bicycle.refreshPositionAndAngles(p.x, p.y, p.z,
                context.getPlayer() == null ? 0.0F : context.getPlayer().getYaw(), 0.0F);

        if (!context.getWorld().spawnEntity(bicycle)) return ActionResult.FAIL;
        if (context.getPlayer() == null || !context.getPlayer().getAbilities().creativeMode) {
            context.getStack().decrement(1);
        }
        return ActionResult.CONSUME;
    }
}
