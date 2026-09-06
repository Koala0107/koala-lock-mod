package kr.koala.crouchlock;

import com.mojang.brigadier.Command;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.entity.Entity;
import net.minecraft.entity.decoration.DisplayEntity;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;

import java.util.ArrayList;
import java.util.List;

/** Manual escape hatch for old orphaned display entities left by earlier builds. */
public final class LockMarkerCleanupMod implements ModInitializer {
    private static final String SECONDARY_MARKER_TAG = CrouchLockMod.MOD_ID + ":secondary_marker_v7";
    private static final String SECONDARY_LOCK_PREFIX = CrouchLockMod.MOD_ID + ":secondary_lock:";
    private static final String LAYOUT_PREFIX = CrouchLockMod.MOD_ID + ":marker_layout_";

    @Override
    public void onInitialize() {
        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) ->
                dispatcher.register(CommandManager.literal("crouchlock")
                        .then(CommandManager.literal("cleanup")
                                .executes(context -> cleanup(context.getSource().getWorld(), context.getSource())))));
    }

    private static int cleanup(ServerWorld world, net.minecraft.server.command.ServerCommandSource source) {
        List<Entity> remove = new ArrayList<>();
        for (Entity entity : world.iterateEntities()) {
            if (!(entity instanceof DisplayEntity.ItemDisplayEntity)) continue;
            boolean ours = entity.getCommandTags().contains(CrouchLockMod.MARKER_TAG)
                    || entity.getCommandTags().contains(SECONDARY_MARKER_TAG)
                    || entity.getCommandTags().stream().anyMatch(tag ->
                            tag.startsWith(LAYOUT_PREFIX) || tag.startsWith(SECONDARY_LOCK_PREFIX));
            if (ours) remove.add(entity);
        }
        for (Entity entity : remove) entity.discard();
        int count = remove.size();
        source.sendFeedback(() -> Text.translatable("command.crouchlock.cleanup", count), false);
        return count == 0 ? Command.SINGLE_SUCCESS : count;
    }
}
