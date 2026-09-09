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

        // Alignment is the anchor point of the entire configured image area around the
        // one placed block. This makes every direction visibly move the image even when
        // the image itself perfectly fills the configured width/height ratio.
        float frameLeft = -frameWidth * alignment.getHorizontal();
        float frameBottom = -frameHeight * alignment.getVertical();
        float imageLeft = frameLeft + (frameWidth - drawWidth) * 0.5F;
        float imageBottom = frameBottom + (frameHeight - drawHeight) * 0.5F;

        VertexConsumer vc = vertexConsumers.getBuffer(RenderLayer.getEntityCutoutNoCull(texture.id()));
        MatrixStack.Entry entry = matrices.peek();
        Matrix4f p = entry.getPositionMatrix();
        Matrix3f n = entry.getNormalMatrix();
        Direction facing = frame.getCachedState().get(ImageFrameBlock.FACING);

        switch (facing) {
            case SOUTH -> drawQuad(vc, p, n,
                    0.5F, 0.5F, 0.01F,
                    1, 0, 0, 0, 1, 0, 0, 0, 1,
                    imageLeft, imageBottom, drawWidth, drawHeight, light);
            case NORTH -> drawQuad(vc, p, n,
                    0.5F, 0.5F, 0.99F,
                    -1, 0, 0, 0, 1, 0, 0, 0, -1,
                    imageLeft, imageBottom, drawWidth, drawHeight, light);
            case EAST -> drawQuad(vc, p, n,
                    0.01F, 0.5F, 0.5F,
                    0, 0, -1, 0, 1, 0, 1, 0, 0,
                    imageLeft, imageBottom, drawWidth, drawHeight, light);
            case WEST -> drawQuad(vc, p, n,
                    0.99F, 0.5F, 0.5F,
                    0, 0, 1, 0, 1, 0, -1, 0, 0,
                    imageLeft, imageBottom, drawWidth, drawHeight, light);
            case UP -> drawQuad(vc, p, n,
                    0.5F, 0.01F, 0.5F,
                    1, 0, 0, 0, 0, -1, 0, 1, 0,
                    imageLeft, imageBottom, drawWidth, drawHeight, light);
            case DOWN -> drawQuad(vc, p, n,
                    0.5F, 0.99F, 0.5F,
                    1, 0, 0, 0, 0, 1, 0, -1, 0,
                    imageLeft, imageBottom, drawWidth, drawHeight, light);
        }
    }

    private static void drawQuad(VertexConsumer vc, Matrix4f p, Matrix3f n,
                                 float cx, float cy, float cz,
                                 float rx, float ry, float rz,
                                 float ux, float uy, float uz,
                                 float nx, float ny, float nz,
                                 float left, float bottom, float width, float height,
                                 int light) {
        float right = left + width;
        float top = bottom + height;

        float tlx = cx + rx * left + ux * top;
        float tly = cy + ry * left + uy * top;
        float tlz = cz + rz * left + uz * top;
        float blx = cx + rx * left + ux * bottom;
        float bly = cy + ry * left + uy * bottom;
        float blz = cz + rz * left + uz * bottom;
        float brx = cx + rx * right + ux * bottom;
        float bry = cy + ry * right + uy * bottom;
        float brz = cz + rz * right + uz * bottom;
        float trx = cx + rx * right + ux * top;
        float try_ = cy + ry * right + uy * top;
        float trz = cz + rz * right + uz * top;

        vertex(vc, p, n, tlx, tly, tlz, 0, 0, nx, ny, nz, light);
        vertex(vc, p, n, blx, bly, blz, 0, 1, nx, ny, nz, light);
        vertex(vc, p, n, brx, bry, brz, 1, 1, nx, ny, nz, light);
        vertex(vc, p, n, trx, try_, trz, 1, 0, nx, ny, nz, light);
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
