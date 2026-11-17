package com.example.fateapocrypha.client.fx;

import com.example.fateapocrypha.FateApocrypha;
import com.example.fateapocrypha.ModDimensions;
import com.example.fateapocrypha.world.CastleSite;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.BufferUploader;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexFormat;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderLevelStageEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.joml.Matrix4f;

/**
 * Собственные эффекты Трифаса, нарисованные вручную (без частиц и текстур Майнкрафта): дождь с тонкими каплями и
 * кругами на воде и крышах, лужи во дворе замка, которые набираются в дождь и высыхают после, и ползущий туман
 * в подземелье и подвале замка.
 */
@Mod.EventBusSubscriber(modid = FateApocrypha.MODID, value = Dist.CLIENT)
public final class TrifasFx {
    private static final double RANGE = 28.0D;
    private static final double TILE = 24.0D;
    private static final double FALL_H = 22.0D;

    private static List<double[]> puddles = null;
    private static long lastScan = 0L;

    private TrifasFx() {
    }

    private static double hf(int a, int b, int c, int salt) {
        long h = a * 73856093L ^ b * 19349663L ^ c * 83492791L ^ salt * 2654435761L;
        h = (h ^ (h >>> 13)) * 1274126177L;
        h ^= h >>> 16;
        return (h & 0xFFFFFF) / (double) 0x1000000;
    }

    private static void vertex(BufferBuilder bb, Matrix4f m, Vec3 cam, double x, double y, double z,
                               float r, float g, float b, float a) {
        bb.vertex(m, (float) (x - cam.x), (float) (y - cam.y), (float) (z - cam.z)).color(r, g, b, a).endVertex();
    }

    private static void finish(BufferBuilder bb) {
        BufferBuilder.RenderedBuffer rendered = bb.endOrDiscardIfEmpty();
        if (rendered != null) {
            BufferUploader.drawWithShader(rendered);
        }
    }

    @SubscribeEvent
    public static void onRender(RenderLevelStageEvent event) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_PARTICLES) {
            return;
        }
        Minecraft mc = Minecraft.getInstance();
        ClientLevel level = mc.level;
        if (level == null || mc.player == null || !level.dimension().equals(ModDimensions.TRIFAS)) {
            return;
        }
        long dayTime = level.getDayTime();
        double time = (dayTime % 100000000L + event.getPartialTick()) / 20.0D;
        float rain = TrifasWeather.intensity(dayTime);
        float wet = TrifasWeather.wetness(dayTime);
        Vec3 cam = event.getCamera().getPosition();
        int zone = mistZone(cam);
        boolean castleNear = Math.abs(cam.x - CastleSite.CX) < 160.0D && Math.abs(cam.z - CastleSite.CZ) < 180.0D;
        if (rain <= 0.01F && !(wet > 0.04F && castleNear) && zone == 0) {
            return;
        }
        PoseStack ps = event.getPoseStack();
        Matrix4f m = ps.last().pose();
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.disableCull();
        RenderSystem.enableDepthTest();
        RenderSystem.depthMask(false);
        RenderSystem.setShader(GameRenderer::getPositionColorShader);
        if (rain > 0.01F) {
            drops(m, level, cam, time, rain);
            ripples(m, level, cam, time, rain);
        }
        if (wet > 0.04F && castleNear) {
            puddles(m, level, cam, time, wet, rain);
        }
        if (zone != 0) {
            mist(m, cam, time, zone);
        }
        RenderSystem.depthMask(true);
        RenderSystem.enableCull();
        RenderSystem.disableBlend();
    }

    // ================= дождь =================
    private static void drops(Matrix4f m, ClientLevel level, Vec3 cam, double time, float rain) {
        Tesselator tess = Tesselator.getInstance();
        BufferBuilder bb = tess.getBuilder();
        bb.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_COLOR);
        int perTile = (int) (46.0F * rain) + 4;
        int tx0 = (int) Math.floor((cam.x - RANGE) / TILE);
        int tx1 = (int) Math.floor((cam.x + RANGE) / TILE);
        int tz0 = (int) Math.floor((cam.z - RANGE) / TILE);
        int tz1 = (int) Math.floor((cam.z + RANGE) / TILE);
        for (int tx = tx0; tx <= tx1; tx++) {
            for (int tz = tz0; tz <= tz1; tz++) {
                for (int j = 0; j < perTile; j++) {
                    double x = (tx + hf(tx, tz, j, 1)) * TILE;
                    double z = (tz + hf(tx, tz, j, 2)) * TILE;
                    double dx = x - cam.x;
                    double dz = z - cam.z;
                    double d2 = dx * dx + dz * dz;
                    if (d2 > RANGE * RANGE) {
                        continue;
                    }
                    int bx = (int) Math.floor(x);
                    int bz = (int) Math.floor(z);
                    if (!level.hasChunk(bx >> 4, bz >> 4)) {
                        continue;
                    }
                    double top = level.getHeight(Heightmap.Types.MOTION_BLOCKING, bx, bz);
                    double speed = 11.0D + 6.0D * hf(tx, tz, j, 4);
                    double f = (time * speed / FALL_H + hf(tx, tz, j, 3)) % 1.0D;
                    double y = top + FALL_H * (1.0D - f);
                    if (Math.abs(y - cam.y) > 34.0D) {
                        continue;
                    }
                    double dist = Math.sqrt(d2) + 0.001D;
                    double rx = -dz / dist * 0.012D;
                    double rz = dx / dist * 0.012D;
                    float head = 0.55F * rain;
                    float tail = 0.04F * rain;
                    vertex(bb, m, cam, x - rx, y, z - rz, 0.75F, 0.87F, 1.0F, head);
                    vertex(bb, m, cam, x + rx, y, z + rz, 0.75F, 0.87F, 1.0F, head);
                    vertex(bb, m, cam, x + rx, y + 0.8D, z + rz, 0.85F, 0.92F, 1.0F, tail);
                    vertex(bb, m, cam, x - rx, y + 0.8D, z - rz, 0.85F, 0.92F, 1.0F, tail);
                }
            }
        }
        finish(bb);
    }

    /** Круги расходятся там, где капля только что упала: на земле, воде и крышах (по карте высот). */
    private static void ripples(Matrix4f m, ClientLevel level, Vec3 cam, double time, float rain) {
        Tesselator tess = Tesselator.getInstance();
        BufferBuilder bb = tess.getBuilder();
        bb.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_COLOR);
        int perTile = (int) (46.0F * rain) + 4;
        int tx0 = (int) Math.floor((cam.x - 18.0D) / TILE);
        int tx1 = (int) Math.floor((cam.x + 18.0D) / TILE);
        int tz0 = (int) Math.floor((cam.z - 18.0D) / TILE);
        int tz1 = (int) Math.floor((cam.z + 18.0D) / TILE);
        for (int tx = tx0; tx <= tx1; tx++) {
            for (int tz = tz0; tz <= tz1; tz++) {
                for (int j = 0; j < perTile; j++) {
                    double speed = 11.0D + 6.0D * hf(tx, tz, j, 4);
                    double f = (time * speed / FALL_H + hf(tx, tz, j, 3)) % 1.0D;
                    if (f > 0.07D) {
                        continue;
                    }
                    double x = (tx + hf(tx, tz, j, 1)) * TILE;
                    double z = (tz + hf(tx, tz, j, 2)) * TILE;
                    double dx = x - cam.x;
                    double dz = z - cam.z;
                    if (dx * dx + dz * dz > 18.0D * 18.0D) {
                        continue;
                    }
                    int bx = (int) Math.floor(x);
                    int bz = (int) Math.floor(z);
                    if (!level.hasChunk(bx >> 4, bz >> 4)) {
                        continue;
                    }
                    double y = level.getHeight(Heightmap.Types.MOTION_BLOCKING, bx, bz) + 0.02D;
                    double t = f / 0.07D;
                    ring(bb, m, cam, x, y, z, 0.08D + 0.38D * t, 0.035D, (float) ((1.0D - t) * 0.6D * rain), 0.82F, 0.92F, 1.0F);
                }
            }
        }
        finish(bb);
    }

    private static void ring(BufferBuilder bb, Matrix4f m, Vec3 cam, double x, double y, double z, double radius,
                             double width, float alpha, float r, float g, float b) {
        int seg = 10;
        for (int k = 0; k < seg; k++) {
            double a0 = k * Math.PI * 2.0D / seg;
            double a1 = (k + 1) * Math.PI * 2.0D / seg;
            double in = Math.max(0.0D, radius - width);
            vertex(bb, m, cam, x + Math.cos(a0) * in, y, z + Math.sin(a0) * in, r, g, b, alpha);
            vertex(bb, m, cam, x + Math.cos(a0) * radius, y, z + Math.sin(a0) * radius, r, g, b, alpha);
            vertex(bb, m, cam, x + Math.cos(a1) * radius, y, z + Math.sin(a1) * radius, r, g, b, alpha);
            vertex(bb, m, cam, x + Math.cos(a1) * in, y, z + Math.sin(a1) * in, r, g, b, alpha);
        }
    }

    // ================= лужи во дворе =================
    private static void ensurePuddles(ClientLevel level) {
        long now = System.currentTimeMillis();
        if (puddles != null && (!puddles.isEmpty() || now - lastScan < 20000L)) {
            return;
        }
        lastScan = now;
        if (!level.hasChunk(CastleSite.CX >> 4, 300 >> 4)) {
            return;
        }
        List<double[]> list = new ArrayList<>();
        int y = CastleSite.Y;
        for (int x = 144; x <= 288; x += 2) {
            for (int z = 262; z <= 346; z += 2) {
                if (hf(x, z, 7, 11) > 0.10D) {
                    continue;
                }
                BlockState st = level.getBlockState(new BlockPos(x, y, z));
                boolean paved = st.is(Blocks.STONE_BRICKS) || st.is(Blocks.POLISHED_ANDESITE)
                        || st.is(Blocks.ANDESITE) || st.is(Blocks.CRACKED_STONE_BRICKS);
                if (!paved || !level.getBlockState(new BlockPos(x, y + 1, z)).isAir()) {
                    continue;
                }
                list.add(new double[] {x + hf(x, z, 1, 12), z + hf(x, z, 2, 12), 0.8D + 0.9D * hf(x, z, 3, 12), hf(x, z, 4, 12)});
            }
        }
        puddles = list;
    }

    private static void puddles(Matrix4f m, ClientLevel level, Vec3 cam, double time, float wet, float rain) {
        ensurePuddles(level);
        if (puddles == null || puddles.isEmpty()) {
            return;
        }
        Tesselator tess = Tesselator.getInstance();
        BufferBuilder bb = tess.getBuilder();
        bb.begin(VertexFormat.Mode.TRIANGLES, DefaultVertexFormat.POSITION_COLOR);
        double y = CastleSite.Y + 1.014D;
        float a = wet * 0.42F;
        int seg = 12;
        for (double[] p : puddles) {
            double dx = p[0] - cam.x;
            double dz = p[1] - cam.z;
            if (dx * dx + dz * dz > 70.0D * 70.0D) {
                continue;
            }
            double size = p[2] * (0.4D + 0.6D * wet);   // растут по мере дождя
            for (int k = 0; k < seg; k++) {
                double a0 = k * Math.PI * 2.0D / seg;
                double a1 = (k + 1) * Math.PI * 2.0D / seg;
                double r0 = size * (0.72D + 0.28D * hf(k, (int) (p[3] * 1000.0D), 5, 13));
                double r1 = size * (0.72D + 0.28D * hf((k + 1) % seg, (int) (p[3] * 1000.0D), 5, 13));
                vertex(bb, m, cam, p[0], y, p[1], 0.50F, 0.68F, 0.90F, a);
                vertex(bb, m, cam, p[0] + Math.cos(a0) * r0, y, p[1] + Math.sin(a0) * r0, 0.62F, 0.78F, 0.95F, a * 0.15F);
                vertex(bb, m, cam, p[0] + Math.cos(a1) * r1, y, p[1] + Math.sin(a1) * r1, 0.62F, 0.78F, 0.95F, a * 0.15F);
            }
            if (rain > 0.05F) {   // кольцо от капли в луже
                double t = (time * 0.8D + p[3]) % 1.0D;
                double rad = size * 0.55D * t;
                float ra = (float) ((1.0D - t) * 0.45D * rain);
                for (int k = 0; k < seg; k++) {
                    double a0 = k * Math.PI * 2.0D / seg;
                    double a1 = (k + 1) * Math.PI * 2.0D / seg;
                    double in = Math.max(0.0D, rad - 0.04D);
                    vertex(bb, m, cam, p[0] + Math.cos(a0) * in, y + 0.002D, p[1] + Math.sin(a0) * in, 0.9F, 0.96F, 1.0F, ra);
                    vertex(bb, m, cam, p[0] + Math.cos(a0) * rad, y + 0.002D, p[1] + Math.sin(a0) * rad, 0.9F, 0.96F, 1.0F, ra);
                    vertex(bb, m, cam, p[0] + Math.cos(a1) * rad, y + 0.002D, p[1] + Math.sin(a1) * rad, 0.9F, 0.96F, 1.0F, ra);
                    vertex(bb, m, cam, p[0] + Math.cos(a0) * in, y + 0.002D, p[1] + Math.sin(a0) * in, 0.9F, 0.96F, 1.0F, ra);
                    vertex(bb, m, cam, p[0] + Math.cos(a1) * rad, y + 0.002D, p[1] + Math.sin(a1) * rad, 0.9F, 0.96F, 1.0F, ra);
                    vertex(bb, m, cam, p[0] + Math.cos(a1) * in, y + 0.002D, p[1] + Math.sin(a1) * in, 0.9F, 0.96F, 1.0F, ra);
                }
            }
        }
        finish(bb);
    }

    // ================= туман в подземелье и подвале =================
    /** 0 нет, 1 подземелье Хирона, 2 подвал. */
    private static int mistZone(Vec3 c) {
        int y = CastleSite.Y;
        if (c.x > 140.0D && c.x < 262.0D && c.z > 258.0D && c.z < 284.0D && c.y < y - 7.0D) {
            return 1;
        }
        if (c.x > 160.0D && c.x < 270.0D && c.z > 205.0D && c.z < 248.0D && c.y < y - 0.5D && c.y > y - 8.0D) {
            return 2;
        }
        return 0;
    }

    private static void mist(Matrix4f m, Vec3 cam, double time, int zone) {
        Tesselator tess = Tesselator.getInstance();
        BufferBuilder bb = tess.getBuilder();
        bb.begin(VertexFormat.Mode.TRIANGLES, DefaultVertexFormat.POSITION_COLOR);
        double floor = CastleSite.Y - (zone == 1 ? 12.0D : 5.0D);
        float strength = zone == 1 ? 0.15F : 0.075F;
        float r = zone == 1 ? 0.80F : 0.90F;
        float g = zone == 1 ? 0.86F : 0.85F;
        float b = zone == 1 ? 0.96F : 0.80F;
        double cell = 7.0D;
        int cx0 = (int) Math.floor((cam.x - 22.0D) / cell);
        int cx1 = (int) Math.floor((cam.x + 22.0D) / cell);
        int cz0 = (int) Math.floor((cam.z - 22.0D) / cell);
        int cz1 = (int) Math.floor((cam.z + 22.0D) / cell);
        int seg = 14;
        for (int cx = cx0; cx <= cx1; cx++) {
            for (int cz = cz0; cz <= cz1; cz++) {
                if (hf(cx, cz, 1, 21) > 0.6D) {
                    continue;
                }
                double phase = hf(cx, cz, 2, 21) * 6.28D;
                double x = (cx + hf(cx, cz, 3, 21)) * cell + Math.sin(time * 0.12D + phase) * 1.8D;
                double z = (cz + hf(cx, cz, 4, 21)) * cell + Math.cos(time * 0.10D + phase) * 1.4D;
                double rad = 3.5D + 3.0D * hf(cx, cz, 5, 21);
                for (int layer = 0; layer < 2; layer++) {
                    double y = floor + 0.35D + layer * 0.55D + Math.sin(time * 0.2D + phase + layer) * 0.12D;
                    float a = strength * (layer == 0 ? 1.0F : 0.65F);
                    for (int k = 0; k < seg; k++) {
                        double a0 = k * Math.PI * 2.0D / seg;
                        double a1 = (k + 1) * Math.PI * 2.0D / seg;
                        vertex(bb, m, cam, x, y, z, r, g, b, a);
                        vertex(bb, m, cam, x + Math.cos(a0) * rad, y, z + Math.sin(a0) * rad, r, g, b, 0.0F);
                        vertex(bb, m, cam, x + Math.cos(a1) * rad, y, z + Math.sin(a1) * rad, r, g, b, 0.0F);
                    }
                }
            }
        }
        finish(bb);
    }
}
