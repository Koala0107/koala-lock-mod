package kr.koala.korime_scene;

import net.minecraft.block.HorizontalFacingBlock;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.block.entity.BlockEntityRenderer;
import net.minecraft.client.render.block.entity.BlockEntityRendererFactory;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.client.render.OverlayTexture;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.RotationAxis;
import org.joml.Matrix3f;
import org.joml.Matrix4f;

public final class ImageFrameBlockEntityRenderer implements BlockEntityRenderer<ImageFrameBlockEntity> {
    public ImageFrameBlockEntityRenderer(BlockEntityRendererFactory.Context context) { }

    @Override
    public void render(ImageFrameBlockEntity frame, float tickDelta, MatrixStack matrices,
                       VertexConsumerProvider vertexConsumers, int light, int overlay) {
        Identifier texture = ImageFrameTextureCache.get(frame.getImageUrl());
        if (texture == null) return;

        Direction facing = frame.getCachedState().get(HorizontalFacingBlock.FACING);
        float width = frame.getFrameWidth();
        float height = frame.getFrameHeight();

        matrices.push();
        matrices.translate(0.5, 0.5, 0.5);
        float yaw = switch (facing) {
            case NORTH -> 180.0F;
            case EAST -> -90.0F;
            case WEST -> 90.0F;
            default -> 0.0F;
        };
        matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(yaw));
        matrices.translate(0.0, (height - 1.0F) * 0.5F, 0.505);

        float x0 = -width * 0.5F;
        float x1 = width * 0.5F;
        float y0 = -height * 0.5F;
        float y1 = height * 0.5F;

        VertexConsumer vc = vertexConsumers.getBuffer(RenderLayer.getEntityCutoutNoCull(texture));
        MatrixStack.Entry entry = matrices.peek();
        Matrix4f position = entry.getPositionMatrix();
        Matrix3f normal = entry.getNormalMatrix();

        vc.vertex(position, x0, y1, 0).color(255, 255, 255, 255).texture(0, 0)
                .overlay(OverlayTexture.DEFAULT_UV).light(light).normal(normal, 0, 0, 1).next();
        vc.vertex(position, x0, y0, 0).color(255, 255, 255, 255).texture(0, 1)
                .overlay(OverlayTexture.DEFAULT_UV).light(light).normal(normal, 0, 0, 1).next();
        vc.vertex(position, x1, y0, 0).color(255, 255, 255, 255).texture(1, 1)
                .overlay(OverlayTexture.DEFAULT_UV).light(light).normal(normal, 0, 0, 1).next();
        vc.vertex(position, x1, y1, 0).color(255, 255, 255, 255).texture(1, 0)
                .overlay(OverlayTexture.DEFAULT_UV).light(light).normal(normal, 0, 0, 1).next();

        matrices.pop();
    }

    @Override
    public int getRenderDistance() {
        return 128;
    }
}
