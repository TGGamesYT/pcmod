package dev.tggamesyt.pcmod;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

public class PCblock extends Block {

    VoxelShape shape0 = Block.box(0.25f, 0f, 0.0625f, 0.3125f, 0.0625f, 0.1875f);
    VoxelShape shape1 = Block.box(0.25f, 0f, 0.8125f, 0.3125f, 0.0625f, 0.9375f);
    VoxelShape shape2 = Block.box(0.6875f, 0f, 0.8125f, 0.75f, 0.0625f, 0.9375f);
    VoxelShape shape3 = Block.box(0.6875f, 0f, 0.0625f, 0.75f, 0.0625f, 0.1875f);
    VoxelShape shape4 = Block.box(0.25f, 0.0625f, 0.0625f, 0.75f, 1.125f, 0.9375f);
    VoxelShape COLLISION_SHAPE = Shapes.or(shape0, shape1, shape2, shape3, shape4);

    public PCblock(Properties properties) {
        super(properties);
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return COLLISION_SHAPE;
    }

    // Overrides the physical block collision (prevents players/mobs from walking through it)
    @Override
    protected VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return COLLISION_SHAPE;
    }
}
