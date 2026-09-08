package kr.koala.korime_scene;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.blockrenderlayer.v1.BlockRenderLayerMap;
import net.fabricmc.fabric.api.client.rendering.v1.ArmorRenderer;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.model.ModelPart;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.entity.model.BipedEntityModel;
import net.minecraft.client.render.entity.model.EntityModelLayers;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.LivingEntity;
import net.minecraft.util.Identifier;

public final class DetectiveItemsClient implements ClientModInitializer {
    private static final Identifier LAYER_1 = new Identifier(KorimeSceneMod.MOD_ID, "textures/models/armor/detective_layer_1.png");

    private BipedEntityModel<LivingEntity> outerArmorModel;

    @Override
    public void onInitializeClient() {
        BlockRenderLayerMap.INSTANCE.putBlock(DetectiveItemsMod.MAGNIFYING_GLASS_BLOCK, RenderLayer.getCutout());
        BlockRenderLayerMap.INSTANCE.putBlock(DetectiveItemsMod.INVESTIGATION_BOARD_BLOCK, RenderLayer.getCutout());

        ArmorRenderer.register((matrices, vertexConsumers, stack, entity, slot, light, contextModel) -> {
            BipedEntityModel<LivingEntity> model = getOuterArmorModel();
            if (model == null) return;

            contextModel.copyBipedStateTo(model);
            model.setVisible(false);
            switch (slot) {
                case HEAD -> {
                    model.head.visible = true;
                    model.hat.visible = true;
                }
                case CHEST -> {
                    model.body.visible = true;
                    model.rightArm.visible = true;
                    model.leftArm.visible = true;
                }
                default -> { }
            }

            ArmorRenderer.renderPart(matrices, vertexConsumers, light, stack, model, LAYER_1);
        }, DetectiveItemsMod.DETECTIVE_HELMET, DetectiveItemsMod.DETECTIVE_CHESTPLATE);
    }

    private BipedEntityModel<LivingEntity> getOuterArmorModel() {
        if (outerArmorModel != null) return outerArmorModel;
        MinecraftClient client = MinecraftClient.getInstance();
        if (client == null || client.getEntityModelLoader() == null) return null;
        ModelPart root = client.getEntityModelLoader().getModelPart(EntityModelLayers.PLAYER_OUTER_ARMOR);
        outerArmorModel = new BipedEntityModel<>(root);
        return outerArmorModel;
    }
}
