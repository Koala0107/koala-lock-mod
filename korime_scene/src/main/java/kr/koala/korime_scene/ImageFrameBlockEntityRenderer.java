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

        Direction facing = frame.getCachedState().get(ImageFrameBlock.FACING);
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

        float horizontalOffset = (frameWidth - drawWidth) * alignment.getHorizontal();
        float verticalOffset = (frameHeight - drawHeight) * alignment.getVertical();

        VertexConsumer vc = vertexConsumers.getBuffer(RenderLayer.getEntityCutoutNoCull(texture.id()));
        MatrixStack.Entry entry = matrices.peek();
        Matrix4f position = entry.getPositionMatrix();
        Matrix3f normal = entry.getNormalMatrix();

        switch (facing) {
            case SOUTH -> drawWallZ(vc, position, normal, frameWidth, drawWidth, drawHeight,
                    horizontalOffset, verticalOffset, 0.01F, true, light);
            case NORTH -> drawWallZ(vc, position, normal, frameWidth, drawWidth, drawHeight,
                    horizontalOffset, verticalOffset, 0.99F, false, light);
            case EAST -> drawWallX(vc, position, normal, frameWidth, drawWidth, drawHeight,
                    horizontalOffset, verticalOffset, 0.01F, true, light);
            case WEST -> drawWallX(vc, position, normal, frameWidth, drawWidth, drawHeight,
                    horizontalOffset, verticalOffset, 0.99F, false, light);
            case UP -> drawFloor(vc, position, normal, frameWidth, frameHeight, drawWidth, drawHeight,
                    horizontalOffset, verticalOffset, 0.01F, true, light);
            case DOWN -> drawFloor(vc, position, normal, frameWidth, frameHeight, drawWidth, drawHeight,
                    horizontalOffset, verticalOffset, 0.99F, false, light);
        }
    }

    private static void drawWallZ(VertexConsumer vc, Matrix4f p, Matrix3f n,
                                  float frameWidth, float drawWidth, float drawHeight,
                                  float horizontalOffset, float verticalOffset,
                                  float z, boolean south, int light) {
        float frameX0 = 0.5F - frameWidth * 0.5F;
        float x0 = frameX0 + horizontalOffset;
        float x1 = x0 + drawWidth;
        float y0 = verticalOffset;
        float y1 = y0 + drawHeight;
        float nz = south ? 1F : -1F;
        if (south) {
            vertex(vc, p, n, x0, y1, z, 0, 0, 0, 0, nz, light);
            vertex(vc, p, n, x0, y0, z, 0, 1, 0, 0, nz, light);
            vertex(vc, p, n, x1, y0, z, 1, 1, 0, 0, nz, light);
            vertex(vc, p, n, x1, y1, z, 1, 0, 0, 0, nz, light);
        } else {
            vertex(vc, p, n, x1, y1, z, 0, 0, 0, 0, nz, light);
            vertex(vc, p, n, x1, y0, z, 0, 1, 0, 0, nz, light);
            vertex(vc, p, n, x0, y0, z, 1, 1, 0, 0, nz, light);
            vertex(vc, p, n, x0, y1, z, 1, 0, 0, 0, nz, light);
        }
    }

    private static void drawWallX(VertexConsumer vc, Matrix4f p, Matrix3f n,
                                  float frameWidth, float drawWidth, float drawHeight,
                                  float horizontalOffset, float verticalOffset,
                                  float x, boolean east, int light) {
        float frameZ0 = 0.5F - frameWidth * 0.5F;
        float z0 = frameZ0 + horizontalOffset;
        float z1 = z0 + drawWidth;
        float y0 = verticalOffset;
        float y1 = y0 + drawHeight;
        float nx = east ? 1F : -1F;
        if (east) {
            vertex(vc, p, n, x, y1, z1, 0, 0, nx, 0, 0, light);
            vertex(vc, p, n, x, y0, z1, 0, 1, nx, 0, 0, light);
            vertex(vc, p, n, x, y0, z0, 1, 1, nx, 0, 0, light);
            vertex(vc, p, n, x, y1, z0, 1, 0, nx, 0, 0, light);
        } else {
            vertex(vc, p, n, x, y1, z0, 0, 0, nx, 0, 0, light);
            vertex(vc, p, n, x, y0, z0, 0, 1, nx, 0, 0, light);
            vertex(vc, p, n, x, y0, z1, 1, 1, nx, 0, 0, light);
            vertex(vc, p, n, x, y1, z1, 1, 0, nx, 0, 0, light);
        }
    }

    private static void drawFloor(VertexConsumer vc, Matrix4f p, Matrix3f n,
                                  float frameWidth, float frameDepth,
                                  float drawWidth, float drawDepth,
                                  float horizontalOffset, float verticalOffset,
                                  float y, boolean up, int light) {
        float frameX0 = 0.5F - frameWidth * 0.5F;
        float frameZ0 = 0.5F - frameDepth * 0.5F;
        float x0 = frameX0 + horizontalOffset;
        float x1 = x0 + drawWidth;
        float z0 = frameZ0 + verticalOffset;
        float z1 = z0 + drawDepth;
        float ny = up ? 1F : -1F;
        if (up) {
            vertex(vc, p, n, x0, y, z0, 0, 0, 0, ny, 0, light);
            vertex(vc, p, n, x0, y, z1, 0, 1, 0, ny, 0, light);
            vertex(vc, p, n, x1, y, z1, 1, 1, 0, ny, 0, light);
            vertex(vc, p, n, x1, y, z0, 1, 0, 0, ny, 0, light);
        } else {
            vertex(vc, p, n, x0, y, z1, 0, 0, 0, ny, 0, light);
            vertex(vc, p, n, x0, y, z0, 0, 1, 0, ny, 0, light);
            vertex(vc, p, n, x1, y, z0, 1, 1, 0, ny, 0, light);
            vertex(vc, p, n, x1, y, z1, 1, 0, 0, ny, 0, light);
        }
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
