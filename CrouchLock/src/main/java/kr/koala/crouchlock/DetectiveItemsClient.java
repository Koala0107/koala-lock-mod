package kr.koala.crouchlock;

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
    private static final Identifier LAYER_1 = new Identifier(CrouchLockMod.MOD_ID, "textures/models/armor/detective_layer_1.png");
    private static final Identifier LAYER_2 = new Identifier(CrouchLockMod.MOD_ID, "textures/models/armor/detective_layer_2.png");

    private BipedEntityModel<LivingEntity> outerArmorModel;
    private BipedEntityModel<LivingEntity> innerArmorModel;

    @Override
    public void onInitializeClient() {
        BlockRenderLayerMap.INSTANCE.putBlock(DetectiveItemsMod.MAGNIFYING_GLASS_BLOCK, RenderLayer.getCutout());

        ArmorRenderer.register((matrices, vertexConsumers, stack, entity, slot, light, contextModel) -> {
            BipedEntityModel<LivingEntity> model = slot == EquipmentSlot.LEGS ? getInnerArmorModel() : getOuterArmorModel();
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
                case LEGS -> {
                    model.body.visible = true;
                    model.rightLeg.visible = true;
                    model.leftLeg.visible = true;
                }
                case FEET -> {
                    model.rightLeg.visible = true;
                    model.leftLeg.visible = true;
                }
                default -> { }
            }

            ArmorRenderer.renderPart(matrices, vertexConsumers, light, stack, model,
                    slot == EquipmentSlot.LEGS ? LAYER_2 : LAYER_1);
        }, DetectiveItemsMod.DETECTIVE_HELMET, DetectiveItemsMod.DETECTIVE_CHESTPLATE,
                DetectiveItemsMod.DETECTIVE_LEGGINGS, DetectiveItemsMod.DETECTIVE_BOOTS);
    }

    private BipedEntityModel<LivingEntity> getOuterArmorModel() {
        if (outerArmorModel != null) return outerArmorModel;
        MinecraftClient client = MinecraftClient.getInstance();
        if (client == null || client.getEntityModelLoader() == null) return null;
        ModelPart root = client.getEntityModelLoader().getModelPart(EntityModelLayers.PLAYER_OUTER_ARMOR);
        outerArmorModel = new BipedEntityModel<>(root);
        return outerArmorModel;
    }

    private BipedEntityModel<LivingEntity> getInnerArmorModel() {
        if (innerArmorModel != null) return innerArmorModel;
        MinecraftClient client = MinecraftClient.getInstance();
        if (client == null || client.getEntityModelLoader() == null) return null;
        ModelPart root = client.getEntityModelLoader().getModelPart(EntityModelLayers.PLAYER_INNER_ARMOR);
        innerArmorModel = new BipedEntityModel<>(root);
        return innerArmorModel;
    }
}
