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
    public static final Block HORIZONTAL = registerEdge("boundary_line_horizontal", "하얀선 (가로)", true);
    public static final Block VERTICAL = registerEdge("boundary_line_vertical", "하얀선 (세로)", false);
    public static final Block CORNER = register(
            "boundary_line_corner",
            "하얀선 (모서리)",
            new BoundaryCornerBlock(settings())
    );

    private static Block register(String id, String displayName) {
        return register(id, displayName, new BoundaryLineBlock(settings()));
    }

    private static Block registerEdge(String id, String displayName, boolean horizontal) {
        return register(id, displayName, new BoundaryEdgeBlock(settings(), horizontal));
    }

    private static AbstractBlock.Settings settings() {
        return AbstractBlock.Settings.create()
                .strength(0.05F)
                .nonOpaque()
                .sounds(BlockSoundGroup.WOOL);
    }

    private static Block register(String id, String displayName, Block block) {
        Registry.register(Registries.BLOCK, new Identifier(CrouchLockMod.MOD_ID, id), block);
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
