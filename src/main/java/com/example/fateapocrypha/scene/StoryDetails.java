package com.example.fateapocrypha.scene;

import com.example.fateapocrypha.story.StoryLocation;
import com.example.fateapocrypha.world.StoryGates;
import java.util.Random;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CandleBlock;
import net.minecraft.world.level.block.RotatedPillarBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;

/**
 * Проход детализации: дополнительные мелкие детали на уже построенных локациях
 * (цветы, костры, знамёна, фонарные столбы, баррикады, фонтаны, горящие руины).
 *
 * Детали ставятся ТОЛЬКО в свободный воздух над твёрдым блоком, поэтому уже построенные здания,
 * ковры, посевы и точки сцен не затрагиваются. Каждая локация обрабатывается один раз
 * (метка: блок мха под меткой "построено"). /fate build пересоздаёт локации заново.
 */
public final class StoryDetails {
    private static final int Y0 = StoryWorld.Y0;

    private StoryDetails() {
    }

    private static BlockState s(Block block) {
        return block.defaultBlockState();
    }

    /** Вызывается после построения любой локации и при старте сцены; идемпотентно. */
    public static void ensure(ServerLevel l) {
        if (l == null) {
            return;
        }
        run(l, 100, 100, 6);   // замок: лаборатория и коридор
        run(l, StoryWorld.FOREST_CX, StoryWorld.FOREST_CZ, 1);
        run(l, StoryWorld.STAGE_RED_X, StoryWorld.STAGE_RED_Z, 2);
        run(l, StoryWorld.STAGE_STREET_X, StoryWorld.STAGE_STREET_Z, 3);
        run(l, StoryWorld.FARM_X, StoryWorld.FARM_Z, 4);
        run(l, StoryWorld.GARDEN_X, StoryWorld.GARDEN_Z, 5);

        // ворота и проходы через кольцо деревьев
        gates(l, 100, 100, StoryLocation.CASTLE_LAB, StoryLocation.CASTLE_CORRIDOR, StoryLocation.CASTLE_GATE);
        gates(l, StoryWorld.FOREST_CX, StoryWorld.FOREST_CZ, StoryLocation.FOREST);
        gates(l, StoryWorld.STAGE_RED_X, StoryWorld.STAGE_RED_Z, StoryLocation.RED_CAMP, StoryLocation.CHAPEL, StoryLocation.RUINS);
        gates(l, StoryWorld.STAGE_STREET_X, StoryWorld.STAGE_STREET_Z, StoryLocation.STREET, StoryLocation.SIEGE,
                StoryLocation.LONDON, StoryLocation.PLAIN);
        gates(l, StoryWorld.FARM_X, StoryWorld.FARM_Z, StoryLocation.FARM);
        gates(l, StoryWorld.GARDEN_X, StoryWorld.GARDEN_Z, StoryLocation.GARDENS);
    }

    private static void gates(ServerLevel l, int mx, int mz, StoryLocation... locations) {
        if (!l.getBlockState(Reloc.pos(mx, Y0 - 1, mz)).is(Blocks.ROOTED_DIRT)) {
            return;
        }
        BlockPos done = Reloc.pos(mx + 2, Y0 - 1, mz);
        if (l.getBlockState(done).is(Blocks.MOSS_BLOCK)) {
            return;
        }
        l.setBlock(done, s(Blocks.MOSS_BLOCK), 2);
        for (StoryLocation location : locations) {
            StoryGates.build(l, location);
        }
    }

    private static void run(ServerLevel l, int mx, int mz, int kind) {
        // локация построена, но детали ещё не ставились
        if (!l.getBlockState(Reloc.pos(mx, Y0 - 1, mz)).is(Blocks.ROOTED_DIRT)) {
            return;
        }
        BlockPos done = Reloc.pos(mx + 1, Y0 - 1, mz);
        if (l.getBlockState(done).is(Blocks.MOSS_BLOCK)) {
            return;
        }
        switch (kind) {
            case 1 -> forest(l);
            case 2 -> stagesA(l);
            case 3 -> stagesB(l);
            case 4 -> farm(l);
            case 6 -> StoryInteriors.build(l);
            default -> gardens(l);
        }
        l.setBlock(done, s(Blocks.MOSS_BLOCK), 2);
    }

    // ---------- помощники ----------

    /** Ставит блок, только если там воздух, а под ним твёрдая опора. */
    private static boolean put(ServerLevel l, int x, int y, int z, BlockState state) {
        BlockPos pos = Reloc.pos(x, y, z);
        if (!l.getBlockState(pos).isAir()) {
            return false;
        }
        BlockPos below = pos.below();
        if (!l.getBlockState(below).isFaceSturdy(l, below, Direction.UP)) {
            return false;
        }
        l.setBlock(pos, state, 2);
        return true;
    }

    private static boolean free(ServerLevel l, int x, int y, int z) {
        return l.getBlockState(Reloc.pos(x, y, z)).isAir();
    }

    /** Столб высотой h: основание и верх (фонарь, знамя). Ничего не ставит, если место занято. */
    private static void post(ServerLevel l, int x, int y, int z, int h, BlockState body, BlockState top) {
        for (int i = 0; i < h; i++) {
            if (!free(l, x, y + i, z)) {
                return;
            }
        }
        if (!free(l, x, y + h, z)) {
            return;
        }
        if (put(l, x, y, z, body)) {
            for (int i = 1; i < h; i++) {
                l.setBlock(Reloc.pos(x, y + i, z), body, 2);
            }
            l.setBlock(Reloc.pos(x, y + h, z), top, 2);
        }
    }

    private static int surface(ServerLevel l, int x, int z) {
        return Reloc.height(l, x, z);
    }

    private static boolean onGrass(ServerLevel l, int x, int y, int z) {
        BlockState below = l.getBlockState(Reloc.pos(x, y - 1, z));
        return below.is(Blocks.GRASS_BLOCK) || below.is(Blocks.PODZOL) || below.is(Blocks.DIRT);
    }

    private static BlockState candles(int count) {
        return Blocks.CANDLE.defaultBlockState().setValue(CandleBlock.CANDLES, count).setValue(CandleBlock.LIT, true);
    }

    private static final Block[] FLOWERS = {Blocks.DANDELION, Blocks.POPPY, Blocks.CORNFLOWER, Blocks.OXEYE_DAISY,
            Blocks.AZURE_BLUET, Blocks.LILY_OF_THE_VALLEY, Blocks.RED_TULIP, Blocks.WHITE_TULIP};

    // ---------- Лес: цветы, папоротники, грибы, поваленные брёвна, валуны, кусты ягод ----------
    private static void forest(ServerLevel l) {
        Random rnd = new Random(31L);
        int x0 = StoryWorld.FOREST_X0;
        int z0 = StoryWorld.FOREST_Z0;
        int size = StoryWorld.FOREST_SIZE;
        for (int i = 0; i < 1400; i++) {
            int x = x0 + 4 + rnd.nextInt(size - 8);
            int z = z0 + 4 + rnd.nextInt(size - 8);
            double dc = Math.hypot(x - StoryWorld.FOREST_CX, z - StoryWorld.FOREST_CZ);
            int y = surface(l, x, z);
            if (!onGrass(l, x, y, z)) {
                continue;
            }
            int roll = rnd.nextInt(20);
            if (roll < 9) {
                put(l, x, y, z, s(FLOWERS[rnd.nextInt(FLOWERS.length)]));
            } else if (roll < 13) {
                put(l, x, y, z, s(Blocks.FERN));
            } else if (roll < 15) {
                put(l, x, y, z, s(rnd.nextBoolean() ? Blocks.RED_MUSHROOM : Blocks.BROWN_MUSHROOM));
            } else if (roll < 16) {
                put(l, x, y, z, s(Blocks.SWEET_BERRY_BUSH).setValue(
                        net.minecraft.world.level.block.SweetBerryBushBlock.AGE, 3));
            } else if (roll == 16 && dc > 16.0D) {
                // поваленное бревно
                Direction.Axis axis = rnd.nextBoolean() ? Direction.Axis.X : Direction.Axis.Z;
                BlockState log = s(Blocks.OAK_LOG).setValue(RotatedPillarBlock.AXIS, axis);
                int len = 3 + rnd.nextInt(2);
                boolean ok = true;
                for (int k = 0; k < len && ok; k++) {
                    int lx = axis == Direction.Axis.X ? x + k : x;
                    int lz = axis == Direction.Axis.Z ? z + k : z;
                    ok = free(l, lx, y, lz) && onGrass(l, lx, surface(l, lx, lz), lz) && surface(l, lx, lz) == y;
                }
                for (int k = 0; k < len && ok; k++) {
                    int lx = axis == Direction.Axis.X ? x + k : x;
                    int lz = axis == Direction.Axis.Z ? z + k : z;
                    l.setBlock(Reloc.pos(lx, y, lz), log, 2);
                }
            } else if (roll == 17 && dc > 16.0D) {
                // мшистый валун 2x2
                for (int dx = 0; dx < 2; dx++) {
                    for (int dz = 0; dz < 2; dz++) {
                        if (surface(l, x + dx, z + dz) == y) {
                            put(l, x + dx, y, z + dz, s(Blocks.MOSSY_COBBLESTONE));
                        }
                    }
                }
                put(l, x, y + 1, z, s(Blocks.MOSSY_COBBLESTONE_SLAB));
            } else if (roll == 18 && dc > 16.0D) {
                put(l, x, y, z, s(Blocks.OAK_LOG));   // пень
            }
        }
        // у края поляны боя: кострище и скамьи из брёвен
        int cx = StoryWorld.FOREST_CX;
        int cz = StoryWorld.FOREST_CZ;
        put(l, cx - 15, surface(l, cx - 15, cz + 10), cz + 10, s(Blocks.CAMPFIRE));
        put(l, cx - 17, surface(l, cx - 17, cz + 10), cz + 10, s(Blocks.OAK_LOG));
        put(l, cx - 13, surface(l, cx - 13, cz + 10), cz + 10, s(Blocks.OAK_LOG));
    }

    // ---------- Лагерь Красных, церковь, руины ----------
    private static void stagesA(ServerLevel l) {
        int cx = StoryWorld.STAGE_RED_X;
        int cz = StoryWorld.STAGE_RED_Z;
        int y = Y0 + 1;

        // лагерь: знамёна, костры, карта, припасы
        for (int[] p : new int[][] {{-9, -4}, {9, -4}, {-4, 9}, {5, 9}, {0, -13}}) {
            post(l, cx + p[0], y, cz + p[1], 3, s(Blocks.OAK_FENCE), s(Blocks.RED_BANNER));
        }
        for (int[] p : new int[][] {{-8, 7}, {8, 6}, {1, 12}}) {
            put(l, cx + p[0], y, cz + p[1], s(Blocks.CAMPFIRE));
        }
        put(l, cx + 4, y, cz - 10, s(Blocks.CARTOGRAPHY_TABLE));
        put(l, cx + 5, y, cz - 10, s(Blocks.LECTERN));
        put(l, cx - 12, y, cz + 2, s(Blocks.BARREL));
        put(l, cx - 12, y, cz + 3, s(Blocks.BARREL));
        put(l, cx - 12, y, cz + 4, s(Blocks.CHEST));
        put(l, cx - 11, y, cz + 2, s(Blocks.HAY_BLOCK));
        put(l, cx + 10, y, cz + 4, s(Blocks.ANVIL));
        put(l, cx + 11, y, cz + 4, s(Blocks.GRINDSTONE));
        for (int[] p : new int[][] {{-6, 3}, {6, 3}, {-2, -8}, {3, 7}}) {
            put(l, cx + p[0], y, cz + p[1], s(Blocks.OAK_LOG));   // сиденья вокруг костра
        }

        // церковь Широ: свечи вдоль нефов, душевые фонари у алтаря
        int px = StoryWorld.STAGE_CHAPEL_X;
        for (int z = 895; z <= 915; z += 3) {
            put(l, px - 8, y, z, candles(3));
            put(l, px + 8, y, z, candles(3));
        }
        put(l, px - 3, y, 916, s(Blocks.SOUL_LANTERN));
        put(l, px + 3, y, 916, s(Blocks.SOUL_LANTERN));
        put(l, px - 3, y, 893, candles(4));
        put(l, px + 3, y, 893, candles(4));

        // руины: догорающие очаги, груды щебня
        int rx = StoryWorld.STAGE_RUINS_X;
        int rz = StoryWorld.STAGE_RUINS_Z;
        Random rnd = new Random(55L);
        for (int i = 0; i < 18; i++) {
            double ang = rnd.nextDouble() * Math.PI * 2.0D;
            double rad = 14.0D + rnd.nextDouble() * 6.0D;
            int x = rx + (int) Math.round(rad * Math.cos(ang));
            int z = rz + (int) Math.round(rad * Math.sin(ang));
            if (free(l, x, y, z) && !free(l, x, Y0, z)) {
                if (i % 3 == 0) {
                    l.setBlock(Reloc.pos(x, Y0, z), s(Blocks.NETHERRACK), 2);
                    l.setBlock(Reloc.pos(x, y, z), s(Blocks.FIRE), 2);
                } else if (i % 3 == 1) {
                    put(l, x, y, z, s(Blocks.COBBLESTONE));
                    put(l, x + 1, y, z, s(Blocks.COBBLESTONE_SLAB));
                } else {
                    put(l, x, y, z, s(Blocks.COBBLED_DEEPSLATE));
                    put(l, x, y + 1, z, s(Blocks.COBBLESTONE_SLAB));
                }
            }
        }
    }

    // ---------- Штурм замка, улицы ----------
    private static void stagesB(ServerLevel l) {
        int cx = StoryWorld.STAGE_SIEGE_X;
        int y = Y0 + 1;
        // полевой лагерь осаждающих: мешки с песком, костры, знамёна
        for (int x = cx - 14; x <= cx - 8; x++) {
            put(l, x, y, 887, s(Blocks.HAY_BLOCK));
        }
        for (int x = cx + 8; x <= cx + 14; x++) {
            put(l, x, y, 887, s(Blocks.HAY_BLOCK));
        }
        put(l, cx - 12, y, 884, s(Blocks.CAMPFIRE));
        put(l, cx + 12, y, 884, s(Blocks.CAMPFIRE));
        put(l, cx - 16, y, 884, s(Blocks.BARREL));
        put(l, cx - 16, y, 885, s(Blocks.BARREL));
        put(l, cx + 16, y, 884, s(Blocks.CHEST));
        post(l, cx - 10, y, 892, 3, s(Blocks.OAK_FENCE), s(Blocks.WHITE_BANNER));
        post(l, cx + 10, y, 892, 3, s(Blocks.OAK_FENCE), s(Blocks.WHITE_BANNER));
        post(l, cx - 18, y, 890, 2, s(Blocks.OAK_FENCE), s(Blocks.LANTERN));
        post(l, cx + 18, y, 890, 2, s(Blocks.OAK_FENCE), s(Blocks.LANTERN));
        put(l, cx - 14, y, 893, s(Blocks.ANVIL));
        put(l, cx + 14, y, 893, s(Blocks.FLETCHING_TABLE));
    }

    // ---------- Ферма ----------
    private static void farm(ServerLevel l) {
        Random rnd = new Random(63L);
        int cx = StoryWorld.FARM_X;
        int cz = StoryWorld.FARM_Z;
        for (int i = 0; i < 260; i++) {
            int x = cx - 30 + rnd.nextInt(60);
            int z = cz - 30 + rnd.nextInt(60);
            int y = surface(l, x, z);
            if (!onGrass(l, x, y, z)) {
                continue;
            }
            put(l, x, y, z, s(FLOWERS[rnd.nextInt(FLOWERS.length)]));
        }
        int y = Y0 + 1;
        // пугало и сено
        put(l, cx - 6, y, cz - 4, s(Blocks.OAK_FENCE));
        put(l, cx - 6, y + 1, cz - 4, s(Blocks.HAY_BLOCK));
        put(l, cx - 6, y + 2, cz - 4, s(Blocks.CARVED_PUMPKIN));
        put(l, cx + 12, y, cz + 12, s(Blocks.HAY_BLOCK));
        put(l, cx + 13, y, cz + 12, s(Blocks.HAY_BLOCK));
        put(l, cx + 12, y + 1, cz + 12, s(Blocks.HAY_BLOCK));
        put(l, cx - 14, y, cz + 8, s(Blocks.PUMPKIN));
        put(l, cx - 13, y, cz + 8, s(Blocks.MELON));
        put(l, cx + 8, y, cz - 12, s(Blocks.BARREL));
        put(l, cx + 9, y, cz - 12, s(Blocks.BARREL));
        post(l, cx - 4, y, cz + 14, 2, s(Blocks.OAK_FENCE), s(Blocks.LANTERN));
        post(l, cx + 4, y, cz + 14, 2, s(Blocks.OAK_FENCE), s(Blocks.LANTERN));
    }

    // ---------- Висячие сады: цветники, фонтаны, фонарные столбы ----------
    private static void gardens(ServerLevel l) {
        int cx = StoryWorld.GARDEN_X;
        int cz = StoryWorld.GARDEN_Z;
        int y = StoryWorld.GARDEN_TOP + 1;
        Random rnd = new Random(81L);
        Block[] bloom = {Blocks.ALLIUM, Blocks.RED_TULIP, Blocks.ORANGE_TULIP, Blocks.PINK_TULIP,
                Blocks.AZURE_BLUET, Blocks.OXEYE_DAISY, Blocks.CORNFLOWER, Blocks.FLOWERING_AZALEA};
        for (int i = 0; i < 700; i++) {
            double ang = rnd.nextDouble() * Math.PI * 2.0D;
            double rad = 11.0D + rnd.nextDouble() * 22.0D;
            int x = cx + (int) Math.round(rad * Math.cos(ang));
            int z = cz + (int) Math.round(rad * Math.sin(ang));
            if (Math.abs(x - cx) <= 2 || Math.abs(z - cz) <= 2) {
                continue;   // аллеи свободны
            }
            put(l, x, y, z, s(bloom[rnd.nextInt(bloom.length)]));
        }
        // фонарные столбы вдоль аллей
        for (int d : new int[] {13, 22, 31}) {
            for (int sign : new int[] {-1, 1}) {
                post(l, cx + 3, y, cz + sign * d, 3, s(Blocks.QUARTZ_PILLAR), s(Blocks.SEA_LANTERN));
                post(l, cx - 3, y, cz + sign * d, 3, s(Blocks.QUARTZ_PILLAR), s(Blocks.SEA_LANTERN));
                post(l, cx + sign * d, y, cz + 3, 3, s(Blocks.QUARTZ_PILLAR), s(Blocks.SEA_LANTERN));
                post(l, cx + sign * d, y, cz - 3, 3, s(Blocks.QUARTZ_PILLAR), s(Blocks.SEA_LANTERN));
            }
        }
        // четыре фонтана на диагоналях
        for (int sx : new int[] {-1, 1}) {
            for (int sz : new int[] {-1, 1}) {
                int fx = cx + sx * 16;
                int fz = cz + sz * 16;
                boolean ok = true;
                for (int dx = -2; dx <= 2 && ok; dx++) {
                    for (int dz = -2; dz <= 2 && ok; dz++) {
                        ok = free(l, fx + dx, y, fz + dz) && !free(l, fx + dx, y - 1, fz + dz);
                    }
                }
                if (!ok) {
                    continue;
                }
                for (int dx = -2; dx <= 2; dx++) {
                    for (int dz = -2; dz <= 2; dz++) {
                        boolean rim = Math.abs(dx) == 2 || Math.abs(dz) == 2;
                        l.setBlock(Reloc.pos(fx + dx, y, fz + dz),
                                rim ? s(Blocks.SMOOTH_QUARTZ) : s(Blocks.WATER), 2);
                    }
                }
                l.setBlock(Reloc.pos(fx, y, fz), s(Blocks.QUARTZ_PILLAR), 2);
                l.setBlock(Reloc.pos(fx, y + 1, fz), s(Blocks.QUARTZ_PILLAR), 2);
                l.setBlock(Reloc.pos(fx, y + 2, fz), s(Blocks.SEA_LANTERN), 2);
            }
        }
    }
}
