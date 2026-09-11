package kr.koala.korime_scene;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.itemgroup.v1.ItemGroupEvents;
import net.fabricmc.fabric.api.object.builder.v1.entity.FabricEntityTypeBuilder;
import net.minecraft.entity.EntityDimensions;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.SpawnGroup;
import net.minecraft.item.Item;
import net.minecraft.item.ItemGroups;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.util.Identifier;

public final class BicycleMod implements ModInitializer {
    public static final EntityType<BicycleEntity> BICYCLE = Registry.register(
            Registries.ENTITY_TYPE,
            new Identifier("korime_scene", "bicycle"),
            FabricEntityTypeBuilder.create(SpawnGroup.MISC, BicycleEntity::new)
                    .dimensions(EntityDimensions.fixed(2.15F, 1.45F))
                    .trackRangeBlocks(12)
                    .trackedUpdateRate(1)
                    .build()
    );

    public static final Item BICYCLE_ITEM = Registry.register(
            Registries.ITEM,
            new Identifier("korime_scene", "bicycle"),
            new BicycleItem(new Item.Settings().maxCount(1))
    );

    @Override
    public void onInitialize() {
        ItemGroupEvents.modifyEntriesEvent(ItemGroups.TOOLS)
                .register(entries -> entries.add(BICYCLE_ITEM));
    }
}
