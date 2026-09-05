package kr.koala.crouchlock;

import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.inventory.SimpleInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.screen.GenericContainerScreenHandler;
import net.minecraft.screen.ScreenHandlerType;
import net.minecraft.screen.slot.SlotActionType;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

import java.util.Map;
import java.util.function.Predicate;

public final class KeypadScreenHandler extends GenericContainerScreenHandler {
    private static final int DISPLAY_SLOT = 4;
    private static final int CLEAR_SLOT = 37;
    private static final int ZERO_SLOT = 40;
    private static final int CONFIRM_SLOT = 43;
    private static final Map<Integer, Integer> DIGIT_SLOTS = Map.of(
            12, 1, 13, 2, 14, 3,
            21, 4, 22, 5, 23, 6,
            30, 7, 31, 8, 32, 9,
            ZERO_SLOT, 0
    );

    private final SimpleInventory keypadInventory;
    private final Predicate<String> confirmAction;
    private final StringBuilder input = new StringBuilder(4);

    public KeypadScreenHandler(int syncId, PlayerInventory playerInventory, Predicate<String> confirmAction) {
        this(syncId, playerInventory, new SimpleInventory(45), confirmAction);
    }

    private KeypadScreenHandler(int syncId, PlayerInventory playerInventory,
                                SimpleInventory keypadInventory, Predicate<String> confirmAction) {
        super(ScreenHandlerType.GENERIC_9X5, syncId, playerInventory, keypadInventory, 5);
        this.keypadInventory = keypadInventory;
        this.confirmAction = confirmAction;
        refreshButtons();
    }

    @Override
    public void onSlotClick(int slotIndex, int button, SlotActionType actionType, PlayerEntity player) {
        if (slotIndex < 0 || slotIndex >= 45) {
            super.onSlotClick(slotIndex, button, actionType, player);
            return;
        }

        Integer digit = DIGIT_SLOTS.get(slotIndex);
        if (digit != null && input.length() < 4) {
            input.append(digit);
            refreshButtons();
            return;
        }

        if (slotIndex == CLEAR_SLOT) {
            input.setLength(0);
            refreshButtons();
            return;
        }

        if (slotIndex == CONFIRM_SLOT) {
            if (input.length() != 4) {
                player.sendMessage(Text.translatable("message.crouchlock.four_digits"), true);
                return;
            }

            boolean close = confirmAction.test(input.toString());
            if (close && player instanceof ServerPlayerEntity serverPlayer) {
                serverPlayer.closeHandledScreen();
            } else {
                input.setLength(0);
                refreshButtons();
            }
        }
    }

    @Override
    public ItemStack quickMove(PlayerEntity player, int slot) {
        return ItemStack.EMPTY;
    }

    private void refreshButtons() {
        for (int slot = 0; slot < keypadInventory.size(); slot++) {
            keypadInventory.setStack(slot, named(Items.BLACK_STAINED_GLASS_PANE, " ", Formatting.BLACK));
        }

        DIGIT_SLOTS.forEach((slot, digit) -> keypadInventory.setStack(
                slot,
                named(Items.STONE_BUTTON, Integer.toString(digit), Formatting.GOLD)
        ));

        String hidden = "*".repeat(input.length()) + "_".repeat(4 - input.length());
        keypadInventory.setStack(DISPLAY_SLOT,
                named(Items.PAPER, Text.translatable("screen.crouchlock.display", hidden), Formatting.AQUA));
        keypadInventory.setStack(CLEAR_SLOT,
                named(Items.RED_DYE, Text.translatable("screen.crouchlock.clear"), Formatting.RED));
        keypadInventory.setStack(CONFIRM_SLOT,
                named(Items.LIME_DYE, Text.translatable("screen.crouchlock.confirm"), Formatting.GREEN));
        keypadInventory.markDirty();
        sendContentUpdates();
    }

    private static ItemStack named(net.minecraft.item.Item item, String name, Formatting formatting) {
        return named(item, Text.literal(name), formatting);
    }

    private static ItemStack named(net.minecraft.item.Item item, Text name, Formatting formatting) {
        ItemStack stack = new ItemStack(item);
        stack.setCustomName(name.copy().formatted(formatting));
        return stack;
    }
}
