package kr.koala.korime_scene;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.event.player.UseBlockCallback;
import net.fabricmc.fabric.api.event.player.UseItemCallback;
import net.minecraft.client.MinecraftClient;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ActionResult;
import net.minecraft.util.TypedActionResult;

public final class EvidenceCollectionClient implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        UseItemCallback.EVENT.register((player, world, hand) -> {
            ItemStack stack = player.getStackInHand(hand);
            if (!world.isClient()) return TypedActionResult.pass(stack);

            MinecraftClient client = MinecraftClient.getInstance();
            if (client.currentScreen != null) return TypedActionResult.pass(stack);

            if (stack.isOf(EvidenceCollectionMod.EVIDENCE_ENVELOPE)) {
                client.setScreen(new EvidenceEnvelopeScreen(hand, stack.copy()));
                return TypedActionResult.success(stack);
            }
            if (stack.isOf(EvidenceCollectionMod.NOTE)) {
                client.setScreen(new NoteScreen(hand, stack.copy()));
                return TypedActionResult.success(stack);
            }
            return TypedActionResult.pass(stack);
        });

        UseBlockCallback.EVENT.register((player, world, hand, hit) -> {
            if (!world.isClient()) return ActionResult.PASS;
            ItemStack stack = player.getStackInHand(hand);
            MinecraftClient client = MinecraftClient.getInstance();
            if (client.currentScreen != null) return ActionResult.PASS;

            // Evidence envelopes intentionally do NOT open here: right-clicking a block
            // is reserved for adding another copied evidence item to the 30-slot bag.
            if (stack.isOf(EvidenceCollectionMod.NOTE)) {
                client.setScreen(new NoteScreen(hand, stack.copy()));
                return ActionResult.SUCCESS;
            }
            return ActionResult.PASS;
        });
    }
}
