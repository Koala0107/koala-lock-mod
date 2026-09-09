package kr.koala.korime_scene;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.event.player.UseBlockCallback;
import net.fabricmc.fabric.api.event.player.UseItemCallback;
import net.minecraft.client.MinecraftClient;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.TypedActionResult;
import net.minecraft.util.math.BlockPos;

import java.util.ArrayList;
import java.util.List;

public final class EvidenceCollectionClient implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        ClientPlayNetworking.registerGlobalReceiver(EvidenceCollectionMod.OPEN_CONTAINER_EVIDENCE_PACKET,
                (client, handler, buf, responseSender) -> {
                    final Hand hand;
                    final BlockPos pos;
                    final List<ItemStack> items = new ArrayList<>();
                    try {
                        hand = buf.readEnumConstant(Hand.class);
                        pos = buf.readBlockPos();
                        int size = buf.readVarInt();
                        if (size < 0 || size > 256) return;
                        for (int i = 0; i < size; i++) items.add(buf.readItemStack());
                    } catch (RuntimeException ignored) {
                        return;
                    }
                    client.execute(() -> client.setScreen(new ContainerEvidencePickerScreen(hand, pos, items)));
                });

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

            // Shift-right-click with an evidence envelope is handled by the item itself
            // and only works on inventory block entities. Normal blocks remain untouched.
            if (stack.isOf(EvidenceCollectionMod.NOTE)) {
                client.setScreen(new NoteScreen(hand, stack.copy()));
                return ActionResult.SUCCESS;
            }
            return ActionResult.PASS;
        });
    }
}
