package kr.koala.crouchlock;

import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.screen.ArrayPropertyDelegate;
import net.minecraft.screen.PropertyDelegate;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.screen.slot.SlotActionType;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.sound.SoundEvents;

import java.util.function.Predicate;

/** Server-side state for the button-based keypad screen. */
public final class KeypadScreenHandler extends ScreenHandler {
    public static final int MIN_DIGITS = 1;
    public static final int MAX_DIGITS = 8;
    public static final int CLEAR_BUTTON = 10;
    public static final int CONFIRM_BUTTON = 11;

    private final Predicate<String> confirmAction;
    private final StringBuilder input = new StringBuilder(MAX_DIGITS);
    private final PropertyDelegate properties;

    public KeypadScreenHandler(int syncId, PlayerInventory inventory) {
        this(syncId, inventory, code -> true, new ArrayPropertyDelegate(1));
    }

    public KeypadScreenHandler(int syncId, PlayerInventory inventory, Predicate<String> confirmAction) {
        this(syncId, inventory, confirmAction, new ArrayPropertyDelegate(1));
    }

    private KeypadScreenHandler(int syncId, PlayerInventory inventory, Predicate<String> confirmAction,
                                PropertyDelegate properties) {
        super(CrouchLockMod.KEYPAD_SCREEN_HANDLER, syncId);
        this.confirmAction = confirmAction;
        this.properties = properties;
        addProperties(properties);
    }

    @Override
    public boolean onButtonClick(PlayerEntity player, int id) {
        if (id >= 0 && id <= 9) {
            if (input.length() < MAX_DIGITS) {
                input.append(id);
            }
            return true;
        }

        if (id == CLEAR_BUTTON) {
            input.setLength(0);
            return true;
        }

        if (id == CONFIRM_BUTTON
                && input.length() >= MIN_DIGITS
                && input.length() <= MAX_DIGITS) {
            boolean success = confirmAction.test(input.toString());
            if (success) {
                player.playSound(SoundEvents.BLOCK_NOTE_BLOCK_CHIME.value(), player.getSoundCategory(), 0.9F, 0.9F);
                player.playSound(SoundEvents.ENTITY_PLAYER_LEVELUP, player.getSoundCategory(), 0.55F, 1.25F);
                input.setLength(0);
                if (player instanceof ServerPlayerEntity serverPlayer) {
                    serverPlayer.closeHandledScreen();
                }
            } else {
                input.setLength(0);
                properties.set(0, properties.get(0) + 1);
            }
            return true;
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

    @Override
    public ItemStack quickMove(PlayerEntity player, int slot) {
        return ItemStack.EMPTY;
    }

    public int getErrorCounter() {
        return properties.get(0);
    }
}
