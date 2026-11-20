package com.example.fateapocrypha.client.map;

import com.example.fateapocrypha.ModDimensions;
import com.mojang.blaze3d.platform.NativeImage;
import com.mojang.blaze3d.systems.RenderSystem;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.DoubleUnaryOperator;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.MapColor;
import net.minecraftforge.fml.loading.FMLPaths;

/**
 * Настоящая карта мира в духе JourneyMap: вид сверху из реальных блоков (крыши, деревья, вода с глубиной, дороги),
 * с затенением по уклону. Карта открывается по мере исследования: чанки вокруг игрока считываются небольшими
 * порциями каждый тик (близкие чаще, дальние редко), результат хранится в текстурах регионов 256x256 блоков и
 * сохраняется на диск, так что открытое остаётся между запусками. Рисование на экране это несколько блитов готовых
 * текстур за кадр, поэтому карта не нагружает FPS (раньше рельеф и постройки считались и рисовались каждый кадр).
 */
public final class ChunkMap {
    static final int REGION = 256;
    private static final int RADIUS = 12;          // радиус опроса чанков вокруг игрока
    private static final int SCANS_PER_TICK = 6;
    private static final int STEPS_PER_TICK = 28;
    private static final int UPLOADS_PER_FRAME = 4;
    private static final int LOADS_PER_FRAME = 2;

    private static final Map<Long, Region> REGIONS = new HashMap<>();
    private static final Set<Long> ON_DISK = new HashSet<>();
    private static final Map<Long, Integer> LAST_SCAN = new HashMap<>();
    private static final int[][] OFFSETS = buildOffsets();
    private static final ExecutorService IO = Executors.newSingleThreadExecutor(r -> {
        Thread t = new Thread(r, "fate-map-io");
        t.setDaemon(true);
        return t;
    });

    private static String currentKey = null;
    private static Path dir = null;
    private static int cursor = 0;
    private static int tickCount = 0;

    private ChunkMap() {
    }

    private static final class Region {
        final int rx;
        final int rz;
        final int[] argb = new int[REGION * REGION];
        DynamicTexture tex;
        ResourceLocation id;
        boolean dirtyTex = true;
        boolean dirtySave = false;
        boolean blur = false;

        Region(int rx, int rz) {
            this.rx = rx;
            this.rz = rz;
        }
    }

    private static int[][] buildOffsets() {
        List<int[]> list = new ArrayList<>();
        for (int dx = -RADIUS; dx <= RADIUS; dx++) {
            for (int dz = -RADIUS; dz <= RADIUS; dz++) {
                list.add(new int[] {dx, dz});
            }
        }
        list.sort(Comparator.comparingInt(o -> o[0] * o[0] + o[1] * o[1]));
        return list.toArray(new int[0][]);
    }

    private static long rkey(int rx, int rz) {
        return ((long) rx << 32) ^ (rz & 0xFFFFFFFFL);
    }

    // ===================== мир и диск =====================
    private static String worldKey(Minecraft mc) {
        String base;
        if (mc.getSingleplayerServer() != null) {
            base = "sp_" + mc.getSingleplayerServer().getWorldData().getLevelName();
        } else if (mc.getCurrentServer() != null) {
            base = "mp_" + mc.getCurrentServer().ip;
        } else {
            base = "unknown";
        }
        return base.replaceAll("[^A-Za-z0-9._-]", "_");
    }

    private static void switchWorld(String key) {
        saveAll();
        REGIONS.values().forEach(ChunkMap::release);
        REGIONS.clear();
        LAST_SCAN.clear();
        ON_DISK.clear();
        cursor = 0;
        currentKey = key;
        dir = FMLPaths.GAMEDIR.get().resolve("fateapocrypha_map").resolve(key).resolve("trifas");
        try {
            Files.createDirectories(dir);
            try (var stream = Files.list(dir)) {
                stream.forEach(p -> {
                    String n = p.getFileName().toString();
                    if (n.startsWith("r_") && n.endsWith(".png")) {
                        String[] parts = n.substring(2, n.length() - 4).split("_");
                        if (parts.length == 2) {
                            try {
                                ON_DISK.add(rkey(Integer.parseInt(parts[0]), Integer.parseInt(parts[1])));
                            } catch (NumberFormatException ignored) {
                                // чужой файл в папке карты
                            }
                        }
                    }
                });
            }
        } catch (IOException e) {
            // карта работает и без сохранения на диск
        }
    }

    private static void release(Region r) {
        if (r.tex != null) {
            Minecraft.getInstance().getTextureManager().release(r.id);   // закрывает и саму текстуру
            r.tex = null;
        }
    }

    /** Загрузка региона с диска, если он там есть (PNG 256x256, цвета ARGB). */
    private static Region loadFromDisk(int rx, int rz) {
        Region r = new Region(rx, rz);
        Path file = dir.resolve("r_" + rx + "_" + rz + ".png");
        try (InputStream in = Files.newInputStream(file)) {
            NativeImage img = NativeImage.read(in);
            if (img.getWidth() == REGION && img.getHeight() == REGION) {
                for (int y = 0; y < REGION; y++) {
                    for (int x = 0; x < REGION; x++) {
                        int abgr = img.getPixelRGBA(x, y);
                        r.argb[y * REGION + x] = (abgr & 0xFF00FF00) | ((abgr >> 16) & 0xFF) | ((abgr & 0xFF) << 16);
                    }
                }
            }
            img.close();
        } catch (IOException | RuntimeException e) {
            // повреждённый файл: регион начнётся заново
        }
        return r;
    }

    private static void saveRegion(Region r) {
        if (dir == null) {
            return;
        }
        final int[] copy = r.argb.clone();
        final Path file = dir.resolve("r_" + r.rx + "_" + r.rz + ".png");
        r.dirtySave = false;
        IO.execute(() -> {
            try (NativeImage img = new NativeImage(NativeImage.Format.RGBA, REGION, REGION, false)) {
                for (int y = 0; y < REGION; y++) {
                    for (int x = 0; x < REGION; x++) {
                        int argb = copy[y * REGION + x];
                        img.setPixelRGBA(x, y, (argb & 0xFF00FF00) | ((argb >> 16) & 0xFF) | ((argb & 0xFF) << 16));
                    }
                }
                img.writeToFile(file);
            } catch (IOException | RuntimeException e) {
                // не записалось: попробуем при следующем сохранении
            }
        });
    }

    /** Сохраняет изменённые регионы на диск (в фоне). */
    public static void saveAll() {
        for (Region r : REGIONS.values()) {
            if (r.dirtySave) {
                saveRegion(r);
            }
        }
    }

    /** Выход из мира: сохранить всё и очистить память. */
    public static void shutdown() {
        saveAll();
        REGIONS.values().forEach(ChunkMap::release);
        REGIONS.clear();
        LAST_SCAN.clear();
        ON_DISK.clear();
        currentKey = null;
    }

    private static Region region(int rx, int rz, boolean create) {
        long k = rkey(rx, rz);
        Region r = REGIONS.get(k);
        if (r == null && dir != null) {
            if (ON_DISK.contains(k)) {
                r = loadFromDisk(rx, rz);
            } else if (create) {
                r = new Region(rx, rz);
            }
            if (r != null) {
                REGIONS.put(k, r);
            }
        }
        return r;
    }

    // ===================== сканирование мира =====================
    /** Вызывается каждый тик клиента: опрашивает несколько чанков вокруг игрока. */
    public static void tick() {
        Minecraft mc = Minecraft.getInstance();
        ClientLevel level = mc.level;
        LocalPlayer player = mc.player;
        if (level == null || player == null || !level.dimension().equals(ModDimensions.TRIFAS)) {
            return;
        }
        String key = worldKey(mc);
        if (!key.equals(currentKey)) {
            switchWorld(key);
        }
        tickCount++;
        if (tickCount % 1200 == 0) {
            saveAll();
        }
        int pcx = player.getBlockX() >> 4;
        int pcz = player.getBlockZ() >> 4;
        int budget = SCANS_PER_TICK;
        for (int steps = 0; steps < STEPS_PER_TICK && budget > 0; steps++) {
            int[] o = OFFSETS[cursor];
            cursor = (cursor + 1) % OFFSETS.length;
            int cx = pcx + o[0];
            int cz = pcz + o[1];
            int dist = Math.max(Math.abs(o[0]), Math.abs(o[1]));
            int interval = dist <= 2 ? 40 : dist <= 5 ? 200 : 3600;
            long ck = ChunkPos.asLong(cx, cz);
            Integer last = LAST_SCAN.get(ck);
            if (last != null && tickCount - last < interval) {
                continue;
            }
            LevelChunk chunk = level.getChunkSource().getChunkNow(cx, cz);
            if (chunk == null) {
                continue;
            }
            scan(level, chunk);
            LAST_SCAN.put(ck, tickCount);
            budget--;
        }
    }

    private static int lerp(int a, int b, float t) {
        int r = (int) (((a >> 16) & 0xFF) * (1 - t) + ((b >> 16) & 0xFF) * t);
        int g = (int) (((a >> 8) & 0xFF) * (1 - t) + ((b >> 8) & 0xFF) * t);
        int bl = (int) ((a & 0xFF) * (1 - t) + (b & 0xFF) * t);
        return (r << 16) | (g << 8) | bl;
    }

    private static int shade(int rgb, float f) {
        int r = Math.min(255, (int) (((rgb >> 16) & 0xFF) * f));
        int g = Math.min(255, (int) (((rgb >> 8) & 0xFF) * f));
        int b = Math.min(255, (int) ((rgb & 0xFF) * f));
        return (r << 16) | (g << 8) | b;
    }

    private static void scan(ClientLevel level, LevelChunk chunk) {
        ChunkPos cp = chunk.getPos();
        int bx = cp.getMinBlockX();
        int bz = cp.getMinBlockZ();
        Region r = region(Math.floorDiv(bx, REGION), Math.floorDiv(bz, REGION), true);
        if (r == null) {
            return;
        }
        LevelChunk north = level.getChunkSource().getChunkNow(cp.x, cp.z - 1);
        LevelChunk west = level.getChunkSource().getChunkNow(cp.x - 1, cp.z);
        int ox = Math.floorMod(bx, REGION);
        int oz = Math.floorMod(bz, REGION);
        int[] hts = new int[256];
        for (int lz = 0; lz < 16; lz++) {
            for (int lx = 0; lx < 16; lx++) {
                hts[lz * 16 + lx] = chunk.getHeight(Heightmap.Types.MOTION_BLOCKING, lx, lz);
            }
        }
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
        int minY = level.getMinBuildHeight();
        boolean changed = false;
        for (int lz = 0; lz < 16; lz++) {
            for (int lx = 0; lx < 16; lx++) {
                int h = hts[lz * 16 + lx];
                int hN = lz > 0 ? hts[(lz - 1) * 16 + lx]
                        : north != null ? north.getHeight(Heightmap.Types.MOTION_BLOCKING, lx, 15) : h;
                int hW = lx > 0 ? hts[lz * 16 + lx - 1]
                        : west != null ? west.getHeight(Heightmap.Types.MOTION_BLOCKING, 15, lz) : h;
                int y = h - 1;
                int argb = 0;
                if (y >= minY) {
                    pos.set(bx + lx, y, bz + lz);
                    BlockState st = chunk.getBlockState(pos);
                    int rgb = color(level, chunk, pos, st, y, minY, h, hN, hW);
                    argb = rgb < 0 ? 0 : 0xFF000000 | rgb;
                }
                int idx = (oz + lz) * REGION + ox + lx;
                if (r.argb[idx] != argb) {
                    r.argb[idx] = argb;
                    changed = true;
                }
            }
        }
        if (changed) {
            r.dirtyTex = true;
            r.dirtySave = true;
        }
    }

    /** Цвет верхнего блока колонки (вода с глубиной, остальное по цвету карты блока и уклону); -1 если пусто. */
    private static int color(ClientLevel level, LevelChunk chunk, BlockPos.MutableBlockPos pos, BlockState st,
                             int y, int minY, int h, int hN, int hW) {
        if (st.isAir()) {
            return -1;
        }
        FluidState fluid = st.getFluidState();
        if (!fluid.isEmpty()) {
            int depth = 1;
            for (int d = 1; d < 24 && y - d >= minY; d++) {
                pos.setY(y - d);
                if (chunk.getBlockState(pos).getFluidState().isEmpty()) {
                    break;
                }
                depth++;
            }
            return lerp(0x6DBBF2, 0x1C4A9C, Math.min(1.0F, (depth - 1) / 12.0F));
        }
        MapColor mc = st.getMapColor(level, pos);
        int tries = 0;
        while (mc == MapColor.NONE && tries < 6 && y - tries - 1 >= minY) {   // стекло и подобное: берём цвет блока под ним
            tries++;
            pos.setY(y - tries);
            mc = chunk.getBlockState(pos).getMapColor(level, pos);
        }
        if (mc == MapColor.NONE) {
            return 0x303846;
        }
        float d = h - (hN + hW) * 0.5F;
        float f = 1.0F + Math.max(-4.0F, Math.min(4.0F, d)) * 0.06F;
        return shade(mc.col, f);
    }

    // ===================== рисование =====================
    private static double invert(DoubleUnaryOperator f, double screen) {
        double a = f.applyAsDouble(0.0D);
        double b = f.applyAsDouble(1000.0D);
        return (screen - a) / ((b - a) / 1000.0D);
    }

    private static void upload(Region r) {
        Minecraft mc = Minecraft.getInstance();
        if (r.tex == null) {
            r.tex = new DynamicTexture(REGION, REGION, false);
            r.id = new ResourceLocation("fateapocrypha", "map/region_" + r.rx + "_" + r.rz);
            mc.getTextureManager().register(r.id, r.tex);
        }
        NativeImage img = r.tex.getPixels();
        if (img == null) {
            return;
        }
        for (int y = 0; y < REGION; y++) {
            for (int x = 0; x < REGION; x++) {
                int argb = r.argb[y * REGION + x];
                img.setPixelRGBA(x, y, (argb & 0xFF00FF00) | ((argb >> 16) & 0xFF) | ((argb & 0xFF) << 16));
            }
        }
        r.tex.upload();
        r.dirtyTex = false;
    }

    /**
     * Рисует открытые регионы в видимой области. sx и sy переводят мировые координаты в экранные, zoom это пикселей
     * на блок. При уменьшении (zoom меньше 1) текстуры сглаживаются, при увеличении остаются чёткими пикселями.
     */
    public static void draw(GuiGraphics g, float zoom, int vx1, int vy1, int vx2, int vy2,
                            DoubleUnaryOperator sx, DoubleUnaryOperator sy) {
        if (dir == null) {
            return;
        }
        int rx0 = Math.floorDiv((int) Math.floor(invert(sx, vx1)), REGION);
        int rx1 = Math.floorDiv((int) Math.ceil(invert(sx, vx2)), REGION);
        int rz0 = Math.floorDiv((int) Math.floor(invert(sy, vy1)), REGION);
        int rz1 = Math.floorDiv((int) Math.ceil(invert(sy, vy2)), REGION);
        boolean blur = zoom < 1.0F;
        int uploads = 0;
        int loads = 0;
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        for (int rz = rz0; rz <= rz1; rz++) {
            for (int rx = rx0; rx <= rx1; rx++) {
                long k = rkey(rx, rz);
                Region r = REGIONS.get(k);
                if (r == null) {
                    if (!ON_DISK.contains(k) || loads >= LOADS_PER_FRAME) {
                        continue;
                    }
                    r = region(rx, rz, false);
                    loads++;
                    if (r == null) {
                        continue;
                    }
                }
                if (r.tex == null || r.dirtyTex) {
                    if (uploads >= UPLOADS_PER_FRAME && r.tex != null) {
                        // обновим на следующем кадре; пока показываем прежнюю текстуру
                    } else {
                        upload(r);
                        uploads++;
                        r.blur = !blur;   // фильтр выставится ниже
                    }
                }
                if (r.tex == null) {
                    continue;
                }
                if (r.blur != blur) {
                    r.tex.setFilter(blur, false);
                    r.blur = blur;
                }
                int x0 = (int) Math.round(sx.applyAsDouble((double) rx * REGION));
                int x1 = (int) Math.round(sx.applyAsDouble((double) (rx + 1) * REGION));
                int y0 = (int) Math.round(sy.applyAsDouble((double) rz * REGION));
                int y1 = (int) Math.round(sy.applyAsDouble((double) (rz + 1) * REGION));
                g.blit(r.id, x0, y0, x1 - x0, y1 - y0, 0.0F, 0.0F, REGION, REGION, REGION, REGION);
            }
        }
    }

    /** Исследована ли эта точка (для подсказок и скрытия меток в неизвестных местах). */
    public static boolean explored(int x, int z) {
        Region r = REGIONS.get(rkey(Math.floorDiv(x, REGION), Math.floorDiv(z, REGION)));
        return r != null && r.argb[Math.floorMod(z, REGION) * REGION + Math.floorMod(x, REGION)] != 0;
    }
}
