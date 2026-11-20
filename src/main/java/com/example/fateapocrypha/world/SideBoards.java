package com.example.fateapocrypha.world;

import com.example.fateapocrypha.story.SideBoard;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Ставит доски объявлений побочных заданий: плоский деревянный щит между двумя столбами, под навесом, на щите
 * клиент рисует список заданий, по бокам фонари, под ногами каменная площадка.
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
            if (!level.hasChunk((x - 4) >> 4, (z - 4) >> 4) || !level.hasChunk((x + 4) >> 4, (z + 4) >> 4)) {
                continue;
            }
            int y = board.y();
            if (y - 1 < TrifasTerrain.SEA + 1) {
                continue;   // вода: доску не ставим
            }
            // щит стоит на высоте y + 1; если его там нет, доска строится (или перестраивается старая)
            if (level.getBlockState(new BlockPos(x, y + 1, z)).is(Blocks.DARK_OAK_PLANKS)) {
                continue;
            }
            build(level, board, x, y, z);
        }
    }

    private static Direction direction(int facing) {
        return switch (facing) {
            case 0 -> Direction.NORTH;
            case 1 -> Direction.EAST;
            case 2 -> Direction.SOUTH;
            default -> Direction.WEST;
        };
    }

    /** Блок в осях доски: side вбок (по часовой от взгляда), depth вперёд (к дороге). */
    private static void put(BuildKit kit, int x, int z, Direction f, int side, int depth, int y, BlockState state) {
        Direction s = f.getClockWise();
        kit.set(x + s.getStepX() * side + f.getStepX() * depth, y, z + s.getStepZ() * side + f.getStepZ() * depth, state);
    }

    private static void build(ServerLevel level, SideBoard board, int x, int y, int z) {
        BuildKit kit = new BuildKit(level);
        Direction f = direction(board.facing());
        // расчистка объёма доски (в том числе остатки старой версии) и каменная площадка
        for (int side = -3; side <= 3; side++) {
            for (int depth = -1; depth <= 2; depth++) {
                for (int yy = y; yy <= y + 5; yy++) {
                    put(kit, x, z, f, side, depth, yy, BuildKit.AIR);
                }
                put(kit, x, z, f, side, depth, y - 1, BuildKit.s(Blocks.STONE_BRICKS));
            }
        }
        BlockState log = BuildKit.s(Blocks.STRIPPED_SPRUCE_LOG);
        BlockState frame = BuildKit.s(Blocks.DARK_OAK_PLANKS);
        // два столба и навес
        for (int side : new int[] {-2, 2}) {
            for (int yy = y; yy <= y + 3; yy++) {
                put(kit, x, z, f, side, 0, yy, log);
            }
            put(kit, x, z, f, side, 0, y + 4, BuildKit.s(Blocks.LANTERN));
        }
        for (int side = -2; side <= 2; side++) {
            put(kit, x, z, f, side, 0, y + 3, BuildKit.s(Blocks.SPRUCE_SLAB));
            put(kit, x, z, f, side, 1, y + 3, BuildKit.s(Blocks.SPRUCE_SLAB));
        }
        // щит: два ряда по три доски между столбами (центральный блок на высоте y + 1 служит меткой построенной доски)
        for (int side = -1; side <= 1; side++) {
            put(kit, x, z, f, side, 0, y + 1, frame);
            put(kit, x, z, f, side, 0, y + 2, frame);
        }
        // задания пишутся на самом щите: их рисует клиент (BoardRenderer), блоков-табличек нет
        // бочка и компостер по бокам
        put(kit, x, z, f, -2, 1, y, BuildKit.s(Blocks.BARREL));
        put(kit, x, z, f, 2, 1, y, BuildKit.s(Blocks.COMPOSTER));
    }
}
