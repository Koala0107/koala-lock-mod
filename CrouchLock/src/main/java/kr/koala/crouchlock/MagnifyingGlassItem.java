package kr.koala.crouchlock;

import net.minecraft.block.Block;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.BlockItem;
import net.minecraft.item.ItemStack;
import net.minecraft.sound.SoundCategory;
import net.minecraft.util.Hand;
import net.minecraft.util.TypedActionResult;
import net.minecraft.world.World;

public final class MagnifyingGlassItem extends BlockItem {
    public MagnifyingGlassItem(Block block, Settings settings) {
        super(block, settings);
    }

    @Override
    public TypedActionResult<ItemStack> use(World world, PlayerEntity user, Hand hand) {
        if (!world.isClient) {
            world.playSound(
                    null,
                    user.getX(), user.getY(), user.getZ(),
                    DetectiveItemsMod.MAGNIFYING_GLASS_USE,
                    SoundCategory.PLAYERS,
                    1.0F,
                    1.0F
            );
        }
        return TypedActionResult.success(user.getStackInHand(hand), world.isClient());
    }
}
