package kr.koala.korime_scene;

import net.minecraft.block.Block;
import net.minecraft.item.BlockItem;
import net.minecraft.item.ItemStack;
import net.minecraft.text.Text;

public final class ImageFrameItem extends BlockItem {
    public ImageFrameItem(Block block, Settings settings) {
        super(block, settings);
    }

    @Override
    public Text getName(ItemStack stack) {
        return Text.literal("이미지 액자");
    }
}
