package kr.koala.korime_scene;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.fabricmc.fabric.api.object.builder.v1.block.entity.FabricBlockEntityTypeBuilder;
import net.minecraft.block.AbstractBlock;
import net.minecraft.block.Block;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.item.Item;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.sound.BlockSoundGroup;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;

public final class ImageFrameMod implements ModInitializer {
    public static final Identifier SAVE_PACKET = new Identifier(KorimeSceneMod.MOD_ID, "image_frame_save");

    public static final Block IMAGE_FRAME = Registry.register(
            Registries.BLOCK,
            new Identifier(KorimeSceneMod.MOD_ID, "image_frame"),
            new ImageFrameBlock(AbstractBlock.Settings.create()
                    .strength(0.5F)
                    .sounds(BlockSoundGroup.WOOD)
                    .nonOpaque()
                    .noCollision())
    );

    public static final BlockEntityType<ImageFrameBlockEntity> IMAGE_FRAME_BLOCK_ENTITY = Registry.register(
            Registries.BLOCK_ENTITY_TYPE,
            new Identifier(KorimeSceneMod.MOD_ID, "image_frame"),
            FabricBlockEntityTypeBuilder.create(ImageFrameBlockEntity::new, IMAGE_FRAME).build()
    );

    public static final Item IMAGE_FRAME_ITEM = Registry.register(
            Registries.ITEM,
            new Identifier(KorimeSceneMod.MOD_ID, "image_frame"),
            new ImageFrameItem(IMAGE_FRAME, new Item.Settings())
    );

    @Override
    public void onInitialize() {
        ServerPlayNetworking.registerGlobalReceiver(SAVE_PACKET,
                (server, player, handler, buf, responseSender) -> {
                    final BlockPos pos;
                    final String url;
                    final int width;
                    final int height;
                    final int alignmentId;
                    try {
                        pos = buf.readBlockPos();
                        url = buf.readString(ImageFrameBlockEntity.MAX_URL_LENGTH);
                        width = buf.readUnsignedByte();
                        height = buf.readUnsignedByte();
                        alignmentId = buf.readUnsignedByte();
                    } catch (RuntimeException ignored) {
                        return;
                    }

                    server.execute(() -> {
                        if (!player.getWorld().getBlockState(pos).isOf(IMAGE_FRAME)) return;
                        if (player.squaredDistanceTo(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5) > 64.0) return;
                        if (width < 1 || width > ImageFrameBlockEntity.MAX_SIZE) return;
                        if (height < 1 || height > ImageFrameBlockEntity.MAX_SIZE) return;
                        if (!isAllowedUrl(url)) return;
                        if (!(player.getWorld().getBlockEntity(pos) instanceof ImageFrameBlockEntity frame)) return;
                        frame.configure(url, width, height, ImageFrameAlignment.fromId(alignmentId));
                    });
                });
    }

    private static boolean isAllowedUrl(String value) {
        if (value == null || value.isBlank()) return true;
        String lower = value.trim().toLowerCase(java.util.Locale.ROOT);
        return lower.startsWith("https://") || lower.startsWith("http://");
    }
}
