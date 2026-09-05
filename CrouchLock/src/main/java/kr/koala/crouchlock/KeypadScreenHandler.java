package kr.koala.crouchlock;

import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.screen.ArrayPropertyDelegate;
import net.minecraft.screen.PropertyDelegate;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.screen.slot.SlotActionType;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
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
            int keypadCountBefore = keypadCountInHands(player);
            boolean creativeSetup = player.getAbilities().creativeMode && keypadCountBefore > 0;
            boolean success = confirmAction.test(input.toString());
            int keypadCountAfter = keypadCountInHands(player);

            if (success) {
                // Setting a new keypad consumes the held keypad in survival. Do not treat that as
                // an "answer accepted" chime. Actual unlock/removal confirmations always chime.
                boolean consumedSetupKeypad = keypadCountAfter < keypadCountBefore;
                if (!consumedSetupKeypad && !creativeSetup && player instanceof ServerPlayerEntity serverPlayer) {
                    playSuccessChime(serverPlayer);
                }

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

    private static int keypadCountInHands(PlayerEntity player) {
        int count = 0;
        ItemStack main = player.getMainHandStack();
        ItemStack off = player.getOffHandStack();
        if (main.isOf(CrouchLockMod.KEYPAD)) {
            count += main.getCount();
        }
        if (off.isOf(CrouchLockMod.KEYPAD)) {
            count += off.getCount();
        }
        return count;
    }

    private static void playSuccessChime(ServerPlayerEntity player) {
        playChimeNote(player, 1.00F);
        scheduleChimeNote(player, 150L, 1.25F);
        scheduleChimeNote(player, 300L, 1.50F);
    }

    private static void scheduleChimeNote(ServerPlayerEntity player, long delayMillis, float pitch) {
        MinecraftServer server = player.getServer();
        if (server == null) {
            return;
        }
        CompletableFuture.delayedExecutor(delayMillis, TimeUnit.MILLISECONDS).execute(() ->
                server.execute(() -> {
                    if (!player.isRemoved()) {
                        playChimeNote(player, pitch);
                    }
                })
        );
    }

    private static void playChimeNote(ServerPlayerEntity player, float pitch) {
        ServerWorld world = player.getServerWorld();
        world.playSound(null, player.getBlockPos(), SoundEvents.BLOCK_NOTE_BLOCK_CHIME.value(),
                SoundCategory.BLOCKS, 1.15F, pitch);
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
