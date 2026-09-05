package kr.koala.crouchlock;

import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.screen.slot.SlotActionType;

import java.util.function.Predicate;

/** Server-side state for the button-based keypad screen. */
public final class KeypadScreenHandler extends ScreenHandler {
    public static final int CLEAR_BUTTON = 10;
    public static final int CONFIRM_BUTTON = 11;

    private final Predicate<String> confirmAction;
    private final StringBuilder input = new StringBuilder(4);

    public KeypadScreenHandler(int syncId, PlayerInventory inventory) {
        this(syncId, inventory, code -> true);
    }

    public KeypadScreenHandler(int syncId, PlayerInventory inventory, Predicate<String> confirmAction) {
        super(CrouchLockMod.KEYPAD_SCREEN_HANDLER, syncId);
        this.confirmAction = confirmAction;
    }

    @Override
    public boolean onButtonClick(PlayerEntity player, int id) {
        if (id >= 0 && id <= 9) {
            if (input.length() < 4) {
                input.append(id);
            }
            return true;
        }

        if (id == CLEAR_BUTTON) {
            input.setLength(0);
            return true;
        }

        if (id == CONFIRM_BUTTON && input.length() == 4) {
            boolean close = confirmAction.test(input.toString());
            if (!close) {
                input.setLength(0);
            }
            return close;
        }

        return false;
    }

    @Override
    public void onSlotClick(int slotIndex, int button, SlotActionType actionType, PlayerEntity player) {
        // This screen deliberately has no inventory slots. All interaction is through GUI buttons.
    }

    @Override
    public boolean canUse(PlayerEntity player) {
        return true;
    }
}
