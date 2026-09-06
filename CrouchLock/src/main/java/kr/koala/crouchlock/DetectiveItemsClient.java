package kr.koala.crouchlock;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.blockrenderlayer.v1.BlockRenderLayerMap;
import net.fabricmc.fabric.api.client.rendering.v1.ArmorRenderer;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.model.ModelPart;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.entity.model.BipedEntityModel;
import net.minecraft.client.render.entity.model.EntityModelLayers;
import net.minecraft.entity.LivingEntity;
import net.minecraft.util.Identifier;

public final class DetectiveItemsClient implements ClientModInitializer {
    private static final Identifier DETECTIVE_ARMOR_TEXTURE =
            new Identifier(CrouchLockMod.MOD_ID, "textures/models/armor/detective_layer_1.png");

    private BipedEntityModel<LivingEntity> armorModel;

    @Override
    public void onInitializeClient() {
        BlockRenderLayerMap.INSTANCE.putBlock(DetectiveItemsMod.MAGNIFYING_GLASS_BLOCK, RenderLayer.getCutout());

        ArmorRenderer.register((matrices, vertexConsumers, stack, entity, slot, light, contextModel) -> {
            BipedEntityModel<LivingEntity> model = getArmorModel();
            if (model == null) return;

            contextModel.copyBipedStateTo(model);
            model.setVisible(false);
            switch (slot) {
                case HEAD -> model.head.visible = model.hat.visible = true;
                case CHEST -> {
                    model.body.visible = true;
                    model.rightArm.visible = true;
                    model.leftArm.visible = true;
                }
                default -> { }
            }
            ArmorRenderer.renderPart(matrices, vertexConsumers, light, stack, model, DETECTIVE_ARMOR_TEXTURE);
        }, DetectiveItemsMod.DETECTIVE_HAT, DetectiveItemsMod.DETECTIVE_ROBE);
    }

    private BipedEntityModel<LivingEntity> getArmorModel() {
        if (armorModel != null) return armorModel;
        MinecraftClient client = MinecraftClient.getInstance();
        if (client == null || client.getEntityModelLoader() == null) return null;
        ModelPart root = client.getEntityModelLoader().getModelPart(EntityModelLayers.PLAYER_OUTER_ARMOR);
        armorModel = new BipedEntityModel<>(root);
        return armorModel;
    }
}
