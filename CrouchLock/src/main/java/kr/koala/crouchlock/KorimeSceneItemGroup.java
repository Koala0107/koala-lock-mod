package kr.koala.crouchlock;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.itemgroup.v1.FabricItemGroup;
import net.minecraft.item.ItemGroup;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

/** Dedicated creative-mode tab for every Korime Scene item. */
public final class KorimeSceneItemGroup implements ModInitializer {
    public static final ItemGroup GROUP = Registry.register(
            Registries.ITEM_GROUP,
            new Identifier(CrouchLockMod.MOD_ID, "korime_scene"),
            FabricItemGroup.builder()
                    .displayName(Text.translatable("itemGroup.crouchlock.korime_scene"))
                    .icon(() -> new ItemStack(SmartphoneMod.SMARTPHONE))
                    .entries((context, entries) -> {
                        entries.add(CrouchLockMod.LOCK_KEY);
                        entries.add(CrouchLockMod.KEYPAD);
                        entries.add(SmartphoneMod.SMARTPHONE);
                        for (var item : EvidenceMarkerMod.ITEMS) {
                            entries.add(item);
                        }
                    })
                    .build()
    );

    @Override
    public void onInitialize() {
        // Static registration above creates the tab during mod initialization.
    }
}
