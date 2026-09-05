package kr.koala.crouchlock;

import net.minecraft.client.item.TooltipContext;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtElement;
import net.minecraft.text.Text;
import net.minecraft.world.World;

import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public final class LockKeyItem extends Item {
    private static final String KEY_ID_NBT = "CrouchLockKeyId";

    public LockKeyItem(Settings settings) {
        super(settings);
    }

    public static UUID getOrCreateKeyId(ItemStack stack) {
        Optional<UUID> current = getKeyId(stack);
        if (current.isPresent()) {
            return current.get();
        }

        UUID created = UUID.randomUUID();
        stack.getOrCreateNbt().putString(KEY_ID_NBT, created.toString());
        return created;
    }

    public static Optional<UUID> getKeyId(ItemStack stack) {
        if (!stack.isOf(CrouchLockMod.LOCK_KEY) || !stack.hasNbt()) {
            return Optional.empty();
        }

        String raw = stack.getNbt().getString(KEY_ID_NBT);
        if (raw.isBlank() || !stack.getNbt().contains(KEY_ID_NBT, NbtElement.STRING_TYPE)) {
            return Optional.empty();
        }

        try {
            return Optional.of(UUID.fromString(raw));
        } catch (IllegalArgumentException ignored) {
            return Optional.empty();
        }
    }

    @Override
    public void appendTooltip(ItemStack stack, @Nullable World world, List<Text> tooltip, TooltipContext context) {
        getKeyId(stack).ifPresentOrElse(
                id -> tooltip.add(Text.translatable("item.crouchlock.lock_key.code", id.toString().substring(0, 8))),
                () -> tooltip.add(Text.translatable("item.crouchlock.lock_key.unassigned"))
        );
    }
}
