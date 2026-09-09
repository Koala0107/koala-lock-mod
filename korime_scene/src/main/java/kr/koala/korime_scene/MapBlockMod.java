package kr.koala.korime_scene;

import net.fabricmc.api.ModInitializer;
import net.minecraft.block.AbstractBlock;
import net.minecraft.block.Block;
import net.minecraft.block.BlockSoundGroup;
import net.minecraft.item.BlockItem;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

public final class MapBlockMod implements ModInitializer {
    public static final Block MAP_BLOCK = Registry.register(
            Registries.BLOCK,
            new Identifier(KorimeSceneMod.MOD_ID, "map_block"),
            new Block(AbstractBlock.Settings.create()
                    .strength(2.0F, 6.0F)
                    .sounds(BlockSoundGroup.STONE))
    );

    public static final Item MAP_BLOCK_ITEM = Registry.register(
            Registries.ITEM,
            new Identifier(KorimeSceneMod.MOD_ID, "map_block"),
            new BlockItem(MAP_BLOCK, new Item.Settings()) {
                @Override
                public Text getName(ItemStack stack) {
                    return Text.literal("코라임씬 맵 전용 블록");
                }
            }
    );

    @Override
    public void onInitialize() { }
}
