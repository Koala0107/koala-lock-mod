package kr.koala.korime_scene;

import net.minecraft.block.Block;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.BlockItem;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ItemUsageContext;
import net.minecraft.sound.SoundCategory;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.TypedActionResult;
import net.minecraft.world.World;

/** Magnifying glass: right-click plays the original custom sound; placement is disabled. */
public final class MagnifyingGlassItem extends BlockItem {
    public MagnifyingGlassItem(Block block, Settings settings) {
        super(block, settings);
    }

    @Override
    public TypedActionResult<ItemStack> use(World world, PlayerEntity user, Hand hand) {
        playUseSound(world, user);
        return TypedActionResult.success(user.getStackInHand(hand), world.isClient());
    }

    @Override
    public ActionResult useOnBlock(ItemUsageContext context) {
        playUseSound(context.getWorld(), context.getPlayer());
        return ActionResult.success(context.getWorld().isClient());
    }

    private static void playUseSound(World world, PlayerEntity user) {
        if (user == null || world.isClient()) return;
        world.playSound(
                null,
                user.getX(), user.getY(), user.getZ(),
                DetectiveItemsMod.MAGNIFYING_GLASS_USE,
                SoundCategory.PLAYERS,
                1.0F,
                1.0F
        );
    }
}
