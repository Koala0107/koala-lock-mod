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
import net.minecraft.sound.SoundEvent;
import net.minecraft.util.Identifier;

public final class DetectiveItemsMod implements ModInitializer {
    public static final Item DETECTIVE_HELMET = Registry.register(
            Registries.ITEM, new Identifier(CrouchLockMod.MOD_ID, "detective_helmet"),
            new ArmorItem(ArmorMaterials.IRON, ArmorItem.Type.HELMET, new Item.Settings()));

    public static final Item DETECTIVE_CHESTPLATE = Registry.register(
            Registries.ITEM, new Identifier(CrouchLockMod.MOD_ID, "detective_chestplate"),
            new ArmorItem(ArmorMaterials.IRON, ArmorItem.Type.CHESTPLATE, new Item.Settings()));

    public static final Identifier MAGNIFYING_GLASS_USE_ID =
            new Identifier(CrouchLockMod.MOD_ID, "magnifying_glass_use");
    public static final SoundEvent MAGNIFYING_GLASS_USE = Registry.register(
            Registries.SOUND_EVENT,
            MAGNIFYING_GLASS_USE_ID,
            SoundEvent.of(MAGNIFYING_GLASS_USE_ID));

    public static final Block MAGNIFYING_GLASS_BLOCK = Registry.register(
            Registries.BLOCK, new Identifier(CrouchLockMod.MOD_ID, "magnifying_glass"),
            new MagnifyingGlassBlock(AbstractBlock.Settings.create().strength(0.15F)
                    .nonOpaque().sounds(BlockSoundGroup.METAL)));

    public static final Item MAGNIFYING_GLASS = Registry.register(
            Registries.ITEM, new Identifier(CrouchLockMod.MOD_ID, "magnifying_glass"),
            new MagnifyingGlassItem(MAGNIFYING_GLASS_BLOCK, new Item.Settings().maxCount(16)));

    public static final Block INVESTIGATION_BOARD_BLOCK = Registry.register(
            Registries.BLOCK, new Identifier(CrouchLockMod.MOD_ID, "investigation_board"),
            new InvestigationBoardBlock(AbstractBlock.Settings.create().strength(0.25F)
                    .nonOpaque().sounds(BlockSoundGroup.WOOD)));

    public static final Item INVESTIGATION_BOARD = Registry.register(
            Registries.ITEM, new Identifier(CrouchLockMod.MOD_ID, "investigation_board"),
            new BlockItem(INVESTIGATION_BOARD_BLOCK, new Item.Settings().maxCount(16)));

    @Override public void onInitialize() { }
}
