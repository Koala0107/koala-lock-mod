package kr.koala.crouchlock;

import net.minecraft.block.Block;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.BlockItem;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ItemUsageContext;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.TypedActionResult;
import net.minecraft.world.World;

/** Magnifying glass: normal right-click inspects with sound; sneak-right-click keeps the old placeable behavior. */
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
        PlayerEntity player = context.getPlayer();
        if (player != null && player.isSneaking()) {
            return super.useOnBlock(context);
        }
        playUseSound(context.getWorld(), player);
        return ActionResult.success(context.getWorld().isClient());
    }

    private static void playUseSound(World world, PlayerEntity user) {
        if (user == null || world.isClient()) return;
        world.playSound(
                null,
                user.getX(), user.getY(), user.getZ(),
                SoundEvents.ITEM_SPYGLASS_USE.value(),
                SoundCategory.PLAYERS,
                1.0F,
                1.0F
        );
    }
}
