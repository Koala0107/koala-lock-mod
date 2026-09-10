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
    public static final float MIN_SIZE = 0.1F;
    public static final float MAX_SIZE = 50.0F;

    private String imageUrl = "";
    private float frameWidth = 1.0F;
    private float frameHeight = 1.0F;
    private ImageFrameAlignment alignment = ImageFrameAlignment.CENTER;
    private boolean flipHorizontal = false;
    private float rotationDegrees = 0.0F;

    public ImageFrameBlockEntity(BlockPos pos, BlockState state) {
        super(ImageFrameMod.IMAGE_FRAME_BLOCK_ENTITY, pos, state);
    }

    public String getImageUrl() { return imageUrl; }
    public float getFrameWidth() { return frameWidth; }
    public float getFrameHeight() { return frameHeight; }
    public ImageFrameAlignment getAlignment() { return alignment; }
    public boolean isFlipHorizontal() { return flipHorizontal; }
    public float getRotationDegrees() { return rotationDegrees; }

    public void configure(String url, float width, float height, ImageFrameAlignment alignment,
                          boolean flipHorizontal, float rotationDegrees) {
        String clean = url == null ? "" : url.trim();
        if (clean.length() > MAX_URL_LENGTH) clean = clean.substring(0, MAX_URL_LENGTH);
        imageUrl = clean;
        frameWidth = clampSize(width);
        frameHeight = clampSize(height);
        this.alignment = alignment == null ? ImageFrameAlignment.CENTER : alignment;
        this.flipHorizontal = flipHorizontal;
        this.rotationDegrees = normalizeRotation(rotationDegrees);
        markDirty();

        // Persist on the server and immediately push the new URL/size/alignment/flip/rotation state
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
        nbt.putFloat("FrameWidth", frameWidth);
        nbt.putFloat("FrameHeight", frameHeight);
        nbt.putInt("Alignment", alignment.ordinal());
        nbt.putBoolean("FlipHorizontal", flipHorizontal);
        nbt.putFloat("RotationDegrees", rotationDegrees);
    }

    @Override
    public void readNbt(NbtCompound nbt) {
        super.readNbt(nbt);
        imageUrl = nbt.getString("ImageUrl");
        // getFloat also reads the old integer values, preserving existing image frames.
        frameWidth = nbt.contains("FrameWidth") ? clampSize(nbt.getFloat("FrameWidth")) : 1.0F;
        frameHeight = nbt.contains("FrameHeight") ? clampSize(nbt.getFloat("FrameHeight")) : 1.0F;
        alignment = nbt.contains("Alignment") ? ImageFrameAlignment.fromId(nbt.getInt("Alignment")) : ImageFrameAlignment.CENTER;
        flipHorizontal = nbt.getBoolean("FlipHorizontal");
        rotationDegrees = nbt.contains("RotationDegrees") ? normalizeRotation(nbt.getFloat("RotationDegrees")) : 0.0F;
    }

    private static float clampSize(float value) {
        if (!Float.isFinite(value)) return 1.0F;
        float clamped = Math.max(MIN_SIZE, Math.min(MAX_SIZE, value));
        return Math.round(clamped * 10.0F) / 10.0F;
    }

    private static float normalizeRotation(float value) {
        if (!Float.isFinite(value)) return 0.0F;
        float normalized = value % 360.0F;
        if (normalized < 0.0F) normalized += 360.0F;
        return Math.round(normalized * 10.0F) / 10.0F;
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
