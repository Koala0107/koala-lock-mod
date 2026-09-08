package kr.koala.crouchlock;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.player.UseBlockCallback;
import net.minecraft.block.BlockState;
import net.minecraft.block.ChestBlock;
import net.minecraft.block.enums.ChestType;
import net.minecraft.item.ItemStack;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.state.property.Properties;
import net.minecraft.util.ActionResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;

import java.util.Optional;

/** Restores the key-unlock sound behavior without changing lock display placement. */
public final class KeyUnlockSound implements ModInitializer {
    @Override
    public void onInitialize() {
        UseBlockCallback.EVENT.register((player, world, hand, hit) -> {
            if (world.isClient || !(world instanceof ServerWorld serverWorld) || player.isSneaking()) {
                return ActionResult.PASS;
            }

            Optional<LockState.LockEntry> lock = findLock(serverWorld, hit.getBlockPos());
            if (lock.isEmpty() || !CrouchLockMod.KEY_LOCK.equals(lock.get().type())) {
                return ActionResult.PASS;
            }

            ItemStack held = player.getStackInHand(hand);
            boolean correct = LockKeyItem.getKeyId(held)
                    .map(id -> id.toString().equals(lock.get().credential()))
                    .orElse(false);
            if (!correct) {
                return ActionResult.PASS;
            }

            serverWorld.playSound(null, hit.getBlockPos(), SoundEvents.BLOCK_ANVIL_LAND,
                    SoundCategory.PLAYERS, 0.55F, 1.75F);
            return ActionResult.PASS;
        });
    }

    private static Optional<LockState.LockEntry> findLock(ServerWorld world, BlockPos pos) {
        LockState lockState = LockState.get(world);
        Optional<LockState.LockEntry> direct = lockState.get(pos);
        if (direct.isPresent()) return direct;

        BlockState state = world.getBlockState(pos);
        if (!(state.getBlock() instanceof ChestBlock)
                || !state.contains(Properties.CHEST_TYPE)
                || state.get(Properties.CHEST_TYPE) == ChestType.SINGLE) {
            return Optional.empty();
        }

        for (Direction direction : Direction.Type.HORIZONTAL) {
            BlockPos neighborPos = pos.offset(direction);
            BlockState neighbor = world.getBlockState(neighborPos);
            if (!neighbor.isOf(state.getBlock())
                    || !neighbor.contains(Properties.CHEST_TYPE)
                    || neighbor.get(Properties.CHEST_TYPE) == ChestType.SINGLE) continue;
            Optional<LockState.LockEntry> linked = lockState.get(neighborPos);
            if (linked.isPresent()) return linked;
        }
        return Optional.empty();
    }
}
