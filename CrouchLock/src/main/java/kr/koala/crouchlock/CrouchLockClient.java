package kr.koala.crouchlock;

import net.fabricmc.api.ClientModInitializer;
import net.minecraft.client.gui.screen.ingame.HandledScreens;

public final class CrouchLockClient implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        HandledScreens.register(CrouchLockMod.KEYPAD_SCREEN_HANDLER, KeypadScreen::new);
    }
}
