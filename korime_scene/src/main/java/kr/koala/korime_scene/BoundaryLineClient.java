package kr.koala.korime_scene;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.blockrenderlayer.v1.BlockRenderLayerMap;
import net.minecraft.client.render.RenderLayer;

public final class BoundaryLineClient implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        BlockRenderLayerMap.INSTANCE.putBlocks(
                RenderLayer.getCutout(),
                BoundaryLineMod.HORIZONTAL,
                BoundaryLineMod.VERTICAL,
                BoundaryLineMod.CORNER,
                BoundaryLineMod.L_CORNER
        );
    }
}
