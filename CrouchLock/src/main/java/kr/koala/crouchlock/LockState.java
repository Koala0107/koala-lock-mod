package kr.koala.crouchlock;

import net.minecraft.block.BlockState;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.nbt.NbtList;
import net.minecraft.registry.Registries;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.PersistentState;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

public final class LockState extends PersistentState {
    private static final Type<LockState> TYPE = new Type<>(LockState::new, LockState::fromNbt, null);
    private static final String STORAGE_KEY = CrouchLockMod.MOD_ID + "_locks";

    private final Map<Long, LockEntry> locks = new HashMap<>();

    public static LockState get(ServerWorld world) {
        return world.getPersistentStateManager().getOrCreate(TYPE, STORAGE_KEY);
    }

    public Optional<LockEntry> get(BlockPos pos) {
        return Optional.ofNullable(locks.get(pos.asLong()));
    }

    public Collection<Map.Entry<Long, LockEntry>> entries() {
        return locks.entrySet();
    }

    public void put(BlockPos pos, LockEntry entry) {
        locks.put(pos.asLong(), entry);
        markDirty();
    }

    public void removeLock(UUID lockId) {
        boolean changed = locks.values().removeIf(entry -> entry.lockId().equals(lockId));
        if (changed) {
            markDirty();
        }
    }

    public void pruneInvalid(ServerWorld world) {
        boolean changed = false;
        var iterator = locks.entrySet().iterator();

        while (iterator.hasNext()) {
            Map.Entry<Long, LockEntry> mapEntry = iterator.next();
            BlockPos pos = BlockPos.fromLong(mapEntry.getKey());
            if (!world.isChunkLoaded(pos)) {
                continue;
            }

            LockEntry lock = mapEntry.getValue();
            BlockState current = world.getBlockState(pos);
            String currentBlockId = Registries.BLOCK.getId(current.getBlock()).toString();

            if (!currentBlockId.equals(lock.blockId()) || !CrouchLockMod.isLockable(world, pos, current)) {
                iterator.remove();
                changed = true;
            }
        }

        if (changed) {
            markDirty();
        }
    }

    @Override
    public NbtCompound writeNbt(NbtCompound nbt) {
        NbtList list = new NbtList();
        locks.forEach((packedPos, entry) -> {
            NbtCompound value = new NbtCompound();
            value.putLong("Pos", packedPos);
            value.putString("Type", entry.type());
            value.putString("Credential", entry.credential());
            value.putString("Lock", entry.lockId().toString());
            value.putString("Owner", entry.ownerId().toString());
            value.putString("Block", entry.blockId());
            list.add(value);
        });
        nbt.put("Locks", list);
        return nbt;
    }

    private static LockState fromNbt(NbtCompound nbt) {
        LockState state = new LockState();
        NbtList list = nbt.getList("Locks", NbtElement.COMPOUND_TYPE);

        for (int index = 0; index < list.size(); index++) {
            NbtCompound value = list.getCompound(index);
            try {
                long pos = value.getLong("Pos");
                String type = value.contains("Type", NbtElement.STRING_TYPE)
                        ? value.getString("Type")
                        : CrouchLockMod.KEY_LOCK;
                String credential = value.contains("Credential", NbtElement.STRING_TYPE)
                        ? value.getString("Credential")
                        : value.getString("Key");
                UUID lockId = UUID.fromString(value.getString("Lock"));
                UUID ownerId = UUID.fromString(value.getString("Owner"));
                String blockId = value.getString("Block");
                if (!credential.isBlank()) {
                    state.locks.put(pos, new LockEntry(type, credential, lockId, ownerId, blockId));
                }
            } catch (IllegalArgumentException ignored) {
                // Skip malformed entries instead of preventing the world from loading.
            }
        }
        return state;
    }

    public record LockEntry(String type, String credential, UUID lockId, UUID ownerId, String blockId) {
    }
}
