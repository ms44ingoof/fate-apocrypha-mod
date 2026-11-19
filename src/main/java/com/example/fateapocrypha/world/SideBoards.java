package com.example.fateapocrypha.world;

import com.example.fateapocrypha.story.SideBoard;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LecternBlock;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Ставит доски объявлений побочных заданий в мире: лектор на каменной площадке и столб с фонарём.
 * Доска строится, когда игрок подошёл ближе 96 блоков и нужные чанки загружены; пропавшая доска ставится заново.
 */
public final class SideBoards {
    private SideBoards() {
    }

    public static void ensureNear(ServerLevel level, ServerPlayer player) {
        for (SideBoard board : SideBoard.values()) {
            int x = board.x();
            int z = board.z();
            if (Math.abs(x - player.getX()) > 96.0D || Math.abs(z - player.getZ()) > 96.0D) {
                continue;
            }
            if (!level.hasChunk((x - 2) >> 4, (z - 2) >> 4) || !level.hasChunk((x + 2) >> 4, (z + 2) >> 4)) {
                continue;
            }
            int y = board.y();
            if (y - 1 < TrifasTerrain.SEA + 1) {
                continue;   // вода: доску не ставим
            }
            if (level.getBlockState(new BlockPos(x, y, z)).is(Blocks.LECTERN)) {
                continue;
            }
            build(level, board, x, y, z);
        }
    }

    private static void build(ServerLevel level, SideBoard board, int x, int y, int z) {
        BuildKit kit = new BuildKit(level);
        // расчистка растительности и каменная площадка 3x3
        for (int dx = -1; dx <= 1; dx++) {
            for (int dz = -1; dz <= 1; dz++) {
                for (int yy = y; yy <= y + 4; yy++) {
                    kit.clearVegetation(x + dx, yy, z + dz);
                }
                kit.set(x + dx, y - 1, z + dz, BuildKit.s(Blocks.STONE_BRICKS));
            }
        }
        Direction facing = switch (board.facing()) {
            case 0 -> Direction.NORTH;
            case 1 -> Direction.EAST;
            case 2 -> Direction.SOUTH;
            default -> Direction.WEST;
        };
        BlockState lectern = Blocks.LECTERN.defaultBlockState().setValue(LecternBlock.FACING, facing);
        kit.set(x, y, z, lectern);
        // столб с фонарём сбоку от лектора
        Direction side = facing.getClockWise();
        int px = x + side.getStepX();
        int pz = z + side.getStepZ();
        kit.set(px, y, pz, BuildKit.s(Blocks.OAK_FENCE));
        kit.set(px, y + 1, pz, BuildKit.s(Blocks.OAK_FENCE));
        kit.set(px, y + 2, pz, BuildKit.s(Blocks.LANTERN));
    }
}
