package com.example.storytell.init.radio;

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
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.Nullable;

public class RadioBlock extends Block implements EntityBlock {
    public static final DirectionProperty FACING = HorizontalDirectionalBlock.FACING;
    public static final BooleanProperty ENABLED = BooleanProperty.create("enabled");

    // Хитбоксы для каждого направления, соответствующие модели
    private static final VoxelShape SHAPE_NORTH = Shapes.or(
            Block.box(1, 0, 5, 15, 8, 11),    // Основной корпус
            Block.box(11.25, 8, 7.25, 12.75, 16, 8.75) // Антенна справа
    );

    private static final VoxelShape SHAPE_SOUTH = Shapes.or(
            Block.box(1, 0, 5, 15, 8, 11),    // Основной корпус
            Block.box(3.25, 8, 7.25, 4.75, 16, 8.75) // Антенна слева
    );

    private static final VoxelShape SHAPE_EAST = Shapes.or(
            Block.box(5, 0, 1, 11, 8, 15),    // Основной корпус
            Block.box(7.25, 8, 11.25, 8.75, 16, 12.75) // Антенна справа (соответствует модели)
    );

    private static final VoxelShape SHAPE_WEST = Shapes.or(
            Block.box(5, 0, 1, 11, 8, 15),    // Основной корпус
            Block.box(7.25, 8, 3.25, 8.75, 16, 4.75) // Антенна слева (соответствует модели)
    );

    public RadioBlock(Properties properties) {
        super(properties);
        this.registerDefaultState(this.stateDefinition.any()
                .setValue(FACING, Direction.NORTH)
                .setValue(ENABLED, false));
    }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter worldIn, BlockPos pos, CollisionContext context) {
        Direction direction = state.getValue(FACING);

        switch (direction) {
            case NORTH:
                return SHAPE_NORTH;
            case SOUTH:
                return SHAPE_SOUTH;
            case EAST:
                return SHAPE_EAST;
            case WEST:
                return SHAPE_WEST;
            default:
                return SHAPE_NORTH;
        }
    }

    @Override
    public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
        if (!level.isClientSide) {
            BlockState newState = state.cycle(ENABLED);
            level.setBlock(pos, newState, 3);

            // Уведомляем BlockEntity об изменении состояния
            if (level.getBlockEntity(pos) instanceof RadioBlockEntity radio) {
                radio.setEnabled(newState.getValue(ENABLED));

                // Воспроизводим звук при включении
                if (newState.getValue(ENABLED)) {
                    radio.playRadioSound();
                }
            }

            return InteractionResult.SUCCESS;
        }
        return InteractionResult.CONSUME;
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new RadioBlockEntity(pos, state);
    }

    @Nullable
    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        return this.defaultBlockState().setValue(FACING, context.getHorizontalDirection().getOpposite());
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING, ENABLED);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return createTickerHelper(type, ModRadioBlockEntities.RADIO_BLOCK_ENTITY_TYPE.get(), RadioBlockEntity::tick);
    }

    @Nullable
    protected static <E extends BlockEntity, A extends BlockEntity> BlockEntityTicker<A> createTickerHelper(
            BlockEntityType<A> type, BlockEntityType<E> targetType, BlockEntityTicker<? super E> ticker) {
        return targetType == type ? (BlockEntityTicker<A>) ticker : null;
    }
    @Override
    public void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean isMoving) {
        if (!state.is(newState.getBlock())) {
            if (level.getBlockEntity(pos) instanceof RadioBlockEntity radio) {
                // Помечаем радио как сломанное при удалении блока
                radio.setBroken(true);
            }
            super.onRemove(state, level, pos, newState, isMoving);
        }
    }
}