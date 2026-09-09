package kr.koala.korime_scene;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.player.UseEntityCallback;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.Identifier;

public final class EvidenceCollectionMod implements ModInitializer {
    public static final Identifier ENVELOPE_NOTE_PACKET = new Identifier(KorimeSceneMod.MOD_ID, "evidence_envelope_note");
    public static final Identifier NOTE_SAVE_PACKET = new Identifier(KorimeSceneMod.MOD_ID, "note_save");

    public static final Item EVIDENCE_ENVELOPE = Registry.register(
            Registries.ITEM,
            new Identifier(KorimeSceneMod.MOD_ID, "evidence_envelope"),
            new EvidenceEnvelopeItem(new Item.Settings().maxCount(1))
    );

    public static final Item NOTE = Registry.register(
            Registries.ITEM,
            new Identifier(KorimeSceneMod.MOD_ID, "note"),
            new NoteItem(new Item.Settings().maxCount(1))
    );

    @Override
    public void onInitialize() {
        UseEntityCallback.EVENT.register((player, world, hand, entity, hitResult) -> {
            ItemStack stack = player.getStackInHand(hand);
            if (!stack.isOf(EVIDENCE_ENVELOPE) || EvidenceEnvelopeData.hasStoredItem(stack) || entity == player) {
                return ActionResult.PASS;
            }
            if (world.isClient()) return ActionResult.SUCCESS;

            if (EvidenceEnvelopeData.captureEntity(stack, entity)) {
                player.sendMessage(Text.literal("증거물을 봉투에 넣었습니다."), true);
                return ActionResult.SUCCESS;
            }
            return ActionResult.PASS;
        });

        ServerPlayNetworking.registerGlobalReceiver(ENVELOPE_NOTE_PACKET,
                (server, player, handler, buf, responseSender) -> {
                    final Hand hand;
                    final String note;
                    try {
                        hand = buf.readEnumConstant(Hand.class);
                        note = buf.readString(EvidenceEnvelopeData.MAX_NOTE_LENGTH);
                    } catch (RuntimeException ignored) {
                        return;
                    }
                    server.execute(() -> {
                        ItemStack stack = player.getStackInHand(hand);
                        if (!stack.isOf(EVIDENCE_ENVELOPE) || !EvidenceEnvelopeData.hasStoredItem(stack)) return;
                        EvidenceEnvelopeData.setNote(stack, note);
                        player.currentScreenHandler.sendContentUpdates();
                    });
                });

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
    }
}
