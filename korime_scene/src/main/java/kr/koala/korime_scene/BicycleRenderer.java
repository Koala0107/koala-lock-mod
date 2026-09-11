package kr.koala.korime_scene;

import net.minecraft.client.render.OverlayTexture;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.entity.EntityRenderer;
import net.minecraft.client.render.entity.EntityRendererFactory;
import net.minecraft.client.render.item.ItemRenderer;
import net.minecraft.client.render.model.json.ModelTransformationMode;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.item.ItemStack;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.RotationAxis;

public final class BicycleRenderer extends EntityRenderer<BicycleEntity> {
    private static final Identifier TEXTURE = new Identifier("minecraft", "textures/block/white_concrete.png");
    private final ItemRenderer itemRenderer;
    private final ItemStack stack = new ItemStack(BicycleMod.BICYCLE_ITEM);

    public BicycleRenderer(EntityRendererFactory.Context context) {
        super(context);
        this.itemRenderer = context.getItemRenderer();
        this.shadowRadius = 0.85F;
    }

    @Override
    public void render(BicycleEntity entity, float yaw, float tickDelta, MatrixStack matrices,
                       VertexConsumerProvider vertexConsumers, int light) {
        matrices.push();
        matrices.translate(0.0D, 0.72D, 0.0D);
        // The item model is authored left-to-right on its X axis. Rotate it 90°
        // so the bicycle's long axis matches the horse entity's forward axis.
        matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(90.0F - yaw));
        matrices.scale(3.0F, 3.0F, 3.0F);
        itemRenderer.renderItem(stack, ModelTransformationMode.FIXED, light, OverlayTexture.DEFAULT_UV,
                matrices, vertexConsumers, entity.getWorld(), entity.getId());
        matrices.pop();
        super.render(entity, yaw, tickDelta, matrices, vertexConsumers, light);
    }

    @Override
    public Identifier getTexture(BicycleEntity entity) {
        return TEXTURE;
    }
}
