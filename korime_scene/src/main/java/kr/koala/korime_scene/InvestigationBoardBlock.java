package kr.koala.korime_scene;

import com.mojang.serialization.MapCodec;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.HorizontalFacingBlock;
import net.minecraft.block.ShapeContext;
import net.minecraft.entity.LivingEntity;
import net.minecraft.item.ItemPlacementContext;
import net.minecraft.item.ItemStack;
import net.minecraft.state.StateManager;
import net.minecraft.state.property.IntProperty;
import net.minecraft.util.BlockMirror;
import net.minecraft.util.BlockRotation;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.world.BlockView;
import net.minecraft.world.World;
import net.minecraft.world.WorldAccess;
import net.minecraft.world.WorldView;

public final class InvestigationBoardBlock extends HorizontalFacingBlock {
    public static final MapCodec<InvestigationBoardBlock> CODEC = createCodec(InvestigationBoardBlock::new);
    public static final IntProperty PART = IntProperty.of("part", 0, 3);

    private static final VoxelShape NORTH_SHAPE = Block.createCuboidShape(0, 0, 15, 16, 16, 16);
    private static final VoxelShape SOUTH_SHAPE = Block.createCuboidShape(0, 0, 0, 16, 16, 1);
    private static final VoxelShape WEST_SHAPE = Block.createCuboidShape(15, 0, 0, 16, 16, 16);
    private static final VoxelShape EAST_SHAPE = Block.createCuboidShape(0, 0, 0, 1, 16, 16);

    public InvestigationBoardBlock(Settings settings) {
        super(settings);
        setDefaultState(getStateManager().getDefaultState().with(FACING, Direction.NORTH).with(PART, 0));
    }

    @Override
    protected MapCodec<? extends HorizontalFacingBlock> getCodec() {
        return CODEC;
    }

    @Override
    public BlockState getPlacementState(ItemPlacementContext context) {
        Direction facing = context.getSide();
        if (!facing.getAxis().isHorizontal()) return null;

        BlockPos anchor = context.getBlockPos();
        Direction right = facing.rotateYCounterclockwise();
        BlockPos[] positions = new BlockPos[] {
                anchor,
                anchor.offset(right),
                anchor.up(),
                anchor.up().offset(right)
        };

        for (int i = 0; i < positions.length; i++) {
            BlockPos partPos = positions[i];
            if (i > 0 && !context.getWorld().getBlockState(partPos).isAir()) return null;

            BlockPos supportPos = partPos.offset(facing.getOpposite());
            if (!context.getWorld().getBlockState(supportPos)
                    .isSideSolidFullSquare(context.getWorld(), supportPos, facing)) {
                return null;
            }
        }

        return getDefaultState().with(FACING, facing).with(PART, 0);
    }

    @Override
    public void onPlaced(World world, BlockPos pos, BlockState state, LivingEntity placer, ItemStack itemStack) {
        super.onPlaced(world, pos, state, placer, itemStack);
        if (world.isClient) return;

        Direction right = state.get(FACING).rotateYCounterclockwise();
        world.setBlockState(pos.offset(right), state.with(PART, 1), Block.NOTIFY_ALL);
        world.setBlockState(pos.up(), state.with(PART, 2), Block.NOTIFY_ALL);
        world.setBlockState(pos.up().offset(right), state.with(PART, 3), Block.NOTIFY_ALL);
    }

    @Override
    public boolean canPlaceAt(BlockState state, WorldView world, BlockPos pos) {
        Direction facing = state.get(FACING);
        BlockPos supportPos = pos.offset(facing.getOpposite());
        return world.getBlockState(supportPos).isSideSolidFullSquare(world, supportPos, facing);
    }

    @Override
    public BlockState getStateForNeighborUpdate(BlockState state, Direction direction, BlockState neighborState,
                                                 WorldAccess world, BlockPos pos, BlockPos neighborPos) {
        if (direction == state.get(FACING).getOpposite() && !state.canPlaceAt(world, pos)) {
            return Blocks.AIR.getDefaultState();
        }
        return super.getStateForNeighborUpdate(state, direction, neighborState, world, pos, neighborPos);
    }

    @Override
    public void onStateReplaced(BlockState state, World world, BlockPos pos, BlockState newState, boolean moved) {
        if (!state.isOf(newState.getBlock())) {
            BlockPos anchor = getAnchorPos(pos, state);
            Direction right = state.get(FACING).rotateYCounterclockwise();
            BlockPos[] positions = new BlockPos[] {
                    anchor,
                    anchor.offset(right),
                    anchor.up(),
                    anchor.up().offset(right)
            };
            for (BlockPos partPos : positions) {
                if (!partPos.equals(pos) && world.getBlockState(partPos).isOf(this)) {
                    world.removeBlock(partPos, false);
                }
            }
        }
        super.onStateReplaced(state, world, pos, newState, moved);
    }

    private BlockPos getAnchorPos(BlockPos pos, BlockState state) {
        Direction right = state.get(FACING).rotateYCounterclockwise();
        return switch (state.get(PART)) {
            case 1 -> pos.offset(right.getOpposite());
            case 2 -> pos.down();
            case 3 -> pos.down().offset(right.getOpposite());
            default -> pos;
        };
    }

    @Override
    public VoxelShape getOutlineShape(BlockState state, BlockView world, BlockPos pos, ShapeContext context) {
        return switch (state.get(FACING)) {
            case SOUTH -> SOUTH_SHAPE;
            case WEST -> WEST_SHAPE;
            case EAST -> EAST_SHAPE;
            default -> NORTH_SHAPE;
        };
    }

    @Override
    public BlockState rotate(BlockState state, BlockRotation rotation) {
        return state.with(FACING, rotation.rotate(state.get(FACING)));
    }

    @Override
    public BlockState mirror(BlockState state, BlockMirror mirror) {
        return state.rotate(mirror.getRotation(state.get(FACING)));
    }

    @Override
    protected void appendProperties(StateManager.Builder<Block, BlockState> builder) {
        builder.add(FACING, PART);
    }
}
