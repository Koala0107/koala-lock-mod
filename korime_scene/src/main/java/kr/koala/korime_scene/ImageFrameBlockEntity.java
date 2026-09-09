package kr.koala.korime_scene;

import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.network.listener.ClientPlayPacketListener;
import net.minecraft.network.packet.Packet;
import net.minecraft.network.packet.s2c.play.BlockEntityUpdateS2CPacket;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import org.jetbrains.annotations.Nullable;

public final class ImageFrameBlockEntity extends BlockEntity {
    public static final int MAX_URL_LENGTH = 2048;
    public static final int MAX_SIZE = 8;

    private String imageUrl = "";
    private int frameWidth = 1;
    private int frameHeight = 1;

    public ImageFrameBlockEntity(BlockPos pos, BlockState state) {
        super(ImageFrameMod.IMAGE_FRAME_BLOCK_ENTITY, pos, state);
    }

    public String getImageUrl() { return imageUrl; }
    public int getFrameWidth() { return frameWidth; }
    public int getFrameHeight() { return frameHeight; }

    public void configure(String url, int width, int height) {
        String clean = url == null ? "" : url.trim();
        if (clean.length() > MAX_URL_LENGTH) clean = clean.substring(0, MAX_URL_LENGTH);
        imageUrl = clean;
        frameWidth = Math.max(1, Math.min(MAX_SIZE, width));
        frameHeight = Math.max(1, Math.min(MAX_SIZE, height));
        markDirty();
        if (world instanceof ServerWorld serverWorld) {
            serverWorld.getChunkManager().markForUpdate(pos);
        }
    }

    @Override
    protected void writeNbt(NbtCompound nbt) {
        super.writeNbt(nbt);
        nbt.putString("ImageUrl", imageUrl);
        nbt.putInt("FrameWidth", frameWidth);
        nbt.putInt("FrameHeight", frameHeight);
    }

    @Override
    public void readNbt(NbtCompound nbt) {
        super.readNbt(nbt);
        imageUrl = nbt.getString("ImageUrl");
        frameWidth = nbt.contains("FrameWidth") ? Math.max(1, Math.min(MAX_SIZE, nbt.getInt("FrameWidth"))) : 1;
        frameHeight = nbt.contains("FrameHeight") ? Math.max(1, Math.min(MAX_SIZE, nbt.getInt("FrameHeight"))) : 1;
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
