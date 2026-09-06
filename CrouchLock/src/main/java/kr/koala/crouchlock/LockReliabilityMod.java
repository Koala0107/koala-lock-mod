package kr.koala.crouchlock;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.event.player.PlayerBlockBreakEvents;
import net.minecraft.block.BlockState;
import net.minecraft.block.DoorBlock;
import net.minecraft.entity.Entity;
import net.minecraft.registry.Registries;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

public final class LockReliabilityMod implements ModInitializer {
    @Override
    public void onInitialize() {
        PlayerBlockBreakEvents.AFTER.register((world, player, pos, state, blockEntity) -> {
            if (world instanceof ServerWorld serverWorld) cleanupAfterBreak(serverWorld, pos);
        });
        ServerTickEvents.END_WORLD_TICK.register(world -> {
            LockState state = LockState.get(world);
            for (LockState.RemovedLock removed : state.pruneInvalid(world)) {
                removeMarkers(world, removed.lockId());
            }
        });
    }

    private static void cleanupAfterBreak(ServerWorld world, BlockPos brokenPos) {
        LockState state = LockState.get(world);
        Set<UUID> removeIds = new HashSet<>();
        for (var entry : state.entries()) {
            BlockPos lockPos = BlockPos.fromLong(entry.getKey());
            if (!world.isChunkLoaded(lockPos)) continue;
            LockState.LockEntry lock = entry.getValue();
            BlockState current = world.getBlockState(lockPos);
            String currentId = Registries.BLOCK.getId(current.getBlock()).toString();
            if (lockPos.equals(brokenPos)
                    || !currentId.equals(lock.blockId())
                    || !CrouchLockMod.isLockable(world, lockPos, current)
                    || (current.getBlock() instanceof DoorBlock && lockPos.down().equals(brokenPos))) {
                removeIds.add(lock.lockId());
            }
        }
        for (UUID lockId : removeIds) {
            state.removeLock(lockId);
            removeMarkers(world, lockId);
        }
    }

    private static void removeMarkers(ServerWorld world, UUID lockId) {
        String lockTag = CrouchLockMod.MOD_ID + ":" + lockId;
        List<Entity> remove = new ArrayList<>();
        for (Entity entity : world.iterateEntities()) {
            if (entity.getCommandTags().contains(CrouchLockMod.MARKER_TAG)
                    && entity.getCommandTags().contains(lockTag)) remove.add(entity);
        }
        for (Entity entity : remove) entity.discard();
    }
}
