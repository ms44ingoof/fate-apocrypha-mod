package com.example.fateapocrypha.world;

import com.example.fateapocrypha.story.StoryLocation;
import java.util.HashSet;
import java.util.Set;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Ворота на границе каждой сюжетной локации и проход через кольцо деревьев: путь из ворот ведёт
 * к площадке. Стиль ворот зависит от локации (замок, лес, лагерь, церковь, руины, город, сады...).
 */
public final class StoryGates {
    private StoryGates() {
    }

    private record Style(BlockState pillar, BlockState pillarAlt, BlockState beam, BlockState slab, BlockState floor,
                         BlockState banner, boolean bars, boolean broken, boolean soul) {
    }

    private static BlockState s(Block block) {
        return block.defaultBlockState();
    }

    private static Style style(StoryLocation location) {
        switch (location) {
            case CASTLE_LAB:
            case CASTLE_CORRIDOR:
            case CASTLE_GATE:
                return new Style(s(Blocks.STONE_BRICKS), s(Blocks.CRACKED_STONE_BRICKS), s(Blocks.DEEPSLATE_BRICKS),
                        s(Blocks.DEEPSLATE_BRICK_SLAB), s(Blocks.COBBLESTONE), s(Blocks.BLACK_BANNER), true, false, true);
            case FOREST:
                return new Style(s(Blocks.DARK_OAK_LOG), s(Blocks.STRIPPED_DARK_OAK_LOG), s(Blocks.DARK_OAK_PLANKS),
                        s(Blocks.DARK_OAK_SLAB), s(Blocks.COARSE_DIRT), s(Blocks.GREEN_BANNER), false, false, false);
            case RED_CAMP:
                return new Style(s(Blocks.SPRUCE_LOG), s(Blocks.STRIPPED_SPRUCE_LOG), s(Blocks.SPRUCE_PLANKS),
                        s(Blocks.SPRUCE_SLAB), s(Blocks.COARSE_DIRT), s(Blocks.RED_BANNER), false, false, false);
            case CHAPEL:
                return new Style(s(Blocks.STONE_BRICKS), s(Blocks.MOSSY_STONE_BRICKS), s(Blocks.POLISHED_ANDESITE),
                        s(Blocks.STONE_BRICK_SLAB), s(Blocks.STONE_BRICKS), s(Blocks.PURPLE_BANNER), false, false, true);
            case RUINS:
                return new Style(s(Blocks.CRACKED_STONE_BRICKS), s(Blocks.MOSSY_COBBLESTONE), s(Blocks.COBBLESTONE),
                        s(Blocks.COBBLESTONE_SLAB), s(Blocks.GRAVEL), s(Blocks.GRAY_BANNER), false, true, false);
            case STREET:
            case LONDON:
                return new Style(s(Blocks.BRICKS), s(Blocks.POLISHED_ANDESITE), s(Blocks.STONE_BRICKS),
                        s(Blocks.BRICK_SLAB), s(Blocks.STONE_BRICKS), s(Blocks.WHITE_BANNER), true, false, false);
            case SIEGE:
                return new Style(s(Blocks.OAK_LOG), s(Blocks.STRIPPED_OAK_LOG), s(Blocks.OAK_PLANKS),
                        s(Blocks.OAK_SLAB), s(Blocks.GRAVEL), s(Blocks.WHITE_BANNER), true, false, false);
            case PLAIN:
                return new Style(s(Blocks.MOSSY_STONE_BRICKS), s(Blocks.STONE_BRICKS), s(Blocks.STONE_BRICKS),
                        s(Blocks.STONE_BRICK_SLAB), s(Blocks.GRAVEL), s(Blocks.GREEN_BANNER), false, false, false);
            case FARM:
                return new Style(s(Blocks.OAK_LOG), s(Blocks.STRIPPED_OAK_LOG), s(Blocks.OAK_PLANKS),
                        s(Blocks.OAK_SLAB), s(Blocks.DIRT_PATH), s(Blocks.YELLOW_BANNER), false, false, false);
            case GARDENS:
                return new Style(s(Blocks.QUARTZ_PILLAR), s(Blocks.SMOOTH_QUARTZ), s(Blocks.QUARTZ_BLOCK),
                        s(Blocks.QUARTZ_SLAB), s(Blocks.SMOOTH_QUARTZ), s(Blocks.WHITE_BANNER), false, false, false);
            default:
                return new Style(s(Blocks.STONE_BRICKS), s(Blocks.STONE_BRICKS), s(Blocks.STONE_BRICKS),
                        s(Blocks.STONE_BRICK_SLAB), s(Blocks.COBBLESTONE), s(Blocks.WHITE_BANNER), false, false, false);
        }
    }

    /** Строит все ворота и проходы локации. */
    public static void build(ServerLevel level, StoryLocation location) {
        BuildKit kit = new BuildKit(level);
        Style style = style(location);
        for (TrifasRoads.Opening o : TrifasRoads.openings(location)) {
            carve(kit, o);
            gate(kit, o, style);
        }
    }

    /** Пробивает кольцо деревьев и насыпь: проход шириной 7 от края площадки до ворот, поверх него тропа. */
    private static void carve(BuildKit kit, TrifasRoads.Opening o) {
        double len = Math.hypot(o.gx() - o.ex(), o.gz() - o.ez());
        Set<Long> seen = new HashSet<>();
        for (double t = -2.0D; t <= len + 3.0D; t += 0.7D) {
            double cx = o.ex() + o.ux() * t;
            double cz = o.ez() + o.uz() * t;
            for (int sd = -3; sd <= 3; sd++) {
                int x = (int) Math.round(cx - o.uz() * sd);
                int z = (int) Math.round(cz + o.ux() * sd);
                if (!seen.add(((long) x << 32) ^ (z & 0xFFFFFFFFL))) {
                    continue;
                }
                int base = BuildKit.ground(x, z);
                for (int y = base + 1; y <= base + 14; y++) {
                    if (!kit.isAir(x, y, z)) {
                        kit.set(x, y, z, BuildKit.AIR);
                    }
                }
                if (base >= TrifasTerrain.SEA) {
                    boolean edge = Math.abs(sd) == 3;
                    kit.set(x, base, z, edge ? BuildKit.s(Blocks.COARSE_DIRT) : BuildKit.s(Blocks.DIRT_PATH));
                }
            }
        }
    }

    private static void gate(BuildKit kit, TrifasRoads.Opening o, Style st) {
        boolean alongX = Math.abs(o.ux()) >= Math.abs(o.uz());   // проход идёт вдоль оси x
        int gx = o.gx();
        int gz = o.gz();
        int y0 = BuildKit.ground(gx, gz);

        // мощение под воротами и свободный проём
        for (int across = -3; across <= 3; across++) {
            for (int along = -1; along <= 2; along++) {
                int x = alongX ? gx + along : gx + across;
                int z = alongX ? gz + across : gz + along;
                int base = BuildKit.ground(x, z);
                for (int y = base + 1; y <= y0 + 9; y++) {
                    kit.set(x, y, z, BuildKit.AIR);
                }
                kit.set(x, base, z, st.floor());
                for (int y = base - 1; y >= base - 6; y--) {
                    // подпорка, если ворота стоят на склоне
                    if (kit.isAir(x, y, z)) {
                        kit.set(x, y, z, BuildKit.s(Blocks.DIRT));
                    }
                }
            }
        }

        for (int side : new int[] {-4, 4}) {
            for (int along = 0; along <= 1; along++) {
                int x = alongX ? gx + along : gx + side;
                int z = alongX ? gz + side : gz + along;
                int base = BuildKit.ground(x, z);
                int top = y0 + (st.broken() && side < 0 ? 4 : 8);
                for (int y = Math.min(base, y0) - 2; y <= top; y++) {
                    boolean alt = (y + along) % 3 == 0;
                    kit.set(x, y, z, alt ? st.pillarAlt() : st.pillar());
                }
                // знамя на столбе
                if (!st.broken() || side > 0) {
                    kit.set(x, top + 1, z, st.banner());
                }
            }
        }

        // балка над проходом
        if (!st.broken()) {
            for (int across = -4; across <= 4; across++) {
                for (int along = 0; along <= 1; along++) {
                    int x = alongX ? gx + along : gx + across;
                    int z = alongX ? gz + across : gz + along;
                    kit.set(x, y0 + 7, z, st.beam());
                    if (along == 0) {
                        kit.set(x, y0 + 8, z, st.slab());
                    }
                }
            }
        } else {
            // разрушенная арка: обломок балки только с одной стороны
            for (int across = 1; across <= 4; across++) {
                int x = alongX ? gx : gx + across;
                int z = alongX ? gz + across : gz;
                kit.set(x, y0 + 7, z, st.beam());
            }
        }

        // решётка-герса над проёмом
        if (st.bars() && !st.broken()) {
            for (int across = -3; across <= 3; across++) {
                int x = alongX ? gx : gx + across;
                int z = alongX ? gz + across : gz;
                kit.set(x, y0 + 6, z, BuildKit.s(Blocks.IRON_BARS));
                kit.set(x, y0 + 5, z, BuildKit.s(Blocks.IRON_BARS));
            }
        }

        // фонари под балкой и столбики света у входа
        for (int across : new int[] {-2, 2}) {
            int x = alongX ? gx : gx + across;
            int z = alongX ? gz + across : gz;
            if (!st.broken()) {
                kit.set(x, y0 + 6, z, st.soul()
                        ? Blocks.SOUL_LANTERN.defaultBlockState().setValue(net.minecraft.world.level.block.LanternBlock.HANGING, true)
                        : BuildKit.HANGING_LANTERN);
            }
        }
        for (int side : new int[] {-5, 5}) {
            int x = alongX ? gx : gx + side;
            int z = alongX ? gz + side : gz;
            int base = BuildKit.ground(x, z);
            kit.set(x, base + 1, z, BuildKit.s(Blocks.OAK_FENCE));
            kit.set(x, base + 2, z, BuildKit.s(Blocks.OAK_FENCE));
            kit.set(x, base + 3, z, st.soul() ? BuildKit.s(Blocks.SOUL_LANTERN) : BuildKit.s(Blocks.LANTERN));
        }
    }
}
