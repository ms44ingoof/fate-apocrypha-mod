package com.example.fateapocrypha.client.fx;

import com.example.fateapocrypha.FateApocrypha;
import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.BufferUploader;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexFormat;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderLevelStageEvent;
import net.minecraftforge.client.event.ViewportEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.joml.Matrix4f;

/**
 * Собственные боевые и сюжетные эффекты, нарисованные вручную (без частиц и текстур): магический круг с рунами и
 * вращающейся гексаграммой, расходящееся кольцо, световой столб, луч, дуга взмаха и лучистый всплеск (всё с
 * аддитивным свечением), а также тряска камеры. Эффекты приходят пакетом FxPacket и живут заданное число тиков.
 */
@Mod.EventBusSubscriber(modid = FateApocrypha.MODID, value = Dist.CLIENT)
public final class ClientFx {
    private static final int CIRCLE = 0;
    private static final int RING = 1;
    private static final int PILLAR = 2;
    private static final int BEAM = 3;
    private static final int SLASH = 4;
    private static final int BURST = 5;
    private static final int HELIX = 6;
    private static final int RAIN = 7;
    private static final int SUN = 8;
    private static final int WINGS = 9;
    private static final int LIGHTNING = 10;

    private static final class Fx {
        final int kind;
        final double x;
        final double y;
        final double z;
        final double ax;
        final double ay;
        final double az;
        final float r;
        final float g;
        final float b;
        final float size;
        final int ticks;
        final int start;

        Fx(int kind, double x, double y, double z, double ax, double ay, double az, int color, float size, int ticks, int start) {
            this.kind = kind;
            this.x = x;
            this.y = y;
            this.z = z;
            this.ax = ax;
            this.ay = ay;
            this.az = az;
            this.r = ((color >> 16) & 0xFF) / 255.0F;
            this.g = ((color >> 8) & 0xFF) / 255.0F;
            this.b = (color & 0xFF) / 255.0F;
            this.size = size;
            this.ticks = Math.max(2, ticks);
            this.start = start;
        }
    }

    private static final List<Fx> ACTIVE = new ArrayList<>();
    private static int now = 0;
    private static float shakePower = 0.0F;
    private static int shakeLeft = 0;
    private static int shakeTotal = 1;

    private ClientFx() {
    }

    public static void add(int kind, double x, double y, double z, double ax, double ay, double az,
                           int color, float size, int ticks) {
        if (ACTIVE.size() >= 64) {
            ACTIVE.remove(0);
        }
        ACTIVE.add(new Fx(kind, x, y, z, ax, ay, az, color, size, ticks, now));
    }

    public static void shake(float power, int ticks) {
        float current = shakePower * (float) shakeLeft / shakeTotal;
        if (power >= current) {
            shakePower = power;
            shakeLeft = Math.max(1, ticks);
            shakeTotal = shakeLeft;
        }
    }

    @SubscribeEvent
    public static void onTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) {
            return;
        }
        now++;
        if (shakeLeft > 0) {
            shakeLeft--;
        }
        ACTIVE.removeIf(f -> now - f.start > f.ticks);
    }

    @SubscribeEvent
    public static void onCamera(ViewportEvent.ComputeCameraAngles event) {
        if (shakeLeft <= 0) {
            return;
        }
        float pt = (float) event.getPartialTick();
        float t = now + pt;
        float k = shakePower * Math.max(0.0F, shakeLeft - pt) / shakeTotal;
        event.setYaw(event.getYaw() + Mth.sin(t * 2.3F) * k * 1.1F);
        event.setPitch(event.getPitch() + Mth.cos(t * 2.9F + 1.0F) * k * 0.9F);
        event.setRoll(event.getRoll() + Mth.sin(t * 3.7F + 2.0F) * k * 1.4F);
    }

    // ===================== рисование =====================
    @SubscribeEvent
    public static void onRender(RenderLevelStageEvent event) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_PARTICLES || ACTIVE.isEmpty()) {
            return;
        }
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null) {
            return;
        }
        Vec3 cam = event.getCamera().getPosition();
        Matrix4f m = event.getPoseStack().last().pose();
        float pt = event.getPartialTick();

        RenderSystem.enableBlend();
        RenderSystem.blendFunc(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE);
        RenderSystem.disableCull();
        RenderSystem.enableDepthTest();
        RenderSystem.depthMask(false);
        RenderSystem.setShader(GameRenderer::getPositionColorShader);
        BufferBuilder bb = Tesselator.getInstance().getBuilder();
        bb.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_COLOR);
        for (Fx f : ACTIVE) {
            float age = now - f.start + pt;
            if (age < 0.0F || age > f.ticks) {
                continue;
            }
            switch (f.kind) {
                case CIRCLE -> circle(bb, m, cam, f, age);
                case RING -> ringFx(bb, m, cam, f, age);
                case PILLAR -> pillar(bb, m, cam, f, age);
                case BEAM -> beam(bb, m, cam, f, age);
                case SLASH -> slash(bb, m, cam, f, age);
                case BURST -> burst(bb, m, cam, f, age);
                case HELIX -> helix(bb, m, cam, f, age);
                case RAIN -> rain(bb, m, cam, f, age);
                case SUN -> sun(bb, m, cam, f, age);
                case WINGS -> wings(bb, m, cam, f, age);
                case LIGHTNING -> lightning(bb, m, cam, f, age);
                default -> { }
            }
        }
        BufferBuilder.RenderedBuffer rendered = bb.endOrDiscardIfEmpty();
        if (rendered != null) {
            BufferUploader.drawWithShader(rendered);
        }
        RenderSystem.depthMask(true);
        RenderSystem.enableCull();
        RenderSystem.defaultBlendFunc();
        RenderSystem.disableBlend();
    }

    private static void vtx(BufferBuilder bb, Matrix4f m, Vec3 cam, double x, double y, double z,
                            float r, float g, float b, float a) {
        bb.vertex(m, (float) (x - cam.x), (float) (y - cam.y), (float) (z - cam.z)).color(r, g, b, a).endVertex();
    }

    /** Отрезок в горизонтальной плоскости (лента заданной ширины). */
    private static void flatSeg(BufferBuilder bb, Matrix4f m, Vec3 cam, double x1, double z1, double x2, double z2,
                                double y, double w, float r, float g, float b, float a) {
        double dx = x2 - x1;
        double dz = z2 - z1;
        double len = Math.sqrt(dx * dx + dz * dz);
        if (len < 1.0e-6D) {
            return;
        }
        double nx = -dz / len * w * 0.5D;
        double nz = dx / len * w * 0.5D;
        vtx(bb, m, cam, x1 - nx, y, z1 - nz, r, g, b, a);
        vtx(bb, m, cam, x1 + nx, y, z1 + nz, r, g, b, a);
        vtx(bb, m, cam, x2 + nx, y, z2 + nz, r, g, b, a);
        vtx(bb, m, cam, x2 - nx, y, z2 - nz, r, g, b, a);
    }

    private static void ringLine(BufferBuilder bb, Matrix4f m, Vec3 cam, double cx, double y, double cz, double radius,
                                 double w, int n, float r, float g, float b, float a) {
        for (int i = 0; i < n; i++) {
            double a1 = Math.PI * 2.0D * i / n;
            double a2 = Math.PI * 2.0D * (i + 1) / n;
            flatSeg(bb, m, cam, cx + Math.cos(a1) * radius, cz + Math.sin(a1) * radius,
                    cx + Math.cos(a2) * radius, cz + Math.sin(a2) * radius, y, w, r, g, b, a);
        }
    }

    /** Вертикальная полоса от земли вверх с затуханием по высоте. */
    private static void vquad(BufferBuilder bb, Matrix4f m, Vec3 cam, double x, double y, double z, double tx, double tz,
                              double w, double h, float r, float g, float b, float a0, float a1) {
        double hx = tx * w * 0.5D;
        double hz = tz * w * 0.5D;
        vtx(bb, m, cam, x - hx, y, z - hz, r, g, b, a0);
        vtx(bb, m, cam, x + hx, y, z + hz, r, g, b, a0);
        vtx(bb, m, cam, x + hx, y + h, z + hz, r, g, b, a1);
        vtx(bb, m, cam, x - hx, y + h, z - hz, r, g, b, a1);
    }

    /** Лента между двумя точками, всегда повёрнутая к камере. */
    private static void billboard(BufferBuilder bb, Matrix4f m, Vec3 cam, Vec3 p0, Vec3 p1, double w,
                                  float r, float g, float b, float a0, float a1) {
        Vec3 dir = p1.subtract(p0);
        Vec3 mid = p0.add(p1).scale(0.5D);
        Vec3 side = dir.cross(cam.subtract(mid));
        double len = side.length();
        if (len < 1.0e-6D) {
            return;
        }
        side = side.scale(w * 0.5D / len);
        vtx(bb, m, cam, p0.x - side.x, p0.y - side.y, p0.z - side.z, r, g, b, a0);
        vtx(bb, m, cam, p0.x + side.x, p0.y + side.y, p0.z + side.z, r, g, b, a0);
        vtx(bb, m, cam, p1.x + side.x, p1.y + side.y, p1.z + side.z, r, g, b, a1);
        vtx(bb, m, cam, p1.x - side.x, p1.y - side.y, p1.z - side.z, r, g, b, a1);
    }

    private static float easeOut(float t) {
        t = Mth.clamp(t, 0.0F, 1.0F);
        float u = 1.0F - t;
        return 1.0F - u * u * u;
    }

    private static double rnd(int i, int salt) {
        long h = i * 73856093L ^ salt * 19349663L;
        h = (h ^ (h >>> 13)) * 1274126177L;
        return (h & 0xFFFFL) / 65535.0D;
    }

    // ----- магический круг -----
    private static void circle(BufferBuilder bb, Matrix4f m, Vec3 cam, Fx f, float age) {
        float in = Math.min(1.0F, age / 6.0F);
        float out = Math.min(1.0F, (f.ticks - age) / 10.0F);
        float a = Mth.clamp(Math.min(in, out), 0.0F, 1.0F);
        if (a <= 0.0F) {
            return;
        }
        double radius = f.size * (0.55D + 0.45D * easeOut(in));
        double spin = age * 0.05D;
        double y = f.y + 0.06D;
        ringLine(bb, m, cam, f.x, y, f.z, radius, radius * 0.045D, 64, f.r, f.g, f.b, a * 0.9F);
        ringLine(bb, m, cam, f.x, y, f.z, radius * 0.80D, radius * 0.02D, 64, f.r, f.g, f.b, a * 0.6F);
        int style = (int) f.ay;
        double hr = radius * 0.74D;
        float wl = (float) (radius * 0.025D);
        switch (style) {
            case 1 -> {   // солнце: двенадцать лучей-шипов и внутреннее кольцо
                for (int i = 0; i < 12; i++) {
                    double ang = spin * 0.8D + i * Math.PI / 6.0D;
                    double r2 = radius * (0.66D + 0.1D * Math.sin(age * 0.3D + i));
                    flatSeg(bb, m, cam, f.x + Math.cos(ang) * radius * 0.36D, f.z + Math.sin(ang) * radius * 0.36D,
                            f.x + Math.cos(ang) * r2, f.z + Math.sin(ang) * r2, y, radius * 0.045D, f.r, f.g, f.b, a * 0.85F);
                }
                ringLine(bb, m, cam, f.x, y, f.z, radius * 0.36D, wl, 40, f.r, f.g, f.b, a * 0.8F);
            }
            case 2 -> {   // октаграмма {8/3}
                for (int i = 0; i < 8; i++) {
                    double a1 = spin + i * Math.PI / 4.0D;
                    double a2 = spin + (i + 3) * Math.PI / 4.0D;
                    flatSeg(bb, m, cam, f.x + Math.cos(a1) * hr, f.z + Math.sin(a1) * hr,
                            f.x + Math.cos(a2) * hr, f.z + Math.sin(a2) * hr, y, wl, f.r, f.g, f.b, a * 0.85F);
                }
            }
            case 3 -> {   // лотос: восемь лепестков
                for (int i = 0; i < 8; i++) {
                    double ang = spin * 0.5D + i * Math.PI / 4.0D;
                    double tx = f.x + Math.cos(ang) * hr;
                    double tz = f.z + Math.sin(ang) * hr;
                    double s1x = f.x + Math.cos(ang - 0.30D) * radius * 0.40D;
                    double s1z = f.z + Math.sin(ang - 0.30D) * radius * 0.40D;
                    double s2x = f.x + Math.cos(ang + 0.30D) * radius * 0.40D;
                    double s2z = f.z + Math.sin(ang + 0.30D) * radius * 0.40D;
                    flatSeg(bb, m, cam, f.x, f.z, s1x, s1z, y, wl, f.r, f.g, f.b, a * 0.7F);
                    flatSeg(bb, m, cam, s1x, s1z, tx, tz, y, wl, f.r, f.g, f.b, a * 0.9F);
                    flatSeg(bb, m, cam, tx, tz, s2x, s2z, y, wl, f.r, f.g, f.b, a * 0.9F);
                    flatSeg(bb, m, cam, s2x, s2z, f.x, f.z, y, wl, f.r, f.g, f.b, a * 0.7F);
                }
            }
            case 4 -> {   // шестерня: два квадрата и зубцы по кольцу
                for (int q = 0; q < 2; q++) {
                    for (int i = 0; i < 4; i++) {
                        double a1 = spin * (q == 0 ? 1.0D : -1.0D) + q * Math.PI / 4.0D + i * Math.PI / 2.0D;
                        double a2 = a1 + Math.PI / 2.0D;
                        flatSeg(bb, m, cam, f.x + Math.cos(a1) * hr, f.z + Math.sin(a1) * hr,
                                f.x + Math.cos(a2) * hr, f.z + Math.sin(a2) * hr, y, wl, f.r, f.g, f.b, a * 0.85F);
                    }
                }
                for (int i = 0; i < 16; i++) {
                    double ang = spin * 0.6D + i * Math.PI / 8.0D;
                    flatSeg(bb, m, cam, f.x + Math.cos(ang) * radius * 0.66D, f.z + Math.sin(ang) * radius * 0.66D,
                            f.x + Math.cos(ang) * radius * 0.76D, f.z + Math.sin(ang) * radius * 0.76D, y, radius * 0.05D,
                            f.r, f.g, f.b, a * 0.8F);
                }
            }
            default -> {   // гексаграмма из двух треугольников, вращается медленно
                for (int i = 0; i < 6; i++) {
                    double a1 = spin + i * Math.PI / 3.0D;
                    double a2 = spin + (i + 2) * Math.PI / 3.0D;
                    flatSeg(bb, m, cam, f.x + Math.cos(a1) * hr, f.z + Math.sin(a1) * hr,
                            f.x + Math.cos(a2) * hr, f.z + Math.sin(a2) * hr, y, wl, f.r, f.g, f.b, a * 0.8F);
                }
            }
        }
        // руны по кольцу вращаются в обратную сторону
        for (int i = 0; i < 24; i++) {
            double ang = -spin * 1.4D + i * Math.PI * 2.0D / 24.0D;
            double r1 = radius * 0.84D;
            double r2 = radius * (i % 2 == 0 ? 0.97D : 0.91D);
            flatSeg(bb, m, cam, f.x + Math.cos(ang) * r1, f.z + Math.sin(ang) * r1,
                    f.x + Math.cos(ang) * r2, f.z + Math.sin(ang) * r2, y, radius * 0.03D, f.r, f.g, f.b, a * 0.75F);
        }
        ringLine(bb, m, cam, f.x, y, f.z, radius * 0.12D, radius * 0.02D, 24, 1.0F, 1.0F, 1.0F, a * 0.8F);
        // лучи света, поднимающиеся с кольца
        for (int i = 0; i < 12; i++) {
            double ang = spin * 0.6D + i * Math.PI * 2.0D / 12.0D;
            double phase = (age * 0.035D + i * 0.37D) % 1.0D;
            double h = radius * 0.9D * (0.25D + 0.75D * phase);
            double px = f.x + Math.cos(ang) * radius * 0.92D;
            double pz = f.z + Math.sin(ang) * radius * 0.92D;
            vquad(bb, m, cam, px, y, pz, -Math.sin(ang), Math.cos(ang), radius * 0.05D, h, f.r, f.g, f.b,
                    a * 0.55F, 0.0F);
        }
    }

    // ----- расходящееся кольцо -----
    private static void ringFx(BufferBuilder bb, Matrix4f m, Vec3 cam, Fx f, float age) {
        float p = age / f.ticks;
        double radius = f.size * easeOut(p);
        double w = f.size * 0.10D * (1.0F - p) + 0.05D;
        float a = (float) Math.pow(1.0F - p, 1.5D);
        ringLine(bb, m, cam, f.x, f.y + 0.08D, f.z, radius, w, 56, f.r, f.g, f.b, a * 0.9F);
        ringLine(bb, m, cam, f.x, f.y + 0.08D, f.z, radius * 0.9D, w * 0.6D, 56, 1.0F, 1.0F, 1.0F, a * 0.45F);
    }

    // ----- световой столб -----
    private static void pillar(BufferBuilder bb, Matrix4f m, Vec3 cam, Fx f, float age) {
        float p = age / f.ticks;
        double h = f.size * Math.min(1.0F, age / 3.0F);
        float a = (float) Math.pow(1.0F - p, 0.8D) * Math.min(1.0F, age / 2.0F);
        double rad = 0.45D + 0.5D * (1.0F - p);
        for (int i = 0; i < 3; i++) {
            double ang = i * Math.PI / 3.0D;
            double tx = Math.cos(ang);
            double tz = Math.sin(ang);
            vquad(bb, m, cam, f.x, f.y, f.z, tx, tz, rad * 2.0D, h, f.r, f.g, f.b, a * 0.55F, 0.0F);
            vquad(bb, m, cam, f.x, f.y, f.z, tx, tz, rad * 0.7D, h, 1.0F, 1.0F, 1.0F, a * 0.8F, 0.0F);
        }
        ringLine(bb, m, cam, f.x, f.y + 0.06D, f.z, rad * 1.8D, 0.12D, 32, f.r, f.g, f.b, a * 0.8F);
    }

    // ----- луч -----
    private static void beam(BufferBuilder bb, Matrix4f m, Vec3 cam, Fx f, float age) {
        float p = age / f.ticks;
        Vec3 p0 = new Vec3(f.x, f.y, f.z);
        Vec3 full = new Vec3(f.ax, f.ay, f.az);
        Vec3 p1 = p0.add(full.subtract(p0).scale(Math.min(1.0F, age / 2.0F)));
        float a = (float) Math.pow(1.0F - p, 1.2D);
        billboard(bb, m, cam, p0, p1, f.size * (1.0F - 0.4F * p), f.r, f.g, f.b, a * 0.55F, a * 0.55F);
        billboard(bb, m, cam, p0, p1, f.size * 0.35F * (1.0F - p), 1.0F, 1.0F, 1.0F, a, a);
    }

    // ----- дуга взмаха -----
    private static void slash(BufferBuilder bb, Matrix4f m, Vec3 cam, Fx f, float age) {
        double base = Math.atan2(f.az, f.ax);
        double span = Math.toRadians(150.0D);
        double start = base - span / 2.0D;
        float reveal = Math.min(1.0F, age / 4.0F);
        float fade = Math.max(0.0F, 1.0F - Math.max(0.0F, age - 4.0F) / Math.max(1.0F, f.ticks - 4.0F));
        int n = 28;
        double y = f.y + 0.9D;
        for (int i = 0; i < n * reveal; i++) {
            double t1 = i / (double) n;
            double t2 = (i + 1) / (double) n;
            double a1 = start + span * t1;
            double a2 = start + span * t2;
            double r1 = f.size * (0.85D + 0.15D * Math.sin(t1 * Math.PI));
            double r2 = f.size * (0.85D + 0.15D * Math.sin(t2 * Math.PI));
            double w = f.size * 0.16D * Math.sin((t1 + 0.1D) * Math.PI * 0.9D);
            float a = fade * (0.25F + 0.75F * (float) t1);
            flatSeg(bb, m, cam, f.x + Math.cos(a1) * r1, f.z + Math.sin(a1) * r1,
                    f.x + Math.cos(a2) * r2, f.z + Math.sin(a2) * r2, y, w, f.r, f.g, f.b, a * 0.8F);
            flatSeg(bb, m, cam, f.x + Math.cos(a1) * r1 * 0.9D, f.z + Math.sin(a1) * r1 * 0.9D,
                    f.x + Math.cos(a2) * r2 * 0.9D, f.z + Math.sin(a2) * r2 * 0.9D, y, w * 0.35D, 1.0F, 1.0F, 1.0F, a);
        }
    }

    // ----- лучистый всплеск -----
    private static void burst(BufferBuilder bb, Matrix4f m, Vec3 cam, Fx f, float age) {
        float p = age / f.ticks;
        float e = easeOut(p);
        Vec3 c = new Vec3(f.x, f.y, f.z);
        for (int i = 0; i < 26; i++) {
            double u = rnd(i, 1) * 2.0D - 1.0D;
            double th = rnd(i, 2) * Math.PI * 2.0D;
            double s = Math.sqrt(1.0D - u * u);
            Vec3 dir = new Vec3(s * Math.cos(th), u, s * Math.sin(th));
            double in = f.size * (0.1D + 0.55D * e);
            double out = in + f.size * (0.18D + 0.25D * rnd(i, 3)) * (1.0F - p);
            float a = 1.0F - p;
            billboard(bb, m, cam, c.add(dir.scale(in)), c.add(dir.scale(out)), 0.05D + 0.05D * rnd(i, 4),
                    f.r, f.g, f.b, a, a * 0.15F);
        }
    }

    // ----- спираль: двойная нить света, поднимающаяся вокруг оси -----
    private static void helix(BufferBuilder bb, Matrix4f m, Vec3 cam, Fx f, float age) {
        float p = age / f.ticks;
        float a = Mth.clamp(Math.min(age / 5.0F, (1.0F - p) * 3.0F), 0.0F, 1.0F);
        double h = f.size * Math.min(1.0F, age / 7.0F);
        double rad = f.ax > 0.01D ? f.ax : 1.2D;
        double spin = age * 0.28D;
        int n = 40;
        for (int strand = 0; strand < 2; strand++) {
            Vec3 prev = null;
            for (int k = 0; k <= n; k++) {
                double t = k / (double) n;
                double ang = spin + t * Math.PI * 5.0D + strand * Math.PI;
                double rr = rad * (1.0D - 0.35D * t);
                Vec3 cur = new Vec3(f.x + Math.cos(ang) * rr, f.y + t * h, f.z + Math.sin(ang) * rr);
                if (prev != null) {
                    float al = a * (float) (1.0D - t * 0.75D);
                    billboard(bb, m, cam, prev, cur, 0.16D, f.r, f.g, f.b, al, al);
                    billboard(bb, m, cam, prev, cur, 0.06D, 1.0F, 1.0F, 1.0F, al, al);
                }
                prev = cur;
            }
        }
    }

    // ----- ливень светящихся нитей -----
    private static void rain(BufferBuilder bb, Matrix4f m, Vec3 cam, Fx f, float age) {
        float p = age / f.ticks;
        float a = Mth.clamp(Math.min(age / 4.0F, (1.0F - p) * 4.0F), 0.0F, 1.0F);
        for (int i = 0; i < 44; i++) {
            double ang = rnd(i, 11) * Math.PI * 2.0D;
            double rr = Math.sqrt(rnd(i, 12)) * f.size;
            double fall = (age * (0.8D + 0.5D * rnd(i, 13)) + rnd(i, 14) * 9.0D) % 9.0D;
            double x = f.x + Math.cos(ang) * rr;
            double z = f.z + Math.sin(ang) * rr;
            double y = f.y + 9.0D - fall;
            float al = a * (float) (0.4D + 0.6D * (1.0D - fall / 9.0D));
            billboard(bb, m, cam, new Vec3(x, y, z), new Vec3(x, y + 0.9D, z), 0.07D, f.r, f.g, f.b, al * 0.2F, al);
        }
        ringLine(bb, m, cam, f.x, f.y + 0.05D, f.z, f.size, 0.06D, 48, f.r, f.g, f.b, a * 0.5F);
    }

    // ----- солнечный диск на земле -----
    private static void sun(BufferBuilder bb, Matrix4f m, Vec3 cam, Fx f, float age) {
        float p = age / f.ticks;
        float a = Mth.clamp(Math.min(age / 5.0F, (1.0F - p) * 4.0F), 0.0F, 1.0F);
        double radius = f.size * (0.5D + 0.5D * easeOut(age / 8.0F));
        double spin = age * 0.09D;
        double y = f.y + 0.07D;
        for (int i = 0; i < 18; i++) {
            double ang = spin + i * Math.PI * 2.0D / 18.0D;
            double len = radius * (0.7D + 0.3D * Math.abs(Math.sin(age * 0.22D + i * 1.7D)));
            double w = radius * 0.12D;
            double bx = f.x + Math.cos(ang) * radius * 0.18D;
            double bz = f.z + Math.sin(ang) * radius * 0.18D;
            double ex = f.x + Math.cos(ang) * len;
            double ez = f.z + Math.sin(ang) * len;
            double nx = -Math.sin(ang) * w * 0.5D;
            double nz = Math.cos(ang) * w * 0.5D;
            vtx(bb, m, cam, bx - nx, y, bz - nz, f.r, f.g, f.b, a * 0.8F);
            vtx(bb, m, cam, bx + nx, y, bz + nz, f.r, f.g, f.b, a * 0.8F);
            vtx(bb, m, cam, ex + nx * 0.1D, y, ez + nz * 0.1D, f.r, f.g, f.b, 0.0F);
            vtx(bb, m, cam, ex - nx * 0.1D, y, ez - nz * 0.1D, f.r, f.g, f.b, 0.0F);
        }
        ringLine(bb, m, cam, f.x, y, f.z, radius * 0.18D, radius * 0.03D, 32, 1.0F, 1.0F, 1.0F, a);
        ringLine(bb, m, cam, f.x, y, f.z, radius * 0.95D, radius * 0.02D, 56, f.r, f.g, f.b, a * 0.8F);
    }

    // ----- крылья из света позади точки (направление взгляда в aux x, z) -----
    private static void wings(BufferBuilder bb, Matrix4f m, Vec3 cam, Fx f, float age) {
        float p = age / f.ticks;
        float a = Mth.clamp(Math.min(age / 4.0F, (1.0F - p) * 3.0F), 0.0F, 1.0F);
        double dl = Math.sqrt(f.ax * f.ax + f.az * f.az);
        double dx = dl > 1.0e-6D ? f.ax / dl : 0.0D;
        double dz = dl > 1.0e-6D ? f.az / dl : 1.0D;
        double sx = -dz;
        double sz = dx;
        double flap = Math.sin(age * 0.45D) * 0.18D;
        double spread = easeOut(age / 6.0F);
        Vec3 origin = new Vec3(f.x - dx * 0.25D, f.y + 1.35D, f.z - dz * 0.25D);
        for (int side = -1; side <= 1; side += 2) {
            for (int i = 0; i < 8; i++) {
                double t = i / 7.0D;
                double angle = (0.15D + t * 1.25D + flap) * spread;
                double len = f.size * (1.1D + 0.9D * Math.sin(t * Math.PI * 0.85D + 0.2D));
                Vec3 end = origin.add(sx * side * Math.cos(angle) * len - dx * 0.45D * len * 0.4D,
                        Math.sin(angle) * len,
                        sz * side * Math.cos(angle) * len - dz * 0.45D * len * 0.4D);
                billboard(bb, m, cam, origin, end, 0.28D * (1.0D - 0.4D * t), f.r, f.g, f.b, a * 0.75F, a * 0.05F);
                billboard(bb, m, cam, origin, end, 0.08D, 1.0F, 1.0F, 1.0F, a * 0.9F, 0.0F);
            }
        }
    }

    // ----- молния: ломаная линия, мерцает каждые два тика -----
    private static void lightning(BufferBuilder bb, Matrix4f m, Vec3 cam, Fx f, float age) {
        float p = age / f.ticks;
        float a = (float) Math.pow(1.0F - p, 0.8D);
        Vec3 p0 = new Vec3(f.x, f.y, f.z);
        Vec3 p1 = new Vec3(f.ax, f.ay, f.az);
        Vec3 dir = p1.subtract(p0);
        double len = dir.length();
        if (len < 0.1D) {
            return;
        }
        int flick = (int) (age / 2.0F);
        int n = 9;
        Vec3 up = Math.abs(dir.y / len) > 0.9D ? new Vec3(1, 0, 0) : new Vec3(0, 1, 0);
        Vec3 sideA = dir.cross(up).normalize();
        Vec3 sideB = dir.cross(sideA).normalize();
        Vec3 prev = p0;
        Vec3 mid = p0;
        for (int k = 1; k <= n; k++) {
            double t = k / (double) n;
            double amp = k == n ? 0.0D : len * 0.07D;
            Vec3 pt = p0.add(dir.scale(t))
                    .add(sideA.scale((rnd(k + flick * 17, 21) - 0.5D) * 2.0D * amp))
                    .add(sideB.scale((rnd(k + flick * 17, 22) - 0.5D) * 2.0D * amp));
            billboard(bb, m, cam, prev, pt, f.size, f.r, f.g, f.b, a * 0.6F, a * 0.6F);
            billboard(bb, m, cam, prev, pt, f.size * 0.3D, 1.0F, 1.0F, 1.0F, a, a);
            if (k == n / 2) {
                mid = pt;
            }
            prev = pt;
        }
        // боковая ветвь из середины
        Vec3 branchEnd = mid.add(sideA.scale((rnd(flick, 23) - 0.5D) * len * 0.5D)).add(dir.scale(0.25D))
                .add(sideB.scale((rnd(flick, 24) - 0.5D) * len * 0.3D));
        billboard(bb, m, cam, mid, branchEnd, f.size * 0.6D, f.r, f.g, f.b, a * 0.5F, 0.0F);
    }
}
