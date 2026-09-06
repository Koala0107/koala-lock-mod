package kr.koala.crouchlock;

import net.fabricmc.api.ModInitializer;
import net.minecraft.block.AbstractBlock;
import net.minecraft.block.Block;
import net.minecraft.item.ArmorItem;
import net.minecraft.item.ArmorMaterials;
import net.minecraft.item.BlockItem;
import net.minecraft.item.Item;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.sound.BlockSoundGroup;
import net.minecraft.util.Identifier;

public final class DetectiveItemsMod implements ModInitializer {
    public static final Item DETECTIVE_HAT = Registry.register(
            Registries.ITEM, new Identifier(CrouchLockMod.MOD_ID, "detective_hat"),
            new ArmorItem(ArmorMaterials.LEATHER, ArmorItem.Type.HELMET, new Item.Settings()));

    public static final Item DETECTIVE_ROBE = Registry.register(
            Registries.ITEM, new Identifier(CrouchLockMod.MOD_ID, "detective_robe"),
            new ArmorItem(ArmorMaterials.LEATHER, ArmorItem.Type.CHESTPLATE, new Item.Settings()));

    public static final Block MAGNIFYING_GLASS_BLOCK = Registry.register(
            Registries.BLOCK, new Identifier(CrouchLockMod.MOD_ID, "magnifying_glass"),
            new MagnifyingGlassBlock(AbstractBlock.Settings.create().strength(0.15F)
                    .nonOpaque().sounds(BlockSoundGroup.METAL)));

    public static final Item MAGNIFYING_GLASS = Registry.register(
            Registries.ITEM, new Identifier(CrouchLockMod.MOD_ID, "magnifying_glass"),
            new BlockItem(MAGNIFYING_GLASS_BLOCK, new Item.Settings().maxCount(16)));

    @Override public void onInitialize() { }
}
