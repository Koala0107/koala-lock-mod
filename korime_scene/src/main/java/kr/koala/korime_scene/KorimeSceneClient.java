package kr.koala.korime_scene;

import net.fabricmc.api.ClientModInitializer;
import net.minecraft.client.gui.screen.ingame.HandledScreens;

public final class KorimeSceneClient implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        HandledScreens.register(KorimeSceneMod.KEYPAD_SCREEN_HANDLER, KeypadScreen::new);
    }
}
