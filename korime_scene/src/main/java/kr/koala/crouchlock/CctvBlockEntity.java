package kr.koala.crouchlock;

import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.nbt.NbtList;
import net.minecraft.nbt.NbtString;
import net.minecraft.network.listener.ClientPlayPacketListener;
import net.minecraft.network.packet.Packet;
import net.minecraft.network.packet.s2c.play.BlockEntityUpdateS2CPacket;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

public final class CctvBlockEntity extends BlockEntity {
    public static final int MAX_RECORDS = 64;
    public static final int MAX_RECORD_LENGTH = 512;

    private final List<String> records = new ArrayList<>();
    private boolean finalized;

    public CctvBlockEntity(BlockPos pos, BlockState state) {
        super(SceneToolsMod.CCTV_BLOCK_ENTITY, pos, state);
    }

    public List<String> getRecords() {
        return List.copyOf(records);
    }

    public boolean isFinalized() {
        return finalized;
    }

    public void setEvidence(List<String> newRecords, boolean finalize) {
        if (finalized) return;

        records.clear();
        for (String line : newRecords) {
            String clean = line == null ? "" : line.trim();
            if (clean.isEmpty()) continue;
            if (clean.length() > MAX_RECORD_LENGTH) clean = clean.substring(0, MAX_RECORD_LENGTH);
            records.add(clean);
            if (records.size() >= MAX_RECORDS) break;
        }
        finalized = finalize;
        sync();
    }

    private void sync() {
        markDirty();
        if (world instanceof ServerWorld serverWorld) {
            serverWorld.getChunkManager().markForUpdate(pos);
        }
    }

    @Override
    protected void writeNbt(NbtCompound nbt) {
        super.writeNbt(nbt);
        NbtList list = new NbtList();
        for (String record : records) list.add(NbtString.of(record));
        nbt.put("Records", list);
        nbt.putBoolean("Finalized", finalized);
    }

    @Override
    public void readNbt(NbtCompound nbt) {
        super.readNbt(nbt);
        records.clear();
        if (nbt.contains("Records", NbtElement.LIST_TYPE)) {
            NbtList list = nbt.getList("Records", NbtElement.STRING_TYPE);
            for (int i = 0; i < list.size() && records.size() < MAX_RECORDS; i++) {
                String line = list.getString(i);
                if (line.length() > MAX_RECORD_LENGTH) line = line.substring(0, MAX_RECORD_LENGTH);
                records.add(line);
            }
        }
        finalized = nbt.getBoolean("Finalized");
    }

    @Override
    public NbtCompound toInitialChunkDataNbt() {
        return createNbt();
    }

    @Override
    public @Nullable Packet<ClientPlayPacketListener> toUpdatePacket() {
        return BlockEntityUpdateS2CPacket.create(this);
    }
}
