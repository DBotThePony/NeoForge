/*
 * Copyright (c) Forge Development LLC and contributors
 * SPDX-License-Identifier: LGPL-2.1-only
 */

package net.neoforged.neoforge.oldtest.block;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.BaseRailBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour.Properties;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.block.state.properties.Property;
import net.minecraft.world.level.block.state.properties.RailShape;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.Fluids;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

@Mod(ValidRailShapeTest.MOD_ID)
public class ValidRailShapeTest {
    public static final String MOD_ID = "valid_railshape_test";
    public static final boolean ENABLED = true;

    private static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(MOD_ID);
    private static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(MOD_ID);

    private static final DeferredBlock<Block> RAIL_SLOPE_BLOCK = BLOCKS.registerBlock("rail_slope", RailSlopeBlock::new, Properties.of());
    private static final DeferredItem<BlockItem> RAIL_SLOPE_ITEM = ITEMS.registerSimpleBlockItem(RAIL_SLOPE_BLOCK);

    public ValidRailShapeTest(IEventBus bus) {
        if (ENABLED) {
            BLOCKS.register(bus);
            ITEMS.register(bus);
        }
    }

    private static class RailSlopeBlock extends BaseRailBlock {
        public static final MapCodec<RailSlopeBlock> CODEC = simpleCodec(RailSlopeBlock::new);
        private static final EnumProperty<RailShape> ASCENDING_RAIL_SHAPE = EnumProperty.create("shape", RailShape.class, RailShape::isSlope);

        protected RailSlopeBlock(Properties properties) {
            super(true, properties.noCollission().strength(0.7F).sound(SoundType.METAL));
        }

        @Override
        protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
            builder.add(ASCENDING_RAIL_SHAPE, BaseRailBlock.WATERLOGGED);
        }

        @Override
        public BlockState getStateForPlacement(BlockPlaceContext context) {
            RailShape shape = switch (context.getHorizontalDirection()) {
                case NORTH -> RailShape.ASCENDING_NORTH;
                case EAST -> RailShape.ASCENDING_EAST;
                case SOUTH -> RailShape.ASCENDING_SOUTH;
                case WEST -> RailShape.ASCENDING_WEST;
                default -> throw new IllegalArgumentException("Invalid facing " + context.getHorizontalDirection());
            };

            FluidState fluid = context.getLevel().getFluidState(context.getClickedPos());

            return defaultBlockState()
                    .setValue(ASCENDING_RAIL_SHAPE, shape)
                    .setValue(BaseRailBlock.WATERLOGGED, fluid.getType() == Fluids.WATER);
        }

        @Override
        protected MapCodec<RailSlopeBlock> codec() {
            return CODEC;
        }

        @Override
        public boolean canSurvive(BlockState state, LevelReader level, BlockPos pos) {
            RailShape shape = state.getValue(ASCENDING_RAIL_SHAPE);
            Direction dir = switch (shape) {
                case ASCENDING_NORTH -> Direction.NORTH;
                case ASCENDING_EAST -> Direction.EAST;
                case ASCENDING_SOUTH -> Direction.SOUTH;
                case ASCENDING_WEST -> Direction.WEST;
                default -> throw new IllegalArgumentException("Invalid shape " + shape);
            };

            if (!canSupportRigidBlock(level, pos.relative(dir))) {
                return false;
            }

            return super.canSurvive(state, level, pos);
        }

        @Override
        public boolean isValidRailShape(RailShape shape) {
            return shape.isSlope();
        }

        @Override
        public Property<RailShape> getShapeProperty() {
            return ASCENDING_RAIL_SHAPE;
        }
    }
}
