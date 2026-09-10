package kr.koala.korime_scene;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.blockrenderlayer.v1.BlockRenderLayerMap;
import net.fabricmc.fabric.api.event.player.UseBlockCallback;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.block.entity.BlockEntityRendererFactories;
import net.minecraft.util.ActionResult;

public final class ImageFrameClient implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        BlockRenderLayerMap.INSTANCE.putBlock(ImageFrameMod.IMAGE_FRAME, RenderLayer.getCutout());
        BlockEntityRendererFactories.register(ImageFrameMod.IMAGE_FRAME_BLOCK_ENTITY, ImageFrameBlockEntityRenderer::new);

        UseBlockCallback.EVENT.register((player, world, hand, hit) -> {
            if (!world.isClient) return ActionResult.PASS;
            if (!(world.getBlockEntity(hit.getBlockPos()) instanceof ImageFrameBlockEntity frame)) return ActionResult.PASS;

            MinecraftClient client = MinecraftClient.getInstance();
            if (client.currentScreen != null) return ActionResult.PASS;

            client.setScreen(new ImageFrameScreen(
                    hit.getBlockPos(),
                    frame.getImageUrl(),
                    frame.getFrameWidth(),
                    frame.getFrameHeight(),
                    frame.getAlignment(),
                    frame.isFlipHorizontal()));
            return ActionResult.SUCCESS;
        });
    }
}
