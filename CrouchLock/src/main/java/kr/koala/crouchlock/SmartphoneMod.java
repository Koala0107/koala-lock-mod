package kr.koala.crouchlock;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.itemgroup.v1.ItemGroupEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.item.Item;
import net.minecraft.item.ItemGroups;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.util.Hand;
import net.minecraft.util.Identifier;

public final class SmartphoneMod implements ModInitializer {
    public static final Identifier SAVE_PACKET = new Identifier(CrouchLockMod.MOD_ID, "smartphone_save");

    public static final Item SMARTPHONE = Registry.register(
            Registries.ITEM,
            new Identifier(CrouchLockMod.MOD_ID, "smartphone"),
            new SmartphoneItem(new Item.Settings().maxCount(1))
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
                        if (stack.isOf(SMARTPHONE)) {
                            data.writeTo(stack);
                            player.currentScreenHandler.sendContentUpdates();
                        }
                    });
                });
    }
}
