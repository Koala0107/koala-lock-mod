package kr.koala.crouchlock;

import net.minecraft.client.item.TooltipContext;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Hand;
import net.minecraft.util.TypedActionResult;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public final class SmartphoneItem extends Item {
    public SmartphoneItem(Settings settings) {
        super(settings);
    }

    @Override
    public TypedActionResult<ItemStack> use(World world, PlayerEntity user, Hand hand) {
        return TypedActionResult.success(user.getStackInHand(hand), world.isClient());
    }

    @Override
    public void appendTooltip(ItemStack stack, @Nullable World world, List<Text> tooltip,
                              TooltipContext context) {
        SmartphoneData data = SmartphoneData.fromStack(stack);
        tooltip.add(Text.translatable("item.crouchlock.smartphone.hint").formatted(Formatting.GRAY));
        tooltip.add(Text.translatable("item.crouchlock.smartphone.counts",
                data.calls().size(), data.messages().size()).formatted(Formatting.DARK_GRAY));
    }
}
