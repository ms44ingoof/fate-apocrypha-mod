package com.example.fateapocrypha.world;

import com.example.fateapocrypha.FateApocrypha;
import com.example.fateapocrypha.ModDimensions;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CampfireBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.living.LivingEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * Жизнь замка: патрули стражи на стенах и слуги во дворе (ходят по маршрутам), колокол на крыше (звонит на рассвете,
 * в полдень, на закате и в начале сцен в замке) и жаровни, которые зажигаются ночью и гаснут утром.
 * Маршруты задаются тегом "fa_patrol:id" на существе; состояние хранится в его данных и переживает перезагрузку.
 */
@Mod.EventBusSubscriber(modid = FateApocrypha.MODID)
public final class CastleLife {
    public static final String ROUTE_PREFIX = "fa_patrol:";
    private static final double SPEED = 0.085D;        // блоков за тик, примерно шаг жителя

    /** Колокол на колокольне над западным крылом. */
    public static final BlockPos BELL = new BlockPos(178, CastleSite.Y + 27, 200);

    /** Жаровни (x, z): у ворот, у входа в корпус, у фонтана и на дорожке. Основание на Y+1, костёр на Y+2. */
    public static final int[][] BRAZIERS = {
            {CastleSite.CX - 12, 338}, {CastleSite.CX + 12, 338},
            {CastleSite.CX - 16, 254}, {CastleSite.CX + 16, 254},
            {CastleSite.CX - 18, 272}, {CastleSite.CX + 18, 272},
            {145, 300}, {285, 300}
    };

    private static final double WALK = CastleSite.Y + 14;      // ход по стене
    private static final double GROUND = CastleSite.Y + 1;

    private static int pendingRings = 0;

    private CastleLife() {
    }

    /** Точки маршрута. Стенные идут туда и обратно, "plaza" замкнутый круг у фонтана. */
    private static double[][] route(String id) {
        int x0 = CastleSite.X0;
        int z0 = CastleSite.Z0;
        int x1 = x0 + CastleSite.W;
        int z1 = z0 + CastleSite.D;
        switch (id) {
            case "wall_w":
                return new double[][] {{x0 + 2.0D, WALK, z0 + 12.0D}, {x0 + 2.0D, WALK, z1 - 12.0D}};
            case "wall_n":
                return new double[][] {{x0 + 12.0D, WALK, z0 + 2.0D}, {x1 - 12.0D, WALK, z0 + 2.0D}};
            case "wall_e":
                return new double[][] {{x1 - 1.0D, WALK, z0 + 12.0D}, {x1 - 1.0D, WALK, z1 - 12.0D}};
            case "road":
                return new double[][] {{x0 + 10.0D, GROUND, 298.5D}, {x1 - 10.0D, GROUND, 298.5D}};
            case "plaza": {
                double[][] pts = new double[12][];
                for (int i = 0; i < 12; i++) {
                    double a = i * Math.PI / 6.0D;
                    pts[i] = new double[] {CastleSite.CX + 0.5D + 17.0D * Math.cos(a), GROUND, 272.5D + 17.0D * Math.sin(a)};
                }
                return pts;
            }
            default:
                return null;
        }
    }

    // ================= патрули =================
    @SubscribeEvent
    public static void onLivingTick(LivingEvent.LivingTickEvent event) {
        LivingEntity e = event.getEntity();
        if (e.level().isClientSide || e.getTags().isEmpty()) {
            return;
        }
        String id = null;
        for (String tag : e.getTags()) {
            if (tag.startsWith(ROUTE_PREFIX)) {
                id = tag.substring(ROUTE_PREFIX.length());
                break;
            }
        }
        if (id == null) {
            return;
        }
        double[][] pts = route(id);
        if (pts == null) {
            return;
        }
        boolean loop = id.equals("plaza");
        CompoundTag data = e.getPersistentData();
        if (!data.contains("fa_idx")) {
            int best = 0;
            double bestD = Double.MAX_VALUE;
            for (int i = 0; i < pts.length; i++) {
                double d = Math.hypot(pts[i][0] - e.getX(), pts[i][2] - e.getZ());
                if (d < bestD) {
                    bestD = d;
                    best = i;
                }
            }
            data.putInt("fa_idx", best);
            data.putInt("fa_dir", 1);
            data.putInt("fa_wait", 0);
        }
        int wait = data.getInt("fa_wait");
        if (wait > 0) {
            data.putInt("fa_wait", wait - 1);
            return;
        }
        int idx = data.getInt("fa_idx");
        int dir = data.getInt("fa_dir");
        double[] target = pts[idx];
        double dx = target[0] - e.getX();
        double dz = target[2] - e.getZ();
        double dist = Math.hypot(dx, dz);
        if (dist <= SPEED) {
            e.setPos(target[0], target[1], target[2]);
            if (loop) {
                idx = (idx + 1) % pts.length;
            } else {
                if (idx + dir < 0 || idx + dir >= pts.length) {
                    dir = -dir;
                }
                idx += dir;
                data.putInt("fa_dir", dir);
                data.putInt("fa_wait", 50 + e.getRandom().nextInt(100));   // постоял, осмотрелся
            }
            data.putInt("fa_idx", idx);
            return;
        }
        e.setPos(e.getX() + dx / dist * SPEED, target[1], e.getZ() + dz / dist * SPEED);
        float yaw = (float) Math.toDegrees(Math.atan2(-dx, dz));
        e.setYRot(yaw);
        e.yBodyRot = yaw;
        e.setYHeadRot(yaw);
    }

    // ================= колокол и жаровни =================
    /** Позвонить в колокол несколько раз подряд (например, в начале сцены в замке). */
    public static void ringBell(ServerLevel level, int times) {
        pendingRings = Math.max(pendingRings, times);
    }

    @SubscribeEvent
    public static void onLevelTick(TickEvent.LevelTickEvent event) {
        if (event.phase != TickEvent.Phase.END || !(event.level instanceof ServerLevel level)) {
            return;
        }
        if (!level.dimension().equals(ModDimensions.TRIFAS)) {
            return;
        }
        long game = level.getGameTime();
        long t = level.getDayTime() % 24000L;
        if (game % 4L == 0L) {
            ambient(level, game, t);
        }
        if (game % 20L != 0L || !playerNear(level)) {
            return;
        }
        boolean slot = t < 60L || (t >= 6000L && t < 6060L) || (t >= 12000L && t < 12060L);   // три удара подряд
        if (pendingRings > 0 || slot) {
            ring(level);
            if (pendingRings > 0) {
                pendingRings--;
            }
        }
        if (game % 100L == 0L) {
            boolean lit = t >= 12500L && t < 23500L;
            for (int[] b : BRAZIERS) {
                BlockPos pos = new BlockPos(b[0], CastleSite.Y + 2, b[1]);
                if (!level.isLoaded(pos)) {
                    continue;
                }
                BlockState state = level.getBlockState(pos);
                if (state.is(Blocks.CAMPFIRE) && state.getValue(CampfireBlock.LIT) != lit) {
                    level.setBlock(pos, state.setValue(CampfireBlock.LIT, lit), 3);
                }
            }
        }
    }

    private static boolean playerNear(ServerLevel level) {
        for (ServerPlayer p : level.players()) {
            if (p.distanceToSqr(BELL.getX(), BELL.getY(), BELL.getZ()) < 220.0D * 220.0D) {
                return true;
            }
        }
        return false;
    }

    private static void ring(ServerLevel level) {
        if (!level.isLoaded(BELL) || !level.getBlockState(BELL).is(Blocks.BELL)) {
            return;
        }
        level.blockEvent(BELL, Blocks.BELL, 1, Direction.NORTH.get3DDataValue());
        level.playSound(null, BELL, SoundEvents.BELL_BLOCK, SoundSource.BLOCKS, 4.0F, 1.0F);
        // ноты разлетаются от колокола
        level.sendParticles(ParticleTypes.NOTE, BELL.getX() + 0.5D, BELL.getY() + 0.6D, BELL.getZ() + 0.5D,
                5, 1.2D, 0.6D, 1.2D, 1.0D);
    }

    // ================= живые частицы замка =================
    /** Раз в 4 тика: пылинки в зале, золотые искры в тронном зале, лепестки и светлячки во дворе, брызги фонтана. */
    private static void ambient(ServerLevel level, long game, long t) {
        boolean someone = false;
        for (ServerPlayer p : level.players()) {
            if (Math.abs(p.getX() - CastleSite.CX) < 120.0D && Math.abs(p.getZ() - CastleSite.CZ) < 140.0D) {
                someone = true;
                break;
            }
        }
        if (!someone) {
            return;
        }
        RandomSource r = level.getRandom();
        int y = CastleSite.Y;
        boolean night = t >= 13000L && t < 23000L;
        // большой зал: пылинки в воздухе
        for (int i = 0; i < 2; i++) {
            level.sendParticles(ParticleTypes.END_ROD, 190.0D + r.nextDouble() * 52.0D, y + 3.0D + r.nextDouble() * 12.0D,
                    183.0D + r.nextDouble() * 48.0D, 1, 0.0D, 0.0D, 0.0D, 0.004D);
        }
        // тронный зал: золотистые искры под сводом
        if (game % 8L == 0L) {
            level.sendParticles(ParticleTypes.GLOW, 194.0D + r.nextDouble() * 42.0D, y + 26.0D + r.nextDouble() * 10.0D,
                    185.0D + r.nextDouble() * 20.0D, 1, 0.0D, 0.0D, 0.0D, 0.01D);
        }
        // двор: лепестки падают с неба
        if (game % 8L == 0L) {
            level.sendParticles(ParticleTypes.CHERRY_LEAVES, 140.0D + r.nextDouble() * 150.0D, y + 9.0D + r.nextDouble() * 3.0D,
                    255.0D + r.nextDouble() * 85.0D, 1, 0.0D, 0.0D, 0.0D, 0.0D);
        }
        // ночью над газонами и огородом светлячки
        if (night) {
            for (int i = 0; i < 2; i++) {
                level.sendParticles(ParticleTypes.GLOW, 140.0D + r.nextDouble() * 150.0D, y + 1.2D + r.nextDouble() * 2.5D,
                        255.0D + r.nextDouble() * 85.0D, 1, 0.0D, 0.0D, 0.0D, 0.004D);
            }
        }
        // фонтан: брызги над водой и искры у колонны
        double fx = CastleSite.CX + 0.5D;
        level.sendParticles(ParticleTypes.SPLASH, fx, y + 1.2D, 272.5D, 4, 2.4D, 0.0D, 2.4D, 0.1D);
        if (game % 8L == 0L) {
            level.sendParticles(ParticleTypes.END_ROD, fx + (r.nextDouble() - 0.5D) * 1.6D, y + 2.0D + r.nextDouble() * 4.0D,
                    272.5D + (r.nextDouble() - 0.5D) * 1.6D, 1, 0.0D, 0.0D, 0.0D, 0.01D);
        }
    }
}
