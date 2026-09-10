package kr.koala.korime_scene;

import net.minecraft.client.render.OverlayTexture;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.block.entity.BlockEntityRenderer;
import net.minecraft.client.render.block.entity.BlockEntityRendererFactory;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.math.Direction;
import org.joml.Matrix3f;
import org.joml.Matrix4f;

public final class ImageFrameBlockEntityRenderer implements BlockEntityRenderer<ImageFrameBlockEntity> {
    public ImageFrameBlockEntityRenderer(BlockEntityRendererFactory.Context context) { }

    @Override
    public void render(ImageFrameBlockEntity frame, float tickDelta, MatrixStack matrices,
                       VertexConsumerProvider vertexConsumers, int light, int overlay) {
        ImageFrameTextureCache.TextureInfo texture = ImageFrameTextureCache.getInfo(frame.getImageUrl());
        if (texture == null) return;

        float frameWidth = frame.getFrameWidth();
        float frameHeight = frame.getFrameHeight();
        ImageFrameAlignment alignment = frame.getAlignment();

        float imageAspect = texture.width() / (float) texture.height();
        float frameAspect = frameWidth / frameHeight;
        float drawWidth;
        float drawHeight;
        if (imageAspect >= frameAspect) {
            drawWidth = frameWidth;
            drawHeight = frameWidth / imageAspect;
        } else {
            drawHeight = frameHeight;
            drawWidth = frameHeight * imageAspect;
        }

        float frameLeft = -frameWidth * (1.0F - alignment.getHorizontal());
        float frameBottom = -frameHeight * (1.0F - alignment.getVertical());
        float imageLeft = frameLeft + (frameWidth - drawWidth) * 0.5F;
        float imageBottom = frameBottom + (frameHeight - drawHeight) * 0.5F;

        float uLeft = frame.isFlipHorizontal() ? 1.0F : 0.0F;
        float uRight = frame.isFlipHorizontal() ? 0.0F : 1.0F;
        float rotation = frame.getRotationDegrees();

        VertexConsumer vc = vertexConsumers.getBuffer(RenderLayer.getEntityCutoutNoCull(texture.id()));
        MatrixStack.Entry entry = matrices.peek();
        Matrix4f p = entry.getPositionMatrix();
        Matrix3f n = entry.getNormalMatrix();
        Direction facing = frame.getCachedState().get(ImageFrameBlock.FACING);

        switch (facing) {
            case SOUTH -> drawQuad(vc, p, n,
                    0.5F, 0.5F, 0.01F,
                    1, 0, 0, 0, 1, 0, 0, 0, 1,
                    imageLeft, imageBottom, drawWidth, drawHeight, uLeft, uRight, rotation, light);
            case NORTH -> drawQuad(vc, p, n,
                    0.5F, 0.5F, 0.99F,
                    -1, 0, 0, 0, 1, 0, 0, 0, -1,
                    imageLeft, imageBottom, drawWidth, drawHeight, uLeft, uRight, rotation, light);
            case EAST -> drawQuad(vc, p, n,
                    0.01F, 0.5F, 0.5F,
                    0, 0, -1, 0, 1, 0, 1, 0, 0,
                    imageLeft, imageBottom, drawWidth, drawHeight, uLeft, uRight, rotation, light);
            case WEST -> drawQuad(vc, p, n,
                    0.99F, 0.5F, 0.5F,
                    0, 0, 1, 0, 1, 0, -1, 0, 0,
                    imageLeft, imageBottom, drawWidth, drawHeight, uLeft, uRight, rotation, light);
            case UP -> drawQuad(vc, p, n,
                    0.5F, 0.01F, 0.5F,
                    1, 0, 0, 0, 0, -1, 0, 1, 0,
                    imageLeft, imageBottom, drawWidth, drawHeight, uLeft, uRight, rotation, light);
            case DOWN -> drawQuad(vc, p, n,
                    0.5F, 0.99F, 0.5F,
                    1, 0, 0, 0, 0, 1, 0, -1, 0,
                    imageLeft, imageBottom, drawWidth, drawHeight, uLeft, uRight, rotation, light);
        }
    }

    private static void drawQuad(VertexConsumer vc, Matrix4f p, Matrix3f n,
                                 float cx, float cy, float cz,
                                 float rx, float ry, float rz,
                                 float ux, float uy, float uz,
                                 float nx, float ny, float nz,
                                 float left, float bottom, float width, float height,
                                 float uLeft, float uRight, float rotationDegrees, int light) {
        float right = left + width;
        float top = bottom + height;
        float centerX = left + width * 0.5F;
        float centerY = bottom + height * 0.5F;

        double radians = Math.toRadians(rotationDegrees);
        float cos = (float) Math.cos(radians);
        float sin = (float) Math.sin(radians);

        float[] tl = rotate(left, top, centerX, centerY, cos, sin);
        float[] bl = rotate(left, bottom, centerX, centerY, cos, sin);
        float[] br = rotate(right, bottom, centerX, centerY, cos, sin);
        float[] tr = rotate(right, top, centerX, centerY, cos, sin);

        vertex(vc, p, n,
                cx + rx * tl[0] + ux * tl[1], cy + ry * tl[0] + uy * tl[1], cz + rz * tl[0] + uz * tl[1],
                uLeft, 0, nx, ny, nz, light);
        vertex(vc, p, n,
                cx + rx * bl[0] + ux * bl[1], cy + ry * bl[0] + uy * bl[1], cz + rz * bl[0] + uz * bl[1],
                uLeft, 1, nx, ny, nz, light);
        vertex(vc, p, n,
                cx + rx * br[0] + ux * br[1], cy + ry * br[0] + uy * br[1], cz + rz * br[0] + uz * br[1],
                uRight, 1, nx, ny, nz, light);
        vertex(vc, p, n,
                cx + rx * tr[0] + ux * tr[1], cy + ry * tr[0] + uy * tr[1], cz + rz * tr[0] + uz * tr[1],
                uRight, 0, nx, ny, nz, light);
    }

    private static float[] rotate(float x, float y, float cx, float cy, float cos, float sin) {
        float dx = x - cx;
        float dy = y - cy;
        return new float[] {
                cx + dx * cos - dy * sin,
                cy + dx * sin + dy * cos
        };
    }

    private static void vertex(VertexConsumer vc, Matrix4f p, Matrix3f n,
                               float x, float y, float z, float u, float v,
                               float nx, float ny, float nz, int light) {
        vc.vertex(p, x, y, z).color(255, 255, 255, 255).texture(u, v)
                .overlay(OverlayTexture.DEFAULT_UV).light(light).normal(n, nx, ny, nz).next();
    }

    @Override
    public int getRenderDistance() {
        return 128;
    }
}
