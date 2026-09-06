package kr.koala.crouchlock;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.event.player.UseBlockCallback;
import net.fabricmc.fabric.api.event.player.UseItemCallback;
import net.minecraft.client.MinecraftClient;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ActionResult;
import net.minecraft.util.TypedActionResult;

public final class SmartphoneClient implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        UseItemCallback.EVENT.register((player, world, hand) -> {
            ItemStack stack = player.getStackInHand(hand);
            if (!world.isClient || !stack.isOf(SmartphoneMod.SMARTPHONE)) {
                return TypedActionResult.pass(stack);
            }

            MinecraftClient client = MinecraftClient.getInstance();
            if (client.currentScreen == null) {
                client.setScreen(new SmartphoneScreenV2(hand, stack.copy()));
            }
            return TypedActionResult.success(stack);
        });

        UseBlockCallback.EVENT.register((player, world, hand, hit) -> {
            if (!world.isClient) {
                return ActionResult.PASS;
            }
            if (world.getBlockEntity(hit.getBlockPos()) instanceof SmartphoneBlockEntity phone) {
                MinecraftClient client = MinecraftClient.getInstance();
                if (client.currentScreen == null) {
                    client.setScreen(new SmartphoneScreenV2(phone.getData()));
                }
                return ActionResult.SUCCESS;
            }
            return ActionResult.PASS;
        });
    }
}
