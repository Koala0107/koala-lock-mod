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
    public static final Item DETECTIVE_HELMET = Registry.register(
            Registries.ITEM, new Identifier(CrouchLockMod.MOD_ID, "detective_helmet"),
            new ArmorItem(ArmorMaterials.IRON, ArmorItem.Type.HELMET, new Item.Settings()));

    public static final Item DETECTIVE_CHESTPLATE = Registry.register(
            Registries.ITEM, new Identifier(CrouchLockMod.MOD_ID, "detective_chestplate"),
            new ArmorItem(ArmorMaterials.IRON, ArmorItem.Type.CHESTPLATE, new Item.Settings()));

    public static final Item DETECTIVE_LEGGINGS = Registry.register(
            Registries.ITEM, new Identifier(CrouchLockMod.MOD_ID, "detective_leggings"),
            new ArmorItem(ArmorMaterials.IRON, ArmorItem.Type.LEGGINGS, new Item.Settings()));

    public static final Item DETECTIVE_BOOTS = Registry.register(
            Registries.ITEM, new Identifier(CrouchLockMod.MOD_ID, "detective_boots"),
            new ArmorItem(ArmorMaterials.IRON, ArmorItem.Type.BOOTS, new Item.Settings()));

    public static final Block MAGNIFYING_GLASS_BLOCK = Registry.register(
            Registries.BLOCK, new Identifier(CrouchLockMod.MOD_ID, "magnifying_glass"),
            new MagnifyingGlassBlock(AbstractBlock.Settings.create().strength(0.15F)
                    .nonOpaque().sounds(BlockSoundGroup.METAL)));

    public static final Item MAGNIFYING_GLASS = Registry.register(
            Registries.ITEM, new Identifier(CrouchLockMod.MOD_ID, "magnifying_glass"),
            new BlockItem(MAGNIFYING_GLASS_BLOCK, new Item.Settings().maxCount(16)));

    @Override public void onInitialize() { }
}
