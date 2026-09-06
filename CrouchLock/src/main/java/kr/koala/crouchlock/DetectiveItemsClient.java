package kr.koala.crouchlock;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.blockrenderlayer.v1.BlockRenderLayerMap;
import net.fabricmc.fabric.api.client.rendering.v1.ArmorRenderer;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.model.Dilation;
import net.minecraft.client.model.ModelData;
import net.minecraft.client.model.ModelPart;
import net.minecraft.client.model.ModelPartBuilder;
import net.minecraft.client.model.ModelPartData;
import net.minecraft.client.model.ModelTransform;
import net.minecraft.client.model.TexturedModelData;
import net.minecraft.client.render.OverlayTexture;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.entity.model.BipedEntityModel;
import net.minecraft.client.render.entity.model.EntityModelLayers;
import net.minecraft.entity.LivingEntity;
import net.minecraft.util.Identifier;

public final class DetectiveItemsClient implements ClientModInitializer {
    private static final Identifier DETECTIVE_ARMOR_TEXTURE =
            new Identifier(CrouchLockMod.MOD_ID, "textures/models/armor/detective_layer_1.png");

    private BipedEntityModel<LivingEntity> hatModel;
    private BipedEntityModel<LivingEntity> coatModel;
    private ModelPart capeModel;

    @Override
    public void onInitializeClient() {
        BlockRenderLayerMap.INSTANCE.putBlock(DetectiveItemsMod.MAGNIFYING_GLASS_BLOCK, RenderLayer.getCutout());

        ArmorRenderer.register((matrices, vertexConsumers, stack, entity, slot, light, contextModel) -> {
            BipedEntityModel<LivingEntity> model = slot == net.minecraft.entity.EquipmentSlot.HEAD
                    ? getHatModel() : getCoatModel();
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

            ArmorRenderer.renderPart(matrices, vertexConsumers, light, stack, model, DETECTIVE_ARMOR_TEXTURE);

            if (slot == net.minecraft.entity.EquipmentSlot.CHEST) {
                ModelPart cape = getCapeModel();
                if (cape != null) {
                    matrices.push();
                    model.body.rotate(matrices);
                    cape.pitch = entity.isInSneakingPose() ? 0.38F : 0.12F;
                    VertexConsumer consumer = vertexConsumers.getBuffer(RenderLayer.getArmorCutoutNoCull(DETECTIVE_ARMOR_TEXTURE));
                    cape.render(matrices, consumer, light, OverlayTexture.DEFAULT_UV);
                    matrices.pop();
                }
            }
        }, DetectiveItemsMod.DETECTIVE_HAT, DetectiveItemsMod.DETECTIVE_ROBE);
    }

    private BipedEntityModel<LivingEntity> getHatModel() {
        if (hatModel != null) return hatModel;
        MinecraftClient client = MinecraftClient.getInstance();
        if (client == null || client.getEntityModelLoader() == null) return null;
        ModelPart root = client.getEntityModelLoader().getModelPart(EntityModelLayers.PLAYER_OUTER_ARMOR);
        hatModel = new BipedEntityModel<>(root);
        return hatModel;
    }

    private BipedEntityModel<LivingEntity> getCoatModel() {
        if (coatModel != null) return coatModel;
        MinecraftClient client = MinecraftClient.getInstance();
        if (client == null || client.getEntityModelLoader() == null) return null;
        ModelPart root = client.getEntityModelLoader().getModelPart(EntityModelLayers.PLAYER_INNER_ARMOR);
        coatModel = new BipedEntityModel<>(root);
        return coatModel;
    }

    private ModelPart getCapeModel() {
        if (capeModel != null) return capeModel;
        ModelData data = new ModelData();
        ModelPartData root = data.getRoot();
        root.addChild("cape",
                ModelPartBuilder.create().uv(0, 32)
                        .cuboid(-5.0F, 0.0F, 0.0F, 10.0F, 16.0F, 1.0F, new Dilation(0.05F)),
                ModelTransform.pivot(0.0F, 0.0F, 2.15F));
        capeModel = TexturedModelData.of(data, 64, 64).createModel().getChild("cape");
        return capeModel;
    }
}
