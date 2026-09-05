package kr.koala.crouchlock;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.block.BlockState;
import net.minecraft.block.DoorBlock;
import net.minecraft.block.TrapdoorBlock;
import net.minecraft.entity.Entity;
import net.minecraft.entity.decoration.DisplayEntity;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.state.property.Properties;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/** Keeps lock item displays attached to the block instead of facing the player. */
public final class LockMarkerLayoutFix implements ModInitializer {
    private static final String LAYOUT_TAG = CrouchLockMod.MOD_ID + ":marker_layout_v3";

    @Override
    public void onInitialize() {
        ServerTickEvents.END_WORLD_TICK.register(world -> {
            if (world.getTime() % 20 == 0) {
                syncMarkerLayout(world);
            }
        });
    }

    private static void syncMarkerLayout(ServerWorld world) {
        Map<UUID, BlockPos> targets = new HashMap<>();
        for (var entry : LockState.get(world).entries()) {
            BlockPos pos = BlockPos.fromLong(entry.getKey());
            if (world.isChunkLoaded(pos)) {
                targets.putIfAbsent(entry.getValue().lockId(), pos);
            }
        }

        for (Entity entity : world.iterateEntities()) {
            if (!(entity instanceof DisplayEntity.ItemDisplayEntity marker)
                    || !entity.getCommandTags().contains(CrouchLockMod.MARKER_TAG)) {
                continue;
            }

            Optional<UUID> lockId = markerLockId(entity);
            BlockPos target = lockId.map(targets::get).orElse(null);
            if (target == null) {
                continue;
            }

            MarkerPose pose = markerPose(world, target);
            NbtCompound settings = new NbtCompound();
            settings.putString("billboard", "fixed");
            marker.readNbt(settings);
            marker.refreshPositionAndAngles(
                    pose.position().x,
                    pose.position().y,
                    pose.position().z,
                    pose.yaw(),
                    pose.pitch()
            );
            marker.addCommandTag(LAYOUT_TAG);
        }
    }

    private static MarkerPose markerPose(ServerWorld world, BlockPos originalPos) {
        BlockPos pos = lowerDoorHalf(world, originalPos);
        BlockState state = world.getBlockState(pos);

        if (state.getBlock() instanceof TrapdoorBlock) {
            boolean topHalf = state.contains(Properties.BLOCK_HALF)
                    && "top".equals(state.get(Properties.BLOCK_HALF).asString());
            // Keep the display on the closed panel center and horizontal, so it reads naturally from above.
            double surfaceY = topHalf ? 1.015 : 0.2025;
            return new MarkerPose(
                    new Vec3d(pos.getX() + 0.5, pos.getY() + surfaceY, pos.getZ() + 0.5),
                    0.0F,
                    90.0F
            );
        }

        if (state.getBlock() instanceof DoorBlock) {
            BlockState closedState = state.contains(Properties.OPEN)
                    ? state.with(Properties.OPEN, false)
                    : state;
            Vec3d doorFace = markerNearOutline(world, pos, closedState, 0.76, 0.025);
            Direction facing = state.get(Properties.HORIZONTAL_FACING);
            boolean leftHinge = state.contains(Properties.DOOR_HINGE)
                    && "left".equals(state.get(Properties.DOOR_HINGE).asString());
            Direction handleSide = leftHinge
                    ? facing.rotateYCounterclockwise()
                    : facing.rotateYClockwise();
            Vec3d handle = doorFace.add(
                    handleSide.getOffsetX() * 0.29,
                    0.0,
                    handleSide.getOffsetZ() * 0.29
            );
            return new MarkerPose(handle, horizontalYaw(facing), 0.0F);
        }

        Direction facing = blockFacing(state);
        Vec3d center = Vec3d.ofCenter(pos).add(
                facing.getOffsetX() * 0.515,
                facing.getOffsetY() * 0.515,
                facing.getOffsetZ() * 0.515
        );
        return new MarkerPose(center, yawFor(facing), pitchFor(facing));
    }

    private static Direction blockFacing(BlockState state) {
        if (state.contains(Properties.FACING)) {
            return state.get(Properties.FACING);
        }
        if (state.contains(Properties.HORIZONTAL_FACING)) {
            return state.get(Properties.HORIZONTAL_FACING);
        }
        return Direction.SOUTH;
    }

    private static float yawFor(Direction facing) {
        return switch (facing) {
            case NORTH -> 180.0F;
            case EAST -> -90.0F;
            case WEST -> 90.0F;
            default -> 0.0F;
        };
    }

    private static float horizontalYaw(Direction facing) {
        return yawFor(facing.getAxis().isHorizontal() ? facing : Direction.SOUTH);
    }

    private static float pitchFor(Direction facing) {
        return switch (facing) {
            case UP -> -90.0F;
            case DOWN -> 90.0F;
            default -> 0.0F;
        };
    }

    private static Vec3d markerNearOutline(ServerWorld world, BlockPos pos, BlockState state,
                                           double height, double outwardOffset) {
        Box bounds = state.getOutlineShape(world, pos).getBoundingBox();
        double localX = (bounds.minX + bounds.maxX) * 0.5;
        double localZ = (bounds.minZ + bounds.maxZ) * 0.5;
        double fromCenterX = localX - 0.5;
        double fromCenterZ = localZ - 0.5;
        double horizontalLength = Math.sqrt(fromCenterX * fromCenterX + fromCenterZ * fromCenterZ);
        if (horizontalLength > 0.001) {
            localX += fromCenterX / horizontalLength * outwardOffset;
            localZ += fromCenterZ / horizontalLength * outwardOffset;
        }
        return new Vec3d(pos.getX() + localX, pos.getY() + height, pos.getZ() + localZ);
    }

    private static BlockPos lowerDoorHalf(ServerWorld world, BlockPos pos) {
        BlockState state = world.getBlockState(pos);
        if (state.getBlock() instanceof DoorBlock
                && state.contains(Properties.DOUBLE_BLOCK_HALF)
                && "upper".equals(state.get(Properties.DOUBLE_BLOCK_HALF).asString())) {
            BlockPos lower = pos.down();
            if (world.getBlockState(lower).isOf(state.getBlock())) {
                return lower;
            }
        }
        return pos;
    }

    private static Optional<UUID> markerLockId(Entity marker) {
        String prefix = CrouchLockMod.MOD_ID + ":";
        for (String tag : marker.getCommandTags()) {
            if (!tag.startsWith(prefix)
                    || CrouchLockMod.MARKER_TAG.equals(tag)
                    || tag.startsWith(CrouchLockMod.MOD_ID + ":marker_layout_")) {
                continue;
            }
            try {
                return Optional.of(UUID.fromString(tag.substring(prefix.length())));
            } catch (IllegalArgumentException ignored) {
                // Ignore unrelated command tags.
            }
        }
        return Optional.empty();
    }

    private record MarkerPose(Vec3d position, float yaw, float pitch) {
    }
}
