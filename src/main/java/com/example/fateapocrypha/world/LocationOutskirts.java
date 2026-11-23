package com.example.fateapocrypha.world;

import com.example.fateapocrypha.FateApocrypha;
import com.example.fateapocrypha.ModDimensions;
import com.example.fateapocrypha.story.StoryLocation;
import java.util.HashSet;
import java.util.Random;
import java.util.Set;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LeavesBlock;
import net.minecraft.world.level.block.RotatedPillarBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.core.Direction;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * Окрестности локаций. Площадка локации плоская, и вокруг неё было пусто: теперь у каждой локации (кроме замка, у
 * него свой холм) по кольцу от края круга наружу расставляются деревья, кусты, валуны, цветочные поляны, пни, поваленные
 * брёвна и стога. К краю круга деревьев больше, наружу они редеют и сливаются с естественным лесом. Всё считается по
 * зерну локации, поэтому повторная расстановка ничего не дублирует (занятые места пропускаются).
 */
@Mod.EventBusSubscriber(modid = FateApocrypha.MODID)
public final class LocationOutskirts {
    private static final Set<Long> DONE = new HashSet<>();
    private static final int OBJECTS = 110;

    private LocationOutskirts() {
    }

    @SubscribeEvent
    public static void onLevelTick(TickEvent.LevelTickEvent event) {
        if (event.phase != TickEvent.Phase.END || !(event.level instanceof ServerLevel level)
                || !level.dimension().equals(ModDimensions.TRIFAS) || level.getGameTime() % 100L != 17L) {
            return;
        }
        for (ServerPlayer p : level.players()) {
            for (StoryLocation loc : StoryLocation.values()) {
                if (loc != loc.primary() || loc == StoryLocation.CASTLE_LAB || loc == StoryLocation.CASTLE_GATE) {
                    continue;
                }
                double d = Math.hypot(p.getX() - loc.cx(), p.getZ() - loc.cz());
                if (d < loc.radius() + 150.0D) {
                    decorate(level, loc);
                }
            }
        }
    }

    private static void decorate(ServerLevel level, StoryLocation loc) {
        BuildKit kit = new BuildKit(level);
        Random rnd = new Random(loc.ordinal() * 7919L + 13L);
        for (int i = 0; i < OBJECTS; i++) {
            double ang = rnd.nextDouble() * Math.PI * 2.0D;
            double dist = loc.radius() + 4.0D + Math.pow(rnd.nextDouble(), 1.4D) * 56.0D;
            int roll = rnd.nextInt(100);
            long seed = rnd.nextLong();
            int x = (int) Math.round(loc.cx() + Math.cos(ang) * dist);
            int z = (int) Math.round(loc.cz() + Math.sin(ang) * dist);
            long key = (long) loc.ordinal() * 1000L + i;
            if (DONE.contains(key)) {
                continue;
            }
            if (!level.hasChunk((x - 4) >> 4, (z - 4) >> 4) || !level.hasChunk((x + 4) >> 4, (z + 4) >> 4)) {
                continue;   // чанк не загружен: попробуем позже
            }
            DONE.add(key);
            int y = BuildKit.ground(x, z);
            if (y <= TrifasTerrain.SEA + 1 || TrifasRoads.roadDist(x, z) < 6.0D
                    || Math.abs(y - BuildKit.ground(x + 3, z)) > 3 || Math.abs(y - BuildKit.ground(x, z + 3)) > 3) {
                continue;
            }
            if (!level.getBlockState(new BlockPos(x, y, z)).is(Blocks.GRASS_BLOCK) || !kit.isAir(x, y + 1, z)) {
                continue;
            }
            Random r = new Random(seed);
            if (roll < 24) {
                tree(kit, r, x, y, z, Blocks.OAK_LOG, Blocks.OAK_LEAVES, 4 + r.nextInt(3), 2.5D);
            } else if (roll < 36) {
                tree(kit, r, x, y, z, Blocks.BIRCH_LOG, Blocks.BIRCH_LEAVES, 5 + r.nextInt(3), 2.1D);
            } else if (roll < 52) {
                spruce(kit, r, x, y, z);
            } else if (roll < 64) {
                bush(kit, r, x, y, z);
            } else if (roll < 74) {
                boulders(kit, r, x, y, z);
            } else if (roll < 90) {
                flowers(kit, r, x, y, z);
            } else if (roll < 94) {
                fallenLog(kit, r, x, y, z);
            } else if (roll < 97) {
                kit.set(x, y + 1, z, BuildKit.s(Blocks.OAK_LOG));
                kit.set(x, y + 2, z, BuildKit.s(Blocks.MOSS_CARPET));
            } else {
                kit.set(x, y + 1, z, BuildKit.s(Blocks.HAY_BLOCK));
                if (r.nextBoolean()) {
                    kit.set(x + 1, y + 1, z, BuildKit.s(Blocks.HAY_BLOCK));
                }
            }
        }
    }

    private static BlockState leaf(Block leaves) {
        return leaves.defaultBlockState().setValue(LeavesBlock.PERSISTENT, true);
    }

    /** Эллипсоид из листвы вокруг центра. */
    private static void blob(BuildKit kit, Random r, int cx, int cy, int cz, double rx, double ry, Block leaves) {
        int ir = (int) Math.ceil(rx);
        int iy = (int) Math.ceil(ry);
        for (int dx = -ir; dx <= ir; dx++) {
            for (int dz = -ir; dz <= ir; dz++) {
                for (int dy = -iy; dy <= iy; dy++) {
                    double d = (dx * dx + dz * dz) / (rx * rx) + (dy * dy) / (ry * ry);
                    if (d <= 1.0D && (d < 0.7D || r.nextInt(3) != 0) && kit.isAir(cx + dx, cy + dy, cz + dz)) {
                        kit.set(cx + dx, cy + dy, cz + dz, leaf(leaves));
                    }
                }
            }
        }
    }

    private static void tree(BuildKit kit, Random r, int x, int y, int z, Block log, Block leaves, int h, double radius) {
        for (int i = 1; i <= h; i++) {
            kit.set(x, y + i, z, BuildKit.s(log));
        }
        blob(kit, r, x, y + h, z, radius, radius * 0.8D, leaves);
        blob(kit, r, x, y + h + 2, z, radius * 0.5D, 1.0D, leaves);
        if (radius > 2.3D && r.nextBoolean()) {   // боковая ветка
            Direction[] sides = {Direction.NORTH, Direction.SOUTH, Direction.EAST, Direction.WEST};
            Direction d = sides[r.nextInt(4)];
            kit.set(x + d.getStepX(), y + h - 1, z + d.getStepZ(),
                    BuildKit.s(log).setValue(RotatedPillarBlock.AXIS, d.getAxis()));
        }
    }

    private static void spruce(BuildKit kit, Random r, int x, int y, int z) {
        int h = 6 + r.nextInt(4);
        for (int i = 1; i <= h; i++) {
            kit.set(x, y + i, z, BuildKit.s(Blocks.SPRUCE_LOG));
        }
        for (int k = 0; k < h - 1; k++) {
            int layerY = y + 2 + k;
            int rad = Math.max(0, (int) Math.round((h - 1 - k) * 2.6D / (h - 1)));
            for (int dx = -rad; dx <= rad; dx++) {
                for (int dz = -rad; dz <= rad; dz++) {
                    if (dx * dx + dz * dz <= rad * rad + 1 && (dx != 0 || dz != 0) && kit.isAir(x + dx, layerY, z + dz)) {
                        kit.set(x + dx, layerY, z + dz, leaf(Blocks.SPRUCE_LEAVES));
                    }
                }
            }
        }
        kit.set(x, y + h + 1, z, leaf(Blocks.SPRUCE_LEAVES));
    }

    private static void bush(BuildKit kit, Random r, int x, int y, int z) {
        blob(kit, r, x, y + 1, z, 1.6D, 1.1D, Blocks.OAK_LEAVES);
        if (r.nextInt(3) == 0) {
            kit.set(x, y + 1, z, leaf(Blocks.FLOWERING_AZALEA_LEAVES));
        }
    }

    private static void boulders(BuildKit kit, Random r, int x, int y, int z) {
        Block[] kinds = {Blocks.COBBLESTONE, Blocks.MOSSY_COBBLESTONE, Blocks.ANDESITE, Blocks.STONE};
        int n = 3 + r.nextInt(4);
        for (int i = 0; i < n; i++) {
            int dx = r.nextInt(3) - 1;
            int dz = r.nextInt(3) - 1;
            int gy = BuildKit.ground(x + dx, z + dz);
            kit.set(x + dx, gy + 1, z + dz, BuildKit.s(kinds[r.nextInt(kinds.length)]));
            if (r.nextInt(3) == 0) {
                kit.set(x + dx, gy + 2, z + dz, BuildKit.s(kinds[r.nextInt(kinds.length)]));
            }
        }
    }

    private static void flowers(BuildKit kit, Random r, int x, int y, int z) {
        Block[] kinds = {Blocks.DANDELION, Blocks.POPPY, Blocks.AZURE_BLUET, Blocks.CORNFLOWER, Blocks.OXEYE_DAISY,
                Blocks.ALLIUM, Blocks.GRASS, Blocks.GRASS, Blocks.FERN};
        int n = 8 + r.nextInt(8);
        for (int i = 0; i < n; i++) {
            int dx = r.nextInt(7) - 3;
            int dz = r.nextInt(7) - 3;
            int gy = BuildKit.ground(x + dx, z + dz);
            if (kit.isAir(x + dx, gy + 1, z + dz)
                    && kit.level.getBlockState(new BlockPos(x + dx, gy, z + dz)).is(Blocks.GRASS_BLOCK)) {
                kit.set(x + dx, gy + 1, z + dz, BuildKit.s(kinds[r.nextInt(kinds.length)]));
            }
        }
    }

    private static void fallenLog(BuildKit kit, Random r, int x, int y, int z) {
        Direction.Axis axis = r.nextBoolean() ? Direction.Axis.X : Direction.Axis.Z;
        int len = 3 + r.nextInt(2);
        for (int i = 0; i < len; i++) {
            int lx = x + (axis == Direction.Axis.X ? i : 0);
            int lz = z + (axis == Direction.Axis.Z ? i : 0);
            int gy = BuildKit.ground(lx, lz);
            if (Math.abs(gy - y) <= 1 && kit.isAir(lx, gy + 1, lz)) {
                kit.set(lx, gy + 1, lz, BuildKit.s(Blocks.OAK_LOG).setValue(RotatedPillarBlock.AXIS, axis));
            }
        }
    }
}
