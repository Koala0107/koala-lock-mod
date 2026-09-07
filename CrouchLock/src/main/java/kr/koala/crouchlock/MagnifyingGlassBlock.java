package kr.koala.crouchlock;

import com.mojang.serialization.MapCodec;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.HorizontalFacingBlock;
import net.minecraft.block.ShapeContext;
import net.minecraft.item.ItemPlacementContext;
import net.minecraft.state.StateManager;
import net.minecraft.util.BlockMirror;
import net.minecraft.util.BlockRotation;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.world.BlockView;
import net.minecraft.world.WorldView;

public final class MagnifyingGlassBlock extends HorizontalFacingBlock {
    public static final MapCodec<MagnifyingGlassBlock> CODEC = createCodec(MagnifyingGlassBlock::new);

    private static final VoxelShape NORTH = Block.createCuboidShape(0, 0, 14, 16, 16, 16);
    private static final VoxelShape SOUTH = Block.createCuboidShape(0, 0, 0, 16, 16, 2);
    private static final VoxelShape WEST = Block.createCuboidShape(14, 0, 0, 16, 16, 16);
    private static final VoxelShape EAST = Block.createCuboidShape(0, 0, 0, 2, 16, 16);

    public MagnifyingGlassBlock(Settings settings) {
        super(settings);
        setDefaultState(getStateManager().getDefaultState().with(FACING, Direction.NORTH));
    }

    @Override
    protected MapCodec<? extends HorizontalFacingBlock> getCodec() {
        return CODEC;
    }

    @Override
    public BlockState getPlacementState(ItemPlacementContext context) {
        Direction side = context.getSide();
        if (!side.getAxis().isHorizontal()) return null;
        Direction facing = side;
        BlockState state = getDefaultState().with(FACING, facing);
        return canPlaceAt(state, context.getWorld(), context.getBlockPos()) ? state : null;
    }

    @Override
    public boolean canPlaceAt(BlockState state, WorldView world, BlockPos pos) {
        Direction facing = state.get(FACING);
        BlockPos supportPos = pos.offset(facing.getOpposite());
        return world.getBlockState(supportPos).isSideSolidFullSquare(world, supportPos, facing);
    }

    @Override
    public VoxelShape getOutlineShape(BlockState state, BlockView world, BlockPos pos, ShapeContext context) {
        return switch (state.get(FACING)) {
            case SOUTH -> SOUTH;
            case WEST -> WEST;
            case EAST -> EAST;
            default -> NORTH;
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
        builder.add(FACING);
    }
}
