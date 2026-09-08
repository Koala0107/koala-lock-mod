package kr.koala.korime_scene;

import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.item.ItemPlacementContext;
import net.minecraft.state.StateManager;
import net.minecraft.state.property.BooleanProperty;
import net.minecraft.util.math.BlockPos;

public final class BoundaryEdgeBlock extends BoundaryLineBlock {
    public static final BooleanProperty FLIPPED = BooleanProperty.of("flipped");

    private final boolean horizontal;

    public BoundaryEdgeBlock(Settings settings, boolean horizontal) {
        super(settings);
        this.horizontal = horizontal;
        setDefaultState(getStateManager().getDefaultState().with(FLIPPED, false));
    }

    @Override
    public BlockState getPlacementState(ItemPlacementContext context) {
        BlockPos pos = context.getBlockPos();
        double localCoordinate = horizontal
                ? context.getHitPos().getZ() - pos.getZ()
                : context.getHitPos().getX() - pos.getX();
        return getDefaultState().with(FLIPPED, localCoordinate >= 0.5D);
    }

    @Override
    protected void appendProperties(StateManager.Builder<Block, BlockState> builder) {
        builder.add(FLIPPED);
    }
}
