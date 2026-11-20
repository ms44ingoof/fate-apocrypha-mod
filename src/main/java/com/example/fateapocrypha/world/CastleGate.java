package com.example.fateapocrypha.world;

import com.example.fateapocrypha.FateApocrypha;
import com.example.fateapocrypha.ModDimensions;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.entity.npc.VillagerProfession;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * Главные ворота замка. Створки (дубовые, окованные железом) стоят в проезде привратной башни; привратники у ворот
 * открывают и закрывают их по просьбе игрока. Створки расходятся по колонкам от центра к краям (и сходятся обратно),
 * со скрипом и пылью. Состояние не хранится отдельно: ворота открыты, если центральной балки створок нет в мире.
 */
@Mod.EventBusSubscriber(modid = FateApocrypha.MODID)
public final class CastleGate {
    public static final String GUARD_TAG = "fa_gate_guard";
    public static final int STATE_CLOSED = 0;
    public static final int STATE_OPEN = 1;
    public static final int STATE_BUSY = 2;

    public enum Result { OK, BUSY, BLOCKED, ALREADY }

    private static final int Y = CastleSite.Y;
    private static final int CX = CastleSite.CX;
    private static final int ZA = CastleSite.Z0 + CastleSite.D - 8;   // северная грань привратной башни (створки в z ZA и ZA+1)
    private static final int ZB = ZA + 1;

    private static final int[][] OPEN_STEPS = {{0}, {-1, 1}, {-2, 2}};
    private static final int[][] CLOSE_STEPS = {{-2, 2}, {-1, 1}, {0}};

    private static final List<Task> TASKS = new ArrayList<>();

    private static final class Task {
        final ServerLevel level;
        final boolean closing;
        int index = 0;
        int wait = 2;

        Task(ServerLevel level, boolean closing) {
            this.level = level;
            this.closing = closing;
        }
    }

    private CastleGate() {
    }

    /** Центр ворот (для проверки расстояния). */
    public static BlockPos center() {
        return new BlockPos(CX, Y + 2, ZA);
    }

    public static boolean busy() {
        return !TASKS.isEmpty();
    }

    public static boolean isOpen(ServerLevel level) {
        return !level.getBlockState(new BlockPos(CX, Y + 2, ZA)).is(Blocks.DARK_OAK_LOG);
    }

    public static int state(ServerLevel level) {
        return busy() ? STATE_BUSY : isOpen(level) ? STATE_OPEN : STATE_CLOSED;
    }

    /** Привратник: тегнутый житель или стоящий на посту у ворот (для замков, построенных до появления тега). */
    public static boolean isGuard(Entity e) {
        if (!(e instanceof Villager v)) {
            return false;
        }
        if (v.getTags().contains(GUARD_TAG)) {
            return true;
        }
        return v.getVillagerData().getProfession() == VillagerProfession.WEAPONSMITH && v.isNoAi()
                && Math.abs(v.getY() - (Y + 1)) < 3.0D && v.getZ() > ZA - 6.0D && v.getZ() < ZA
                && (Math.abs(v.getX() - (CX - 5.5D)) < 2.5D || Math.abs(v.getX() - (CX + 5.5D)) < 2.5D);
    }

    /** Открыть или закрыть ворота. Закрыть нельзя, пока в проезде кто-то стоит. */
    public static Result toggle(ServerLevel level, boolean open) {
        if (busy()) {
            return Result.BUSY;
        }
        if (open == isOpen(level)) {
            return Result.ALREADY;
        }
        if (!open) {
            AABB passage = new AABB(CX - 3, Y + 1, ZA - 1, CX + 4, Y + 10, ZB + 2);
            if (!level.getEntitiesOfClass(LivingEntity.class, passage).isEmpty()) {
                return Result.BLOCKED;
            }
        }
        TASKS.add(new Task(level, !open));
        return Result.OK;
    }

    /** Блок створки в колонке dx на высоте y и глубине (front: северная грань z=ZA). */
    private static BlockState leafBlock(int dx, int y, boolean front) {
        if (front) {
            if (dx == 0) {
                return Blocks.DARK_OAK_LOG.defaultBlockState();                                  // стык створок
            }
            if (Math.abs(dx) == 1 && y == Y + 8) {
                return Blocks.IRON_BARS.defaultBlockState();                                      // смотровые щели
            }
            if (Math.abs(dx) == 2 && (y == Y + 3 || y == Y + 7)) {
                return Blocks.IRON_BLOCK.defaultBlockState();                                     // оковка
            }
            if (y == Y + 5) {
                return Blocks.DARK_OAK_LOG.defaultBlockState();                                   // поперечная балка
            }
        }
        return Blocks.DARK_OAK_PLANKS.defaultBlockState();
    }

    private static void column(ServerLevel level, int dx, boolean closing) {
        for (int y = Y + 1; y <= Y + 9; y++) {
            for (int z = ZA; z <= ZB; z++) {
                BlockState st = closing ? leafBlock(dx, y, z == ZA) : Blocks.AIR.defaultBlockState();
                level.setBlock(new BlockPos(CX + dx, y, z), st, 2);
            }
        }
    }

    @SubscribeEvent
    public static void onLevelTick(TickEvent.LevelTickEvent event) {
        if (event.phase != TickEvent.Phase.END || !(event.level instanceof ServerLevel level)
                || !level.dimension().equals(ModDimensions.TRIFAS) || TASKS.isEmpty()) {
            return;
        }
        for (int i = TASKS.size() - 1; i >= 0; i--) {
            Task t = TASKS.get(i);
            if (t.level != level) {
                continue;
            }
            if (--t.wait > 0) {
                continue;
            }
            int[][] steps = t.closing ? CLOSE_STEPS : OPEN_STEPS;
            for (int dx : steps[t.index]) {
                column(level, dx, t.closing);
                level.sendParticles(ParticleTypes.POOF, CX + dx + 0.5D, Y + 2.0D, ZA + 0.5D, 6, 0.3D, 1.2D, 0.6D, 0.01D);
            }
            BlockPos at = new BlockPos(CX, Y + 3, ZA);
            level.playSound(null, at, t.closing ? SoundEvents.WOODEN_DOOR_CLOSE : SoundEvents.WOODEN_DOOR_OPEN,
                    SoundSource.BLOCKS, 1.6F, 0.55F + 0.1F * t.index);
            t.index++;
            t.wait = 5;
            if (t.index >= steps.length) {
                level.playSound(null, at, t.closing ? SoundEvents.IRON_DOOR_CLOSE : SoundEvents.CHAIN_BREAK,
                        SoundSource.BLOCKS, 1.4F, 0.6F);
                TASKS.remove(i);
            }
        }
    }
}
