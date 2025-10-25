package com.example.storytell.init.blocks;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

import org.jetbrains.annotations.Nullable;

public class UpgradeTransmitterBlock extends Block implements EntityBlock {

    public static final DirectionProperty FACING = BlockStateProperties.HORIZONTAL_FACING;

    // Точный хитбокс, соответствующий модели из upgrade_transmitter.json
    private static final VoxelShape SHAPE = Shapes.or(
            // Основание (нижняя часть)
            Block.box(1.0D, 0.0D, 1.0D, 15.0D, 5.0D, 15.0D),
            // Средняя часть
            Block.box(2.0D, 5.0D, 2.0D, 14.0D, 7.0D, 14.0D),
            // Верхняя центральная колонна
            Block.box(5.0D, 6.0D, 5.0D, 11.0D, 12.0D, 11.0D),
            // Верхняя декоративная часть
            Block.box(4.0D, 6.0D, 4.0D, 12.0D, 9.0D, 12.0D),
            // Верхний шпиль
            Block.box(6.0D, 10.0D, 6.0D, 10.0D, 15.0D, 10.0D),
            // Самый верхний элемент
            Block.box(7.0D, 14.0D, 7.0D, 9.0D, 16.0D, 9.0D),
            // Боковые стороны основания
            Block.box(0.0D, 0.0D, 2.0D, 1.0D, 3.0D, 14.0D),
            Block.box(15.0D, 0.0D, 2.0D, 16.0D, 3.0D, 14.0D),
            Block.box(2.0D, 0.0D, 0.0D, 14.0D, 3.0D, 1.0D),
            Block.box(2.0D, 0.0D, 15.0D, 14.0D, 3.0D, 16.0D)
    );

    public UpgradeTransmitterBlock(BlockBehaviour.Properties properties) {
        super(properties);
        this.registerDefaultState(this.stateDefinition.any().setValue(FACING, Direction.NORTH));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING);
    }

    @Nullable
    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        // Блок будет ориентирован в сторону, куда смотрит игрок при установке
        return this.defaultBlockState().setValue(FACING, context.getHorizontalDirection().getOpposite());
    }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter worldIn, BlockPos pos, CollisionContext context) {
        return SHAPE;
    }

    @Override
    public VoxelShape getCollisionShape(BlockState state, BlockGetter worldIn, BlockPos pos, CollisionContext context) {
        return SHAPE;
    }

    public VoxelShape getOcclusionShape(BlockState state, BlockGetter worldIn, BlockPos pos, CollisionContext context) {
        return SHAPE;
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new UpgradeTransmitterBlockEntity(pos, state);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return createTickerHelper(type, ModBlockEntities.UPGRADE_TRANSMITTER_BLOCK_ENTITY_TYPE.get(), UpgradeTransmitterBlockEntity::tick);
    }

    @Override
    public void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean isMoving) {
        if (!state.is(newState.getBlock())) {
            BlockEntity blockEntity = level.getBlockEntity(pos);
            if (blockEntity instanceof UpgradeTransmitterBlockEntity) {
                ((UpgradeTransmitterBlockEntity) blockEntity).removeHologram();
            }
        }
        super.onRemove(state, level, pos, newState, isMoving);
    }

    @Nullable
    protected static <E extends BlockEntity, A extends BlockEntity> BlockEntityTicker<A> createTickerHelper(
            BlockEntityType<A> type, BlockEntityType<E> targetType, BlockEntityTicker<? super E> ticker) {
        return targetType == type ? (BlockEntityTicker<A>) ticker : null;
    }

    @Override
    public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player,
                                 InteractionHand hand, BlockHitResult hit) {
        if (level.isClientSide) {
            // Открываем GUI на клиенте
            net.minecraft.client.Minecraft.getInstance().setScreen(new UpgradeTransmitterScreen(pos));
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }
}