package kr.koala.crouchlock;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.itemgroup.v1.ItemGroupEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.fabricmc.fabric.api.object.builder.v1.block.entity.FabricBlockEntityTypeBuilder;
import net.minecraft.block.AbstractBlock;
import net.minecraft.block.Block;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.item.Item;
import net.minecraft.item.ItemGroups;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.sound.BlockSoundGroup;
import net.minecraft.util.Hand;
import net.minecraft.util.Identifier;

public final class SmartphoneMod implements ModInitializer {
    public static final Identifier SAVE_PACKET = new Identifier(CrouchLockMod.MOD_ID, "smartphone_save");

    public static final Block SMARTPHONE_BLOCK = Registry.register(
            Registries.BLOCK,
            new Identifier(CrouchLockMod.MOD_ID, "smartphone"),
            new SmartphoneBlock(AbstractBlock.Settings.create()
                    .strength(0.25F)
                    .sounds(BlockSoundGroup.METAL)
                    .nonOpaque())
    );

    public static final BlockEntityType<SmartphoneBlockEntity> SMARTPHONE_BLOCK_ENTITY = Registry.register(
            Registries.BLOCK_ENTITY_TYPE,
            new Identifier(CrouchLockMod.MOD_ID, "smartphone"),
            FabricBlockEntityTypeBuilder.create(SmartphoneBlockEntity::new, SMARTPHONE_BLOCK).build()
    );

    public static final Item SMARTPHONE = Registry.register(
            Registries.ITEM,
            new Identifier(CrouchLockMod.MOD_ID, "smartphone"),
            new SmartphoneItem(SMARTPHONE_BLOCK, new Item.Settings().maxCount(1))
    );

    @Override
    public void onInitialize() {
        ItemGroupEvents.modifyEntriesEvent(ItemGroups.TOOLS).register(entries -> entries.add(SMARTPHONE));

        ServerPlayNetworking.registerGlobalReceiver(SAVE_PACKET,
                (server, player, handler, buf, responseSender) -> {
                    final Hand hand;
                    final SmartphoneData data;
                    try {
                        hand = buf.readEnumConstant(Hand.class);
                        data = SmartphoneData.readPacket(buf);
                    } catch (RuntimeException ignored) {
                        return;
                    }

                    server.execute(() -> {
                        ItemStack stack = player.getStackInHand(hand);
                        if (!stack.isOf(SMARTPHONE)) {
                            return;
                        }
                        // A finalized phone is immutable, like a signed written book.
                        if (SmartphoneData.fromStack(stack).finalized()) {
                            return;
                        }
                        data.writeTo(stack);
                        player.currentScreenHandler.sendContentUpdates();
                    });
                });
    }
}
