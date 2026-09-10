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
    public static final int MAX_SIZE = 20;

    private String imageUrl = "";
    private int frameWidth = 1;
    private int frameHeight = 1;
    private ImageFrameAlignment alignment = ImageFrameAlignment.CENTER;
    private boolean flipHorizontal = false;

    public ImageFrameBlockEntity(BlockPos pos, BlockState state) {
        super(ImageFrameMod.IMAGE_FRAME_BLOCK_ENTITY, pos, state);
    }

    public String getImageUrl() { return imageUrl; }
    public int getFrameWidth() { return frameWidth; }
    public int getFrameHeight() { return frameHeight; }
    public ImageFrameAlignment getAlignment() { return alignment; }
    public boolean isFlipHorizontal() { return flipHorizontal; }

    public void configure(String url, int width, int height, ImageFrameAlignment alignment, boolean flipHorizontal) {
        String clean = url == null ? "" : url.trim();
        if (clean.length() > MAX_URL_LENGTH) clean = clean.substring(0, MAX_URL_LENGTH);
        imageUrl = clean;
        frameWidth = Math.max(1, Math.min(MAX_SIZE, width));
        frameHeight = Math.max(1, Math.min(MAX_SIZE, height));
        this.alignment = alignment == null ? ImageFrameAlignment.CENTER : alignment;
        this.flipHorizontal = flipHorizontal;
        markDirty();

        // Persist on the server and immediately push the new URL/size/alignment/flip state
        // to every client tracking this chunk, so multiplayer players see the same image.
        if (world instanceof ServerWorld serverWorld) {
            BlockState state = getCachedState();
            serverWorld.updateListeners(pos, state, state, 3);
            serverWorld.getChunkManager().markForUpdate(pos);
        }
    }

    @Override
    protected void writeNbt(NbtCompound nbt) {
        super.writeNbt(nbt);
        nbt.putString("ImageUrl", imageUrl);
        nbt.putInt("FrameWidth", frameWidth);
        nbt.putInt("FrameHeight", frameHeight);
        nbt.putInt("Alignment", alignment.ordinal());
        nbt.putBoolean("FlipHorizontal", flipHorizontal);
    }

    @Override
    public void readNbt(NbtCompound nbt) {
        super.readNbt(nbt);
        imageUrl = nbt.getString("ImageUrl");
        frameWidth = nbt.contains("FrameWidth") ? Math.max(1, Math.min(MAX_SIZE, nbt.getInt("FrameWidth"))) : 1;
        frameHeight = nbt.contains("FrameHeight") ? Math.max(1, Math.min(MAX_SIZE, nbt.getInt("FrameHeight"))) : 1;
        alignment = nbt.contains("Alignment") ? ImageFrameAlignment.fromId(nbt.getInt("Alignment")) : ImageFrameAlignment.CENTER;
        flipHorizontal = nbt.getBoolean("FlipHorizontal");
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
