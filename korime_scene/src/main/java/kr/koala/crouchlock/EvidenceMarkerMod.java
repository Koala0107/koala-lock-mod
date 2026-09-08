package kr.koala.crouchlock;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.itemgroup.v1.ItemGroupEvents;
import net.minecraft.block.AbstractBlock;
import net.minecraft.block.Block;
import net.minecraft.item.BlockItem;
import net.minecraft.item.Item;
import net.minecraft.item.ItemGroups;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.sound.BlockSoundGroup;
import net.minecraft.util.Identifier;

public final class EvidenceMarkerMod implements ModInitializer {
    public static final int COUNT = 15;
    public static final Block[] BLOCKS = new Block[COUNT];
    public static final Item[] ITEMS = new Item[COUNT];

    static {
        for (int i = 0; i < COUNT; i++) {
            int number = i + 1;
            Identifier id = new Identifier(CrouchLockMod.MOD_ID, "evidence_marker_" + number);
            Block block = Registry.register(Registries.BLOCK, id,
                    new EvidenceMarkerBlock(AbstractBlock.Settings.create()
                            .strength(0.2F)
                            .sounds(BlockSoundGroup.WOOL)
                            .nonOpaque()));
            Item item = Registry.register(Registries.ITEM, id, new BlockItem(block, new Item.Settings()));
            BLOCKS[i] = block;
            ITEMS[i] = item;
        }
    }

    @Override
    public void onInitialize() {
        ItemGroupEvents.modifyEntriesEvent(ItemGroups.FUNCTIONAL).register(entries -> {
            for (Item item : ITEMS) {
                entries.add(item);
            }
        });
    }
}
