package kr.koala.crouchlock;

import net.fabricmc.api.ModInitializer;
import net.minecraft.block.AbstractBlock;
import net.minecraft.block.Block;
import net.minecraft.item.BlockItem;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.sound.BlockSoundGroup;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

public final class BoundaryLineMod implements ModInitializer {
    public static final Block HORIZONTAL = register("boundary_line_horizontal", "하얀선 (가로)");
    public static final Block VERTICAL = register("boundary_line_vertical", "하얀선 (세로)");
    public static final Block CORNER_NW = register("boundary_line_corner_nw", "하얀선 코너 (좌상)");
    public static final Block CORNER_NE = register("boundary_line_corner_ne", "하얀선 코너 (우상)");
    public static final Block CORNER_SW = register("boundary_line_corner_sw", "하얀선 코너 (좌하)");
    public static final Block CORNER_SE = register("boundary_line_corner_se", "하얀선 코너 (우하)");

    private static Block register(String id, String displayName) {
        Block block = Registry.register(
                Registries.BLOCK,
                new Identifier(CrouchLockMod.MOD_ID, id),
                new BoundaryLineBlock(AbstractBlock.Settings.create()
                        .strength(0.05F)
                        .nonOpaque()
                        .noCollision()
                        .sounds(BlockSoundGroup.WOOL))
        );
        Registry.register(
                Registries.ITEM,
                new Identifier(CrouchLockMod.MOD_ID, id),
                new NamedBlockItem(block, new Item.Settings().maxCount(64), displayName)
        );
        return block;
    }

    @Override public void onInitialize() { }

    private static final class NamedBlockItem extends BlockItem {
        private final Text name;
        private NamedBlockItem(Block block, Settings settings, String name) {
            super(block, settings);
            this.name = Text.literal(name);
        }
        @Override public Text getName(ItemStack stack) { return name; }
    }
}
