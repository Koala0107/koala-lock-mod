package kr.koala.korime_scene;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.entity.FabricEntityTypeBuilder;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.fabricmc.fabric.api.object.builder.v1.entity.FabricDefaultAttributeRegistry;
import net.minecraft.entity.EntityDimensions;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.SpawnGroup;
import net.minecraft.item.Item;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.util.Identifier;

public final class BicycleMod implements ModInitializer {
    public static final Identifier JUMP_PACKET = new Identifier(KorimeSceneMod.MOD_ID, "bicycle_jump");

    public static final EntityType<BicycleEntity> BICYCLE_ENTITY = Registry.register(
            Registries.ENTITY_TYPE,
            new Identifier(KorimeSceneMod.MOD_ID, "bicycle"),
            FabricEntityTypeBuilder.create(SpawnGroup.MISC, BicycleEntity::new)
                    .dimensions(EntityDimensions.fixed(0.8F, 1.15F))
                    .trackRangeChunks(10)
                    .trackedUpdateRate(1)
                    .build()
    );

    public static final Item BICYCLE_ITEM = Registry.register(
            Registries.ITEM,
            new Identifier(KorimeSceneMod.MOD_ID, "bicycle"),
            new BicycleItem(new Item.Settings().maxCount(1))
    );

    @Override
    public void onInitialize() {
        FabricDefaultAttributeRegistry.register(BICYCLE_ENTITY, BicycleEntity.createBicycleAttributes());

        ServerPlayNetworking.registerGlobalReceiver(JUMP_PACKET,
                (server, player, handler, buf, responseSender) -> server.execute(() -> {
                    if (player.getVehicle() instanceof BicycleEntity bicycle && bicycle.getControllingPassenger() == player) {
                        bicycle.jumpBicycle();
                    }
                }));
    }
}
