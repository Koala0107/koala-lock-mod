package kr.koala.korime_scene;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.inventory.Inventory;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.text.Text;
import net.minecraft.util.Hand;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;

public final class EvidenceCollectionMod implements ModInitializer {
    public static final Identifier NOTE_SAVE_PACKET = new Identifier(KorimeSceneMod.MOD_ID, "note_save");
    public static final Identifier OPEN_CONTAINER_EVIDENCE_PACKET = new Identifier(KorimeSceneMod.MOD_ID, "evidence_container_open");
    public static final Identifier TAKE_CONTAINER_EVIDENCE_PACKET = new Identifier(KorimeSceneMod.MOD_ID, "evidence_container_take");
    public static final Identifier EXTRACT_POUCH_ITEM_PACKET = new Identifier(KorimeSceneMod.MOD_ID, "evidence_pouch_extract");

    public static final Item EVIDENCE_ENVELOPE = Registry.register(
            Registries.ITEM,
            new Identifier(KorimeSceneMod.MOD_ID, "evidence_pouch"),
            new EvidenceEnvelopeItem(new Item.Settings().maxCount(1))
    );

    public static final Item NOTE = Registry.register(
            Registries.ITEM,
            new Identifier(KorimeSceneMod.MOD_ID, "note"),
            new NoteItem(new Item.Settings().maxCount(1))
    );

    @Override
    public void onInitialize() {
        ServerPlayNetworking.registerGlobalReceiver(NOTE_SAVE_PACKET,
                (server, player, handler, buf, responseSender) -> {
                    final Hand hand;
                    final String body;
                    try {
                        hand = buf.readEnumConstant(Hand.class);
                        body = buf.readString(NoteData.MAX_BODY_LENGTH);
                    } catch (RuntimeException ignored) {
                        return;
                    }
                    server.execute(() -> {
                        ItemStack stack = player.getStackInHand(hand);
                        if (!stack.isOf(NOTE)) return;
                        NoteData.setBody(stack, body);
                        player.currentScreenHandler.sendContentUpdates();
                    });
                });

        ServerPlayNetworking.registerGlobalReceiver(TAKE_CONTAINER_EVIDENCE_PACKET,
                (server, player, handler, buf, responseSender) -> {
                    final Hand hand;
                    final BlockPos pos;
                    final int slot;
                    try {
                        hand = buf.readEnumConstant(Hand.class);
                        pos = buf.readBlockPos();
                        slot = buf.readVarInt();
                    } catch (RuntimeException ignored) {
                        return;
                    }

                    server.execute(() -> {
                        ItemStack pouch = player.getStackInHand(hand);
                        if (!pouch.isOf(EVIDENCE_ENVELOPE) || EvidenceEnvelopeData.isFull(pouch)) return;
                        if (player.squaredDistanceTo(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5) > 64.0) return;

                        BlockEntity blockEntity = player.getWorld().getBlockEntity(pos);
                        if (!(blockEntity instanceof Inventory inventory)) return;
                        if (slot < 0 || slot >= inventory.size()) return;

                        ItemStack source = inventory.getStack(slot);
                        if (source.isEmpty()) return;
                        if (!EvidenceEnvelopeData.addItemCopy(pouch, source)) return;

                        player.sendMessage(Text.literal("증거물을 파우치에 넣었습니다. (" + EvidenceEnvelopeData.getItemCount(pouch) + "/30)"), true);
                        player.currentScreenHandler.sendContentUpdates();
                    });
                });

        ServerPlayNetworking.registerGlobalReceiver(EXTRACT_POUCH_ITEM_PACKET,
                (server, player, handler, buf, responseSender) -> {
                    final Hand hand;
                    final int index;
                    try {
                        hand = buf.readEnumConstant(Hand.class);
                        index = buf.readVarInt();
                    } catch (RuntimeException ignored) {
                        return;
                    }
                    server.execute(() -> {
                        ItemStack pouch = player.getStackInHand(hand);
                        if (!pouch.isOf(EVIDENCE_ENVELOPE)) return;
                        ItemStack extracted = EvidenceEnvelopeData.removeStoredItem(pouch, index);
                        if (extracted.isEmpty()) return;
                        if (!player.getInventory().insertStack(extracted)) {
                            player.dropItem(extracted, false);
                        }
                        player.currentScreenHandler.sendContentUpdates();
                    });
                });
    }
}
