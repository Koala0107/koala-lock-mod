package kr.koala.crouchlock;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.player.UseBlockCallback;
import net.minecraft.item.ItemStack;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.ActionResult;

import java.util.Optional;

/** Plays a short metallic confirmation when the correct physical key opens a locked block. */
public final class KeyUnlockSound implements ModInitializer {
    @Override
    public void onInitialize() {
        UseBlockCallback.EVENT.register((player, world, hand, hit) -> {
            if (world.isClient || !(world instanceof ServerWorld serverWorld) || player.isSneaking()) {
                return ActionResult.PASS;
            }

            Optional<LockState.LockEntry> lock = LockState.get(serverWorld).get(hit.getBlockPos());
            if (lock.isEmpty() || !CrouchLockMod.KEY_LOCK.equals(lock.get().type())) {
                return ActionResult.PASS;
            }

            ItemStack held = player.getStackInHand(hand);
            boolean matches = LockKeyItem.getKeyId(held)
                    .map(id -> id.toString().equals(lock.get().credential()))
                    .orElse(false);
            if (!matches) {
                return ActionResult.PASS;
            }

            serverWorld.playSound(null, hit.getBlockPos(), SoundEvents.BLOCK_ANVIL_LAND,
                    SoundCategory.BLOCKS, 0.55F, 1.75F);
            return ActionResult.PASS;
        });
    }
}
