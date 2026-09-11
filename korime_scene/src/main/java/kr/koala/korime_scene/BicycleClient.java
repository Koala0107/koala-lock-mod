package kr.koala.korime_scene;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.client.rendering.v1.EntityRendererRegistry;
import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;

public final class BicycleClient implements ClientModInitializer {
    private boolean jumpWasDown;

    @Override
    public void onInitializeClient() {
        EntityRendererRegistry.register(BicycleMod.BICYCLE_ENTITY, BicycleEntityRenderer::new);

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            if (client.player == null) {
                jumpWasDown = false;
                return;
            }

            boolean jumpDown = client.options.jumpKey.isPressed();
            if (jumpDown && !jumpWasDown && client.player.getVehicle() instanceof BicycleEntity) {
                ClientPlayNetworking.send(BicycleMod.JUMP_PACKET, PacketByteBufs.empty());
            }
            jumpWasDown = jumpDown;
        });
    }
}
