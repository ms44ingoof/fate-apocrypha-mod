package com.example.fateapocrypha.fx;

import com.example.fateapocrypha.FateApocrypha;
import com.example.fateapocrypha.ModDimensions;
import com.example.fateapocrypha.story.StoryLocation;
import com.example.fateapocrypha.world.CastleSite;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * Живой мир: атмосферные частицы по местам Трифаса. Лепестки и золотые искры в Висячих садах, парящие знаки в часовне,
 * пепел и бледные огни на руинах, искры у костра Красных, светлячки в лесу по ночам, фиолетовые мотыльки на равнине
 * Голема, пыльца на ферме и брызги у фонтана замка. Частицы появляются рядом с игроком, поэтому почти ничего не стоят.
 */
@Mod.EventBusSubscriber(modid = FateApocrypha.MODID)
public final class AmbientFx {
    private AmbientFx() {
    }

    @SubscribeEvent
    public static void onLevelTick(TickEvent.LevelTickEvent event) {
        if (event.phase != TickEvent.Phase.END || !(event.level instanceof ServerLevel level)
                || !level.dimension().equals(ModDimensions.TRIFAS) || level.getGameTime() % 12L != 0L) {
            return;
        }
        RandomSource rnd = level.random;
        for (ServerPlayer p : level.players()) {
            fountain(level, p, rnd);
            StoryLocation loc = StoryLocation.at(p.getX(), p.getZ());
            if (loc == null) {
                continue;
            }
            double px = p.getX() + (rnd.nextDouble() - 0.5D) * 24.0D;
            double py = p.getY() + rnd.nextDouble() * 5.0D - 1.0D;
            double pz = p.getZ() + (rnd.nextDouble() - 0.5D) * 24.0D;
            switch (loc.primary()) {
                case GARDENS -> {
                    level.sendParticles(ParticleTypes.CHERRY_LEAVES, px, p.getY() + 6.0D, pz, 3, 2.0D, 0.5D, 2.0D, 0.02D);
                    level.sendParticles(ParticleTypes.END_ROD, px, py, pz, 1, 0.2D, 0.2D, 0.2D, 0.01D);
                }
                case CHAPEL -> {
                    level.sendParticles(ParticleTypes.ENCHANT, p.getX() + (rnd.nextDouble() - 0.5D) * 8.0D,
                            p.getY() + 2.5D, p.getZ() + (rnd.nextDouble() - 0.5D) * 8.0D, 4, 0.5D, 0.5D, 0.5D, 0.4D);
                    if (rnd.nextInt(3) == 0) {
                        level.sendParticles(ParticleTypes.END_ROD, px, py, pz, 1, 0.1D, 0.1D, 0.1D, 0.005D);
                    }
                }
                case RUINS -> {
                    level.sendParticles(ParticleTypes.ASH, px, py + 1.0D, pz, 5, 1.5D, 0.8D, 1.5D, 0.0D);
                    if (rnd.nextInt(3) == 0) {
                        level.sendParticles(ParticleTypes.SOUL_FIRE_FLAME, px, p.getY() + 0.3D, pz, 1, 0.1D, 0.2D, 0.1D, 0.01D);
                    }
                }
                case RED_CAMP -> {
                    double cx = loc.cx() + 0.5D;
                    double cz = loc.cz() + 0.5D;
                    if (Math.hypot(p.getX() - cx, p.getZ() - cz) < 24.0D) {
                        level.sendParticles(ParticleTypes.LAVA, cx, p.getY() + 0.8D, cz, 2, 0.4D, 0.3D, 0.4D, 0.0D);
                        level.sendParticles(ParticleTypes.ASH, cx, p.getY() + 1.5D, cz, 3, 0.6D, 0.6D, 0.6D, 0.0D);
                    }
                }
                case FOREST -> {
                    if (level.isNight()) {
                        level.sendParticles(ParticleTypes.GLOW, px, py, pz, 2, 0.4D, 0.3D, 0.4D, 0.01D);
                    }
                }
                case PLAIN -> {
                    level.sendParticles(ParticleTypes.REVERSE_PORTAL, px, py, pz, 4, 0.5D, 0.6D, 0.5D, 0.05D);
                    if (rnd.nextInt(4) == 0) {
                        level.sendParticles(ParticleTypes.SOUL, px, p.getY() + 0.5D, pz, 1, 0.1D, 0.2D, 0.1D, 0.02D);
                    }
                }
                case FARM -> {
                    if (!level.isNight()) {
                        level.sendParticles(ParticleTypes.COMPOSTER, px, p.getY() + 0.6D, pz, 2, 1.0D, 0.4D, 1.0D, 0.0D);
                    }
                }
                default -> { }
            }
        }
    }

    /** Брызги над фонтаном замка. */
    private static void fountain(ServerLevel level, ServerPlayer p, RandomSource rnd) {
        double fx = CastleSite.CX + 0.5D;
        double fz = 272.5D;
        if (Math.hypot(p.getX() - fx, p.getZ() - fz) > 60.0D) {
            return;
        }
        double y = CastleSite.Y + 3.2D;
        level.sendParticles(ParticleTypes.SPLASH, fx, y, fz, 8, 0.35D, 0.15D, 0.35D, 0.2D);
        level.sendParticles(ParticleTypes.FALLING_WATER, fx + (rnd.nextDouble() - 0.5D) * 3.0D, y + 1.0D,
                fz + (rnd.nextDouble() - 0.5D) * 3.0D, 3, 0.2D, 0.0D, 0.2D, 0.0D);
    }
}
