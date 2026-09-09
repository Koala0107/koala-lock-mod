package kr.koala.korime_scene;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.blockrenderlayer.v1.BlockRenderLayerMap;
import net.fabricmc.fabric.api.event.player.AttackBlockCallback;
import net.fabricmc.fabric.api.event.player.UseBlockCallback;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.util.ActionResult;

public final class SceneToolsClient implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        BlockRenderLayerMap.INSTANCE.putBlock(SceneToolsMod.CCTV, RenderLayer.getCutout());
        BlockRenderLayerMap.INSTANCE.putBlock(SceneToolsMod.EVIDENCE_MAGNIFIER, RenderLayer.getCutout());

        AttackBlockCallback.EVENT.register((player, world, hand, pos, direction) -> {
            if (!world.isClient) return ActionResult.PASS;
            if (!world.getBlockState(pos).isOf(SceneToolsMod.EVIDENCE_MAGNIFIER)) return ActionResult.PASS;

            MinecraftClient client = MinecraftClient.getInstance();
            if (client.currentScreen == null) {
                client.setScreen(new EvidenceBreakConfirmScreen(pos));
            }
            return ActionResult.FAIL;
        });

        UseBlockCallback.EVENT.register((player, world, hand, hit) -> {
            if (!world.isClient) return ActionResult.PASS;

            MinecraftClient client = MinecraftClient.getInstance();
            if (client.currentScreen != null) return ActionResult.PASS;

            if (world.getBlockEntity(hit.getBlockPos()) instanceof SafeBlockEntity safe) {
                if (safe.isCombinationSet()) {
                    client.setScreen(new SafeDialScreen(hit.getBlockPos()));
                } else {
                    client.setScreen(new SafeSetupScreen(hit.getBlockPos()));
                }
                return ActionResult.SUCCESS;
            }

            if (world.getBlockEntity(hit.getBlockPos()) instanceof EvidenceMagnifierBlockEntity evidence) {
                if (!evidence.isSaved()) {
                    client.setScreen(new EvidenceMagnifierScreen(hit.getBlockPos()));
                    return ActionResult.SUCCESS;
                }
                return ActionResult.PASS;
            }

            if (world.getBlockEntity(hit.getBlockPos()) instanceof CctvBlockEntity cctv) {
                if (cctv.isFinalized()) {
                    client.setScreen(new CctvScreen(cctv.getRecords()));
                } else {
                    client.setScreen(new CctvEditorScreen(hit.getBlockPos(), cctv.getRecords()));
                }
                return ActionResult.SUCCESS;
            }

            if (world.getBlockState(hit.getBlockPos()).isOf(SceneToolsMod.ITEM_EDITOR)) {
                client.setScreen(new ItemEditorScreen(hit.getBlockPos(), player.getMainHandStack().copy()));
                return ActionResult.SUCCESS;
            }

            return ActionResult.PASS;
        });
    }
}
