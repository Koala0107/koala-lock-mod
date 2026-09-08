package kr.koala.crouchlock;

import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.network.listener.ClientPlayPacketListener;
import net.minecraft.network.packet.Packet;
import net.minecraft.network.packet.s2c.play.BlockEntityUpdateS2CPacket;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import org.jetbrains.annotations.Nullable;

public final class EvidenceMagnifierBlockEntity extends BlockEntity {
    public static final int MAX_TEXT_LENGTH = 2048;

    private String evidenceText = "";
    private boolean saved;

    public EvidenceMagnifierBlockEntity(BlockPos pos, BlockState state) {
        super(SceneToolsMod.EVIDENCE_MAGNIFIER_BLOCK_ENTITY, pos, state);
    }

    public String getEvidenceText() { return evidenceText; }
    public boolean isSaved() { return saved; }

    public void saveEvidence(String text) {
        if (saved) return;
        String clean = text == null ? "" : text.trim();
        if (clean.length() > MAX_TEXT_LENGTH) clean = clean.substring(0, MAX_TEXT_LENGTH);
        evidenceText = clean;
        saved = true;
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
        nbt.putString("EvidenceText", evidenceText);
        nbt.putBoolean("Saved", saved);
    }

    @Override
    public void readNbt(NbtCompound nbt) {
        super.readNbt(nbt);
        evidenceText = nbt.getString("EvidenceText");
        if (evidenceText.length() > MAX_TEXT_LENGTH) evidenceText = evidenceText.substring(0, MAX_TEXT_LENGTH);
        saved = nbt.getBoolean("Saved");
    }

    @Override public NbtCompound toInitialChunkDataNbt() { return createNbt(); }
    @Override public @Nullable Packet<ClientPlayPacketListener> toUpdatePacket() { return BlockEntityUpdateS2CPacket.create(this); }
}
