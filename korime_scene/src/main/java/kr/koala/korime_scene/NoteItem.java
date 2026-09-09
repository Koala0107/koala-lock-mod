package kr.koala.korime_scene;

import net.minecraft.client.item.TooltipContext;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.world.World;

import java.util.List;

public final class NoteItem extends Item {
    public NoteItem(Settings settings) {
        super(settings);
    }

    @Override
    public Text getName(ItemStack stack) {
        return Text.literal("노트");
    }

    @Override
    public void appendTooltip(ItemStack stack, World world, List<Text> tooltip, TooltipContext context) {
        String title = NoteData.getTitle(stack);
        if (!title.isBlank()) tooltip.add(Text.literal(title).formatted(Formatting.YELLOW));
        tooltip.add(Text.literal("우클릭하여 자유롭게 기록").formatted(Formatting.GRAY));
    }
}
