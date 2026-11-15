package com.example.fateapocrypha.client.map;

import com.example.fateapocrypha.FateApocrypha;
import com.mojang.blaze3d.platform.NativeImage;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.resources.ResourceLocation;

/**
 * Текстуры карты. Обзорная карта (4 блока на пиксель) считается один раз в фоне, при приближении так же в фоне строится
 * детальный фрагмент вокруг экрана (до 0.5 блока на пиксель). Загрузка в видеопамять идёт только из потока отрисовки.
 */
final class MapTextures {
    static final double BASE_BPP = 4.0D;

    private static final ResourceLocation BASE_COLOR = rl("map_base_color");
    private static final ResourceLocation BASE_COAST = rl("map_base_coast");
    private static final ResourceLocation BASE_CONTOUR = rl("map_base_contour");
    private static final ResourceLocation DETAIL_COLOR = rl("map_detail_color");
    private static final ResourceLocation DETAIL_COAST = rl("map_detail_coast");
    private static final ResourceLocation DETAIL_CONTOUR = rl("map_detail_contour");

    private static final ExecutorService POOL = Executors.newSingleThreadExecutor(r -> {
        Thread t = new Thread(r, "fate-map");
        t.setDaemon(true);
        return t;
    });

    // обзорный слой
    private static volatile boolean baseStarted = false;
    private static boolean baseReady = false;
    private static volatile MapTerrain.Result basePending;
    private static MapTerrain.Result baseInfo;
    static final AtomicInteger PROGRESS = new AtomicInteger();
    private static final AtomicInteger BASE_STAMP = new AtomicInteger(0);

    // детальный слой
    private static final AtomicInteger DETAIL_STAMP = new AtomicInteger(0);
    private static volatile MapTerrain.Result detailPending;
    private static MapTerrain.Result detailInfo;
    private static volatile boolean detailBusy = false;

    private MapTextures() {
    }

    private static ResourceLocation rl(String path) {
        return new ResourceLocation(FateApocrypha.MODID, path);
    }

    static int baseWidth() {
        return (int) Math.ceil(MapTerrain.WIDTH / BASE_BPP);
    }

    static int baseHeight() {
        return (int) Math.ceil(MapTerrain.HEIGHT / BASE_BPP);
    }

    static void ensureBase() {
        if (baseStarted) {
            return;
        }
        baseStarted = true;
        POOL.submit(() -> {
            try {
                basePending = MapTerrain.generate(MapTerrain.X0, MapTerrain.Z0, BASE_BPP, baseWidth(), baseHeight(),
                        true, PROGRESS, BASE_STAMP, 0);
            } catch (RuntimeException e) {
                baseStarted = false;   // можно повторить при следующем открытии
            }
        });
    }

    static boolean baseReady() {
        return baseReady;
    }

    static float baseProgress() {
        return Math.min(1.0F, PROGRESS.get() / (float) baseHeight());
    }

    /** Поток отрисовки: забирает готовые слои и превращает их в текстуры. */
    static void tick() {
        MapTerrain.Result base = basePending;
        if (base != null) {
            basePending = null;
            upload(base, BASE_COLOR, BASE_COAST, BASE_CONTOUR);
            baseInfo = base;
            baseReady = true;
        }
        MapTerrain.Result detail = detailPending;
        if (detail != null) {
            detailPending = null;
            upload(detail, DETAIL_COLOR, DETAIL_COAST, DETAIL_CONTOUR);
            detailInfo = detail;
        }
    }

    private static void upload(MapTerrain.Result r, ResourceLocation color, ResourceLocation coast, ResourceLocation contours) {
        register(color, image(r.width(), r.height(), r.color(), false));
        register(coast, image(r.width(), r.height(), r.coast(), true));
        register(contours, image(r.width(), r.height(), r.contours(), true));
    }

    private static NativeImage image(int w, int h, int[] data, boolean mask) {
        NativeImage img = new NativeImage(NativeImage.Format.RGBA, w, h, false);
        for (int j = 0; j < h; j++) {
            for (int i = 0; i < w; i++) {
                int v = data[j * w + i];
                int abgr;
                if (mask) {
                    abgr = (v << 24) | 0x00FFFFFF;
                } else {
                    abgr = 0xFF000000 | ((v & 0xFF) << 16) | (((v >> 8) & 0xFF) << 8) | ((v >> 16) & 0xFF);
                }
                img.setPixelRGBA(i, j, abgr);
            }
        }
        return img;
    }

    private static void register(ResourceLocation id, NativeImage img) {
        DynamicTexture texture = new DynamicTexture(img);
        texture.setFilter(true, false);
        Minecraft.getInstance().getTextureManager().register(id, texture);
    }

    // ---------- детальный слой ----------
    static void requestDetail(double x0, double z0, double x1, double z1, double bpp) {
        double size = Math.max(x1 - x0, z1 - z0);
        bpp = Math.max(bpp, size / 700.0D);
        int w = (int) Math.ceil((x1 - x0) / bpp);
        int h = (int) Math.ceil((z1 - z0) / bpp);
        int stamp = DETAIL_STAMP.incrementAndGet();
        double fx0 = x0;
        double fz0 = z0;
        double fbpp = bpp;
        detailBusy = true;
        POOL.submit(() -> {
            try {
                MapTerrain.Result result = MapTerrain.generate(fx0, fz0, fbpp, w, h, false, null, DETAIL_STAMP, stamp);
                if (DETAIL_STAMP.get() == stamp) {
                    detailPending = result;
                }
            } catch (RuntimeException ignored) {
                // фрагмент просто не появится
            } finally {
                if (DETAIL_STAMP.get() == stamp) {
                    detailBusy = false;
                }
            }
        });
    }

    static boolean detailBusy() {
        return detailBusy;
    }

    static MapTerrain.Result detail() {
        return detailInfo;
    }

    static MapTerrain.Result base() {
        return baseInfo;
    }

    static ResourceLocation baseColor() {
        return BASE_COLOR;
    }

    static ResourceLocation baseCoast() {
        return BASE_COAST;
    }

    static ResourceLocation baseContour() {
        return BASE_CONTOUR;
    }

    static ResourceLocation detailColor() {
        return DETAIL_COLOR;
    }

    static ResourceLocation detailCoast() {
        return DETAIL_COAST;
    }

    static ResourceLocation detailContour() {
        return DETAIL_CONTOUR;
    }
}
