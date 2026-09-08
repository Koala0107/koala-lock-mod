package kr.koala.crouchlock;

import net.minecraft.block.Block;
import net.minecraft.client.item.TooltipContext;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.BlockItem;
import net.minecraft.item.ItemStack;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Hand;
import net.minecraft.util.TypedActionResult;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public final class SmartphoneItem extends BlockItem {
    public SmartphoneItem(Block block, Settings settings) {
        super(block, settings);
    }

    @Override
    public TypedActionResult<ItemStack> use(World world, PlayerEntity user, Hand hand) {
        return TypedActionResult.success(user.getStackInHand(hand), world.isClient());
    }

    @Override
    public Text getName(ItemStack stack) {
        SmartphoneData data = SmartphoneData.fromStack(stack);
        if (data.finalized() && !data.title().isBlank()) {
            return Text.literal(data.title());
        }
        return Text.translatable("item.korime_scene.smartphone");
    }

    @Override
    public void appendTooltip(ItemStack stack, @Nullable World world, List<Text> tooltip,
                              TooltipContext context) {
        SmartphoneData data = SmartphoneData.fromStack(stack);
        if (data.finalized() && !data.subtitle().isBlank()) {
            tooltip.add(Text.literal(data.subtitle()).formatted(Formatting.LIGHT_PURPLE));
        }
    }
}
