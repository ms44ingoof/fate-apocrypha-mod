package com.example.fateapocrypha.world;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.DoorBlock;
import net.minecraft.world.level.block.StairBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.minecraft.world.level.block.state.properties.Half;

/** Небольшой набор операций для построек мира (ворота, дома, колодцы). */
final class BuildKit {
    static final BlockState AIR = Blocks.AIR.defaultBlockState();
    static final BlockState HANGING_LANTERN = Blocks.LANTERN.defaultBlockState()
            .setValue(net.minecraft.world.level.block.LanternBlock.HANGING, true);

    final ServerLevel level;

    BuildKit(ServerLevel level) {
        this.level = level;
    }

    static BlockState s(Block block) {
        return block.defaultBlockState();
    }

    void set(int x, int y, int z, BlockState state) {
        level.setBlock(new BlockPos(x, y, z), state, 2);
    }

    void fill(int x1, int y1, int z1, int x2, int y2, int z2, BlockState state) {
        for (int x = Math.min(x1, x2); x <= Math.max(x1, x2); x++) {
            for (int y = Math.min(y1, y2); y <= Math.max(y1, y2); y++) {
                for (int z = Math.min(z1, z2); z <= Math.max(z1, z2); z++) {
                    set(x, y, z, state);
                }
            }
        }
    }

    boolean isAir(int x, int y, int z) {
        return level.getBlockState(new BlockPos(x, y, z)).isAir();
    }

    /** Высота верхнего твёрдого блока по функции рельефа (не зависит от деревьев и построек). */
    static int ground(int x, int z) {
        return TrifasTerrain.height(x, z);
    }

    static BlockState stairs(Block block, Direction facing, boolean top) {
        return block.defaultBlockState().setValue(StairBlock.FACING, facing)
                .setValue(StairBlock.HALF, top ? Half.TOP : Half.BOTTOM);
    }

    void door(Block door, int x, int y, int z, Direction facing) {
        set(x, y, z, door.defaultBlockState().setValue(DoorBlock.FACING, facing).setValue(DoorBlock.HALF, DoubleBlockHalf.LOWER));
        set(x, y + 1, z, door.defaultBlockState().setValue(DoorBlock.FACING, facing).setValue(DoorBlock.HALF, DoubleBlockHalf.UPPER));
    }
}
