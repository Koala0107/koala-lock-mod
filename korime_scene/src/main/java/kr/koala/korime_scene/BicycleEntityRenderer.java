package kr.koala.korime_scene;

import net.minecraft.client.render.OverlayTexture;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.entity.EntityRenderer;
import net.minecraft.client.render.entity.EntityRendererFactory;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.RotationAxis;
import org.joml.Matrix3f;
import org.joml.Matrix4f;

/** Lightweight built-in renderer; no external model library is required. */
public final class BicycleEntityRenderer extends EntityRenderer<BicycleEntity> {
    private static final Identifier TEXTURE = new Identifier("minecraft", "textures/block/white_concrete.png");

    public BicycleEntityRenderer(EntityRendererFactory.Context context) {
        super(context);
        this.shadowRadius = 0.8F;
    }

    @Override
    public void render(BicycleEntity bicycle, float yaw, float tickDelta, MatrixStack matrices,
                       VertexConsumerProvider providers, int light) {
        matrices.push();
        float renderYaw = MathHelper.lerpAngleDegrees(tickDelta, bicycle.prevYaw, bicycle.getYaw());
        matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(180.0F - renderYaw));
        matrices.translate(0.0, 0.08, 0.0);

        VertexConsumer vc = providers.getBuffer(RenderLayer.getEntityCutoutNoCull(TEXTURE));

        // Wheels: dark twelve-segment rings with small hubs.
        drawWheel(matrices, vc, -0.67F, 0.48F, light);
        drawWheel(matrices, vc,  0.67F, 0.48F, light);

        // Main white frame.
        drawBarYZ(matrices, vc, 0.49F, -0.67F, 0.57F, 0.02F, 0.055F, 238, 238, 238, light);
        drawBarYZ(matrices, vc, 0.57F, 0.02F, 0.95F, -0.26F, 0.055F, 238, 238, 238, light);
        drawBarYZ(matrices, vc, 0.95F, -0.26F, 0.49F, -0.67F, 0.055F, 238, 238, 238, light);
        drawBarYZ(matrices, vc, 0.95F, -0.26F, 0.92F, 0.47F, 0.055F, 238, 238, 238, light);
        drawBarYZ(matrices, vc, 0.92F, 0.47F, 0.48F, 0.67F, 0.055F, 238, 238, 238, light);

        // Fork and steering column.
        drawBarYZ(matrices, vc, 0.48F, 0.67F, 1.13F, 0.52F, 0.048F, 215, 215, 215, light);
        drawBarYZ(matrices, vc, 0.92F, 0.47F, 1.13F, 0.52F, 0.048F, 215, 215, 215, light);

        // Seat post and saddle.
        drawBarYZ(matrices, vc, 0.57F, 0.02F, 1.07F, -0.25F, 0.042F, 180, 180, 180, light);
        matrices.push();
        matrices.translate(0.0, 1.08, -0.29);
        drawBox(matrices, vc, -0.22F, -0.035F, -0.11F, 0.22F, 0.035F, 0.11F, 35, 35, 35, light);
        matrices.pop();

        // Handlebar.
        matrices.push();
        matrices.translate(0.0, 1.15, 0.52);
        drawBox(matrices, vc, -0.34F, -0.035F, -0.035F, 0.34F, 0.035F, 0.035F, 35, 35, 35, light);
        drawBox(matrices, vc, -0.36F, -0.08F, -0.05F, -0.30F, 0.06F, 0.05F, 25, 25, 25, light);
        drawBox(matrices, vc, 0.30F, -0.08F, -0.05F, 0.36F, 0.06F, 0.05F, 25, 25, 25, light);
        matrices.pop();

        // Pedal axle.
        matrices.push();
        matrices.translate(0.0, 0.57, 0.02);
        drawBox(matrices, vc, -0.16F, -0.025F, -0.025F, 0.16F, 0.025F, 0.025F, 55, 55, 55, light);
        matrices.pop();

        matrices.pop();
        super.render(bicycle, yaw, tickDelta, matrices, providers, light);
    }

    private static void drawWheel(MatrixStack matrices, VertexConsumer vc, float z, float y, int light) {
        final int segments = 12;
        final float radius = 0.41F;
        final float segmentLength = 0.22F;
        final float thickness = 0.045F;
        for (int i = 0; i < segments; i++) {
            float angle = i * (360.0F / segments);
            matrices.push();
            matrices.translate(0.0, y, z);
            matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(angle));
            matrices.translate(0.0, radius, 0.0);
            drawBox(matrices, vc,
                    -thickness, -thickness, -segmentLength / 2.0F,
                     thickness,  thickness,  segmentLength / 2.0F,
                    25, 25, 25, light);
            matrices.pop();
        }
        matrices.push();
        matrices.translate(0.0, y, z);
        drawBox(matrices, vc, -0.09F, -0.09F, -0.055F, 0.09F, 0.09F, 0.055F, 90, 90, 90, light);
        matrices.pop();
    }

    private static void drawBarYZ(MatrixStack matrices, VertexConsumer vc,
                                  float y1, float z1, float y2, float z2, float thickness,
                                  int red, int green, int blue, int light) {
        float dy = y2 - y1;
        float dz = z2 - z1;
        float length = MathHelper.sqrt(dy * dy + dz * dz);
        if (length <= 0.0001F) return;
        float angle = (float) -Math.toDegrees(Math.atan2(dy, dz));

        matrices.push();
        matrices.translate(0.0, (y1 + y2) * 0.5F, (z1 + z2) * 0.5F);
        matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(angle));
        drawBox(matrices, vc, -thickness, -thickness, -length * 0.5F,
                thickness, thickness, length * 0.5F, red, green, blue, light);
        matrices.pop();
    }

    private static void drawBox(MatrixStack matrices, VertexConsumer vc,
                                float x1, float y1, float z1, float x2, float y2, float z2,
                                int red, int green, int blue, int light) {
        MatrixStack.Entry entry = matrices.peek();
        Matrix4f p = entry.getPositionMatrix();
        Matrix3f n = entry.getNormalMatrix();

        // south / north
        quad(vc, p, n, x1,y1,z2, x2,y1,z2, x2,y2,z2, x1,y2,z2, 0,0,1, red,green,blue,light);
        quad(vc, p, n, x2,y1,z1, x1,y1,z1, x1,y2,z1, x2,y2,z1, 0,0,-1, red,green,blue,light);
        // east / west
        quad(vc, p, n, x2,y1,z2, x2,y1,z1, x2,y2,z1, x2,y2,z2, 1,0,0, red,green,blue,light);
        quad(vc, p, n, x1,y1,z1, x1,y1,z2, x1,y2,z2, x1,y2,z1, -1,0,0, red,green,blue,light);
        // top / bottom
        quad(vc, p, n, x1,y2,z2, x2,y2,z2, x2,y2,z1, x1,y2,z1, 0,1,0, red,green,blue,light);
        quad(vc, p, n, x1,y1,z1, x2,y1,z1, x2,y1,z2, x1,y1,z2, 0,-1,0, red,green,blue,light);
    }

    private static void quad(VertexConsumer vc, Matrix4f p, Matrix3f n,
                             float ax,float ay,float az, float bx,float by,float bz,
                             float cx,float cy,float cz, float dx,float dy,float dz,
                             float nx,float ny,float nz, int red,int green,int blue,int light) {
        vertex(vc,p,n,ax,ay,az,0,1,nx,ny,nz,red,green,blue,light);
        vertex(vc,p,n,bx,by,bz,1,1,nx,ny,nz,red,green,blue,light);
        vertex(vc,p,n,cx,cy,cz,1,0,nx,ny,nz,red,green,blue,light);
        vertex(vc,p,n,dx,dy,dz,0,0,nx,ny,nz,red,green,blue,light);
    }

    private static void vertex(VertexConsumer vc, Matrix4f p, Matrix3f n,
                               float x,float y,float z,float u,float v,
                               float nx,float ny,float nz,int red,int green,int blue,int light) {
        vc.vertex(p,x,y,z).color(red,green,blue,255).texture(u,v)
                .overlay(OverlayTexture.DEFAULT_UV).light(light).normal(n,nx,ny,nz).next();
    }

    @Override
    public Identifier getTexture(BicycleEntity entity) {
        return TEXTURE;
    }
}
