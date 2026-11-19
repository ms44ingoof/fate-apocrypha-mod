package com.example.fateapocrypha.client.map;

import com.example.fateapocrypha.ModDimensions;
import com.example.fateapocrypha.client.ClientKeys;
import com.example.fateapocrypha.client.ClientQuestState;
import com.example.fateapocrypha.client.ClientSideQuests;
import com.example.fateapocrypha.client.UiDraw;
import com.example.fateapocrypha.story.SideBoard;
import com.example.fateapocrypha.story.SideQuest;
import com.example.fateapocrypha.story.SideQuestManager;
import com.example.fateapocrypha.story.StoryChapter;
import com.example.fateapocrypha.story.StoryLocation;
import com.example.fateapocrypha.client.fx.TrifasWeather;
import com.example.fateapocrypha.world.TrifasRoads;
import com.example.fateapocrypha.world.TrifasTerrain;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.math.Axis;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import org.lwjgl.glfw.GLFW;

/**
 * Карта всего мира (клавиша M): нежные пастельные цвета, плавное приближение колесом мыши, перетаскивание, подписи мест,
 * дорожки пунктиром, метка текущего задания и игрока. Цвет контура и рамки меняется в панели настроек.
 */
public class MapScreen extends Screen {
    private static final int INK = 0xFF7A6A80;
    private static final int INK_SOFT = 0xFFA796AD;
    private static final int SEA_COLOR = 0xFFD7EEF9;
    private static final int BTN = 18;

    private static boolean hasLast = false;
    private static double lastCx;
    private static double lastCz;
    private static float lastZoom;

    private record Marker(StoryLocation location, Component name, int color, boolean castle) {
    }

    private int px;
    private int py;
    private int pw;
    private int ph;
    private int vx1;
    private int vy1;
    private int vx2;
    private int vy2;

    private double cx = 1250.0D;
    private double cz = 700.0D;
    private float zoom = 0.30F;
    private float zoomTarget = 0.30F;
    private boolean anchoring = false;
    private double anchorWx;
    private double anchorWz;
    private double anchorSx;
    private double anchorSy;
    private boolean flying = false;
    private double flyX;
    private double flyZ;
    private boolean dragging = false;
    private boolean popup = false;
    private long lastFrame = 0L;
    private long lastChange = 0L;
    private long openTime = 0L;
    private final List<Marker> markers = new ArrayList<>();
    private MapMarks.Mark editing = null;
    private boolean editingNew = false;
    private final StringBuilder nameBuf = new StringBuilder();
    private boolean listOpen = false;
    private int listScroll = 0;
    private boolean castleView = false;
    private int castleFloor = 2;
    private double prevCx;
    private double prevCz;
    private float prevZoom;

    public MapScreen() {
        super(Component.translatable("gui.fateapocrypha.map.title"));
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    // ================= раскладка =================
    @Override
    protected void init() {
        MapSettings.load();
        MapTextures.ensureBase();
        pw = Math.min(width - 16, 900);
        ph = Math.min(height - 16, 520);
        px = (width - pw) / 2;
        py = (height - ph) / 2;
        vx1 = px + 10;
        vy1 = py + 28;
        vx2 = px + pw - 10;
        vy2 = py + ph - 10;
        buildMarkers();
        if (!hasLast) {
            LocalPlayer player = Minecraft.getInstance().player;
            if (player != null && inTrifas()) {
                cx = player.getX();
                cz = player.getZ();
            }
            zoom = 0.30F;
            zoomTarget = 0.30F;
            hasLast = true;
        } else {
            cx = lastCx;
            cz = lastCz;
            zoom = lastZoom;
            zoomTarget = lastZoom;
        }
        zoom = Math.max(minZoom(), zoom);
        zoomTarget = Math.max(minZoom(), zoomTarget);
        lastFrame = Util.getMillis();
        lastChange = lastFrame;
        openTime = lastFrame;
        zoom = Math.max(minZoom(), zoomTarget * 0.78F);   // карта плавно «подлетает» при открытии
    }

    /** Появление панели: 0 в начале, 1 через 0.45 с, с мягким замедлением. */
    private float appear() {
        float t = Math.min(1.0F, (Util.getMillis() - openTime) / 450.0F);
        return 1.0F - (1.0F - t) * (1.0F - t) * (1.0F - t);
    }

    @Override
    public void removed() {
        if (editing != null) {
            commitEdit();
        }
        lastCx = cx;
        lastCz = cz;
        lastZoom = zoomTarget;
        MapSettings.save();
    }

    private boolean inTrifas() {
        Minecraft mc = Minecraft.getInstance();
        return mc.level != null && mc.level.dimension().equals(ModDimensions.TRIFAS);
    }

    private float minZoom() {
        return (float) Math.max((vx2 - vx1) / MapTerrain.WIDTH, (vy2 - vy1) / MapTerrain.HEIGHT);
    }

    private static float maxZoom() {
        return 4.0F;
    }

    private double vcx() {
        return (vx1 + vx2) / 2.0D;
    }

    private double vcy() {
        return (vy1 + vy2) / 2.0D;
    }

    private double toScreenX(double wx) {
        return vcx() + (wx - cx) * zoom;
    }

    private double toScreenY(double wz) {
        return vcy() + (wz - cz) * zoom;
    }

    private double toWorldX(double sx) {
        return cx + (sx - vcx()) / zoom;
    }

    private double toWorldZ(double sy) {
        return cz + (sy - vcy()) / zoom;
    }

    // ================= места на карте =================
    private void buildMarkers() {
        markers.clear();
        Minecraft mc = Minecraft.getInstance();
        Set<StoryLocation> known = new HashSet<>();
        StoryChapter current = ClientQuestState.chapter();
        for (StoryChapter chapter : StoryChapter.values()) {
            if (chapter.ordinal() <= current.ordinal()) {
                StoryLocation l = StoryLocation.forChapter(chapter);
                if (l != null) {
                    known.add(l.primary());
                }
            }
        }
        for (StoryLocation l : StoryLocation.values()) {
            if (l.primary() != l) {
                continue;
            }
            boolean castle = l == StoryLocation.CASTLE_LAB;
            if (!castle && !known.contains(l) && !MapSettings.isSeen(mc, l)) {
                continue;   // места открываются по ходу истории или когда игрок побывал рядом
            }
            Component name = castle ? Component.translatable("gui.fateapocrypha.map.castle")
                    : Component.translatable(l.nameKey());
            markers.add(new Marker(l, name, markerColor(l), castle));
        }
    }

    private static int markerColor(StoryLocation l) {
        return switch (l) {
            case FOREST -> 0x7FCF9B;
            case RED_CAMP -> 0xF08A8A;
            case CHAPEL -> 0xB9A6EC;
            case RUINS -> 0xB8B0C0;
            case STREET, LONDON -> 0xF5B07A;
            case PLAIN -> 0x8FD3D6;
            case FARM -> 0xF0D58A;
            case GARDENS -> 0xF4A6C8;
            case GRAIL -> 0xF7D97A;
            case REVERSE, DREAM -> 0xA895E0;
            default -> 0xE8A0BF;
        };
    }

    private StoryLocation questLocation() {
        StoryChapter chapter = ClientQuestState.chapter();
        if (chapter == StoryChapter.TO_BE_CONTINUED || ClientQuestState.started()) {
            return null;
        }
        StoryLocation l = StoryLocation.forChapter(chapter);
        return l == null ? null : l.primary();
    }

    // ================= кадр =================
    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partial) {
        MapTextures.tick();
        updateView();
        this.renderBackground(g);

        UiDraw.shadow(g, px, py, px + pw, py + ph, 12);
        UiDraw.outlined(g, px, py, px + pw, py + ph, 12, 0xFF000000 | MapSettings.frame, 0xFFFFF9F3, 0xFFFCEFF6);
        drawTitle(g);

        g.enableScissor(vx1, vy1, vx2, vy2);
        g.fill(vx1, vy1, vx2, vy2, SEA_COLOR);
        if (castleView) {
            drawCastle(g, mouseX, mouseY);
        } else if (MapTextures.baseReady()) {
            drawTerrain(g);
            drawSparkles(g);
            drawClouds(g);
            if (MapSettings.grid) {
                drawGrid(g);
            }
            if (MapSettings.roads) {
                drawRoads(g);
            }
            drawBuildings(g);
            drawMarkers(g, mouseX, mouseY);
            drawSideBoards(g);
            drawMarks(g);
            drawPlayer(g);
        } else {
            drawLoading(g);
        }
        g.disableScissor();

        drawViewportFrame(g);
        drawCompass(g);
        if (!castleView) {
            drawScaleAndCoords(g, mouseX, mouseY);
        }
        drawButtons(g, mouseX, mouseY);
        if (listOpen && !castleView) {
            drawList(g, mouseX, mouseY);
        }
        if (castleView) {
            drawCastleTabs(g, mouseX, mouseY);
        }
        if (popup) {
            drawPopup(g, mouseX, mouseY);
        }
        if (MapTextures.baseReady() && !castleView && editing == null && !popupContains(mouseX, mouseY)
                && !(listOpen && listContains(mouseX, mouseY))) {
            drawHover(g, mouseX, mouseY);
        }
        if (editing != null) {
            drawEdit(g, mouseX, mouseY);
        }
        float a = appear();
        if (a < 1.0F) {
            int alpha = (int) ((1.0F - a) * 255.0F);
            UiDraw.rounded(g, px, py, px + pw, py + ph, 12, (alpha << 24) | 0xFFF8F2);   // панель проявляется из тумана
        }
    }

    private void updateView() {
        long now = Util.getMillis();
        double dt = Math.min(0.1D, (now - lastFrame) / 1000.0D);
        lastFrame = now;
        float k = 1.0F - (float) Math.pow(0.0008D, dt);
        float before = zoom;
        zoom += (zoomTarget - zoom) * k;
        if (Math.abs(zoomTarget - zoom) < 0.0005F * zoomTarget) {
            zoom = zoomTarget;
        }
        if (anchoring) {
            cx = anchorWx - (anchorSx - vcx()) / zoom;
            cz = anchorWz - (anchorSy - vcy()) / zoom;
            if (zoom == zoomTarget) {
                anchoring = false;
            }
        }
        if (flying) {
            cx += (flyX - cx) * k;
            cz += (flyZ - cz) * k;
            if (Math.abs(flyX - cx) < 0.5D && Math.abs(flyZ - cz) < 0.5D) {
                flying = false;
            }
        }
        clampCenter();
        if (before != zoom || anchoring || flying || dragging) {
            lastChange = now;
        }
        requestDetailIfNeeded(now);
    }

    private void clampCenter() {
        cx = Math.max(MapTerrain.X0, Math.min(MapTerrain.X0 + MapTerrain.WIDTH, cx));
        cz = Math.max(MapTerrain.Z0, Math.min(MapTerrain.Z0 + MapTerrain.HEIGHT, cz));
    }

    private void requestDetailIfNeeded(long now) {
        if (!MapTextures.baseReady() || zoomTarget < 0.32F || now - lastChange < 250L) {
            return;
        }
        double halfW = (vx2 - vx1) / 2.0D / zoom;
        double halfH = (vy2 - vy1) / 2.0D / zoom;
        double wantBpp = Math.max(0.5D, Math.min(2.0D, 0.8D / zoom));
        MapTerrain.Result d = MapTextures.detail();
        if (d != null) {
            double dx1 = d.x0();
            double dz1 = d.z0();
            double dx2 = d.x0() + d.width() * d.bpp();
            double dz2 = d.z0() + d.height() * d.bpp();
            boolean covers = cx - halfW >= dx1 && cx + halfW <= dx2 && cz - halfH >= dz1 && cz + halfH <= dz2;
            if (covers && d.bpp() <= wantBpp * 1.7D && d.bpp() >= wantBpp * 0.5D) {
                return;
            }
        }
        if (MapTextures.detailBusy()) {
            return;
        }
        MapTextures.requestDetail(cx - halfW * 1.45D, cz - halfH * 1.45D, cx + halfW * 1.45D, cz + halfH * 1.45D, wantBpp);
    }

    // ================= рельеф =================
    private void drawLayer(GuiGraphics g, ResourceLocation tex, int tw, int th, double wx0, double wz0, double bpp) {
        g.pose().pushPose();
        g.pose().translate((float) toScreenX(wx0), (float) toScreenY(wz0), 0.0F);
        float scale = (float) (zoom * bpp);
        g.pose().scale(scale, scale, 1.0F);
        g.blit(tex, 0, 0, 0, 0, tw, th, tw, th);
        g.pose().popPose();
    }

    private void tinted(GuiGraphics g, ResourceLocation tex, MapTerrain.Result r, float alpha) {
        float red = ((MapSettings.outline >> 16) & 0xFF) / 255.0F;
        float green = ((MapSettings.outline >> 8) & 0xFF) / 255.0F;
        float blue = (MapSettings.outline & 0xFF) / 255.0F;
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        g.setColor(red, green, blue, alpha);
        drawLayer(g, tex, r.width(), r.height(), r.x0(), r.z0(), r.bpp());
        g.setColor(1.0F, 1.0F, 1.0F, 1.0F);
    }

    private void drawTerrain(GuiGraphics g) {
        MapTerrain.Result base = MapTextures.base();
        if (base == null) {
            return;
        }
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        drawLayer(g, MapTextures.baseColor(), base.width(), base.height(), base.x0(), base.z0(), base.bpp());

        MapTerrain.Result d = MapTextures.detail();
        boolean detailCovers = false;
        if (d != null && zoom >= 0.28F) {
            double halfW = (vx2 - vx1) / 2.0D / zoom;
            double halfH = (vy2 - vy1) / 2.0D / zoom;
            detailCovers = cx - halfW >= d.x0() && cx + halfW <= d.x0() + d.width() * d.bpp()
                    && cz - halfH >= d.z0() && cz + halfH <= d.z0() + d.height() * d.bpp();
        }
        if (!detailCovers) {
            if (MapSettings.contours) {
                tinted(g, MapTextures.baseContour(), base, 1.0F);
            }
            tinted(g, MapTextures.baseCoast(), base, 1.0F);
        }
        if (d != null && zoom >= 0.28F) {
            drawLayer(g, MapTextures.detailColor(), d.width(), d.height(), d.x0(), d.z0(), d.bpp());
            if (MapSettings.contours) {
                tinted(g, MapTextures.detailContour(), d, 1.0F);
            }
            tinted(g, MapTextures.detailCoast(), d, 1.0F);
        }
    }

    private void drawLoading(GuiGraphics g) {
        int cxm = (vx1 + vx2) / 2;
        int cym = (vy1 + vy2) / 2;
        Component text = Component.translatable("gui.fateapocrypha.map.loading");
        g.drawString(font, text, cxm - font.width(text) / 2, cym - 14, INK, false);
        int bw = 120;
        UiDraw.rounded(g, cxm - bw / 2, cym, cxm + bw / 2, cym + 6, 3, 0x66FFFFFF);
        int fill = (int) (bw * MapTextures.baseProgress());
        if (fill > 4) {
            UiDraw.rounded(g, cxm - bw / 2, cym, cxm - bw / 2 + fill, cym + 6, 3, 0xFF000000 | MapSettings.outline);
        }
    }

    // ================= сетка и дороги =================
    private void drawGrid(GuiGraphics g) {
        int step = zoom >= 0.6F ? 100 : 500;
        double wx0 = toWorldX(vx1);
        double wx1 = toWorldX(vx2);
        double wz0 = toWorldZ(vy1);
        double wz1 = toWorldZ(vy2);
        for (long x = (long) (Math.floor(wx0 / step) * step); x <= wx1; x += step) {
            int sx = (int) toScreenX(x);
            g.fill(sx, vy1, sx + 1, vy2, 0x40FFFFFF);
            g.drawString(font, Long.toString(x), sx + 2, vy1 + 2, 0xCCA796AD, false);
        }
        for (long z = (long) (Math.floor(wz0 / step) * step); z <= wz1; z += step) {
            int sy = (int) toScreenY(z);
            g.fill(vx1, sy, vx2, sy + 1, 0x40FFFFFF);
            g.drawString(font, Long.toString(z), vx1 + 2, sy + 2, 0xCCA796AD, false);
        }
    }

    private void drawRoads(GuiGraphics g) {
        int dot = zoom >= 0.25F ? 2 : 1;
        double off = (Util.getMillis() / 70.0D) % 10.0D;   // точки медленно «бегут» по дорожке
        for (double[] seg : TrifasRoads.segments()) {
            double ax = toScreenX(seg[0]);
            double ay = toScreenY(seg[1]);
            double bx = toScreenX(seg[2]);
            double by = toScreenY(seg[3]);
            if (Math.max(ax, bx) < vx1 || Math.min(ax, bx) > vx2 || Math.max(ay, by) < vy1 || Math.min(ay, by) > vy2) {
                continue;
            }
            double len = Math.hypot(bx - ax, by - ay);
            if (len < 1.0D) {
                continue;
            }
            for (double d = off; d <= len; d += 10.0D) {
                double t = d / len;
                int x = (int) (ax + (bx - ax) * t);
                int y = (int) (ay + (by - ay) * t);
                g.fill(x, y + 1, x + dot, y + 1 + dot, 0x55A07C80);
                g.fill(x, y, x + dot, y + dot, 0xF2FFF6E4);
            }
        }
    }

    // ================= анимации карты =================
    private static int cellHash(long a, long b) {
        long h = a * 73856093L ^ b * 19349663L;
        h = (h ^ (h >>> 13)) * 1274126177L;
        return (int) (h ^ (h >>> 16));
    }

    /** Мерцающие блики на воде: сетка в мировых координатах, поэтому блики остаются на месте при движении карты. */
    private void drawSparkles(GuiGraphics g) {
        double cell = 80.0D / zoom;
        long time = Util.getMillis();
        long i0 = (long) Math.floor(toWorldX(vx1) / cell);
        long i1 = (long) Math.floor(toWorldX(vx2) / cell);
        long j0 = (long) Math.floor(toWorldZ(vy1) / cell);
        long j1 = (long) Math.floor(toWorldZ(vy2) / cell);
        for (long i = i0; i <= i1; i++) {
            for (long j = j0; j <= j1; j++) {
                int h = cellHash(i, j);
                double phase = ((h >> 16) & 1023) / 1023.0D * Math.PI * 2.0D;
                double tw = Math.sin(time / 520.0D + phase);
                if (tw < 0.55D) {
                    continue;
                }
                double wx = (i + (h & 255) / 255.0D) * cell;
                double wz = (j + ((h >> 8) & 255) / 255.0D) * cell;
                if (TrifasTerrain.height((int) wx, (int) wz) >= TrifasTerrain.SEA) {
                    continue;   // блики только на море
                }
                int sx = (int) toScreenX(wx);
                int sy = (int) toScreenY(wz);
                int alpha = (int) (200.0D * (tw - 0.55D) / 0.45D);
                int color = (alpha << 24) | 0xFFFFFF;
                g.fill(sx, sy - 2, sx + 1, sy + 3, color);
                g.fill(sx - 2, sy, sx + 3, sy + 1, color);
            }
        }
    }

    /** Мягкие облачка плывут над картой (со слабым параллаксом при движении). */
    private void drawClouds(GuiGraphics g) {
        long time = Util.getMillis();
        int w = vx2 - vx1 + 260;
        int alpha = zoom > 1.6F ? 0x14 : 0x2E;
        int color = (alpha << 24) | 0xFFFFFF;
        for (int i = 0; i < 6; i++) {
            double speed = 5.0D + i * 2.2D;
            double shift = i * 211.0D + time / 1000.0D * speed - cx * zoom * 0.18D;
            double x = vx1 - 130 + (((shift % w) + w) % w);
            double y = vy1 + 28 + (i * 97) % Math.max(60, vy2 - vy1 - 70);
            int r = 9 + (i % 3) * 3;
            UiDraw.circle(g, (int) x - r, (int) y + 3, r - 2, color);
            UiDraw.circle(g, (int) x, (int) y - 2, r + 3, color);
            UiDraw.circle(g, (int) x + r + 2, (int) y + 2, r, color);
        }
    }

    // ================= метки =================
    private void drawMarkers(GuiGraphics g, int mouseX, int mouseY) {
        StoryLocation quest = questLocation();
        long time = Util.getMillis();
        for (Marker m : markers) {
            int x = (int) toScreenX(m.location().cx());
            int y = (int) toScreenY(m.location().cz());
            if (x < vx1 - 40 || x > vx2 + 40 || y < vy1 - 20 || y > vy2 + 20) {
                continue;
            }
            boolean isQuest = quest != null && quest == m.location();
            if (isQuest) {
                float pulse = 1.0F + 0.18F * (float) Math.sin(time / 260.0D);
                UiDraw.circle(g, x, y, (int) (13 * pulse), 0x33F5D37A);
                UiDraw.circle(g, x, y, (int) (9 * pulse), 0x44F5D37A);
            }
            if (MapBuildings.hasArt(m.location(), zoom)) {
                // здания места уже нарисованы на карте: вместо точки метка только с подписью
            } else if (m.castle()) {
                drawCastleIcon(g, x, y, m.color());
            } else {
                UiDraw.circle(g, x, y, 5, 0xFFFFFFFF);
                UiDraw.circle(g, x, y, 4, 0xFF000000 | m.color());
                UiDraw.circle(g, x, y, 1, 0xFFFFFFFF);
            }
            if (isQuest) {
                int bob = (int) Math.round(Math.sin(time / 240.0D) * 2.5D);
                UiDraw.diamond(g, x, y - 12 + bob, 3, 0xFFF5C95A);
                UiDraw.diamond(g, x, y - 12 + bob, 1, 0xFFFFFFFF);
                for (int n = 0; n < 3; n++) {
                    double ang = time / 600.0D + n * 2.094D;
                    int sx = x + (int) Math.round(Math.cos(ang) * 9.0D);
                    int sy = y - 12 + bob + (int) Math.round(Math.sin(ang) * 9.0D);
                    g.fill(sx, sy, sx + 1, sy + 1, 0xFFFFF0B0);
                }
            }
            if (MapSettings.labels) {
                int tw = font.width(m.name());
                int lx = x - tw / 2;
                int ly = y + 8;
                if (m.castle() && MapBuildings.hasArt(m.location(), zoom)) {
                    ly = (int) toScreenY(com.example.fateapocrypha.world.CastleSite.RAMP_Z + 4);   // подпись под пандусом замка
                }
                UiDraw.rounded(g, lx - 4, ly - 2, lx + tw + 4, ly + 10, 5, 0xCCFFFFFF);
                g.drawString(font, m.name(), lx, ly, INK, false);
            }
        }
    }

    /** Объёмные постройки мест, открытых игроком, и случайные постройки мира. */
    private void drawBuildings(GuiGraphics g) {
        Set<StoryLocation> known = new HashSet<>();
        for (Marker m : markers) {
            known.add(m.location());
        }
        MapBuildings.draw(g, zoom, vx1, vy1, vx2, vy2, this::toScreenX, this::toScreenY, known);
    }

    /** Доски объявлений побочных заданий: «!» новое, синий значок задание идёт, зелёный сдать или доставить сюда. */
    private void drawSideBoards(GuiGraphics g) {
        long time = Util.getMillis();
        // цели разведки активных заданий: мягкий круг и точка
        for (SideQuest q : SideQuest.values()) {
            if (q.type() != SideQuest.Type.EXPLORE || ClientSideQuests.state(q) != SideQuestManager.ACTIVE) {
                continue;
            }
            int[] p = q.explorePoint();
            int x = (int) toScreenX(p[0]);
            int y = (int) toScreenY(p[1]);
            int r = Math.max(5, (int) (q.radius() * zoom));
            if (x + r < vx1 || x - r > vx2 || y + r < vy1 || y - r > vy2) {
                continue;
            }
            float pulse = 0.5F + 0.5F * (float) Math.sin(time / 380.0D);
            UiDraw.circle(g, x, y, r, 0x22 << 24 | 0x6FA8E8);
            UiDraw.circle(g, x, y, Math.max(3, (int) (r * (0.55F + 0.1F * pulse))), 0x28 << 24 | 0x6FA8E8);
            UiDraw.diamond(g, x, y, 4, 0xFFFFFFFF);
            UiDraw.diamond(g, x, y, 3, 0xFF6FA8E8);
        }
        for (SideBoard b : SideBoard.values()) {
            int st = ClientSideQuests.boardState(b);
            if (st == 0) {
                continue;
            }
            int x = (int) toScreenX(b.x() + 0.5D);
            int y = (int) toScreenY(b.z() + 0.5D);
            if (x < vx1 - 20 || x > vx2 + 20 || y < vy1 - 20 || y > vy2 + 20) {
                continue;
            }
            int color = st == 3 ? 0x7BD389 : st == 1 ? 0xF5C95A : 0x6FA8E8;
            int bob = st == 1 ? (int) Math.round(Math.sin(time / 300.0D) * 1.5D) : 0;
            UiDraw.diamond(g, x, y + bob, 6, 0xFFFFFFFF);
            UiDraw.diamond(g, x, y + bob, 5, 0xFF000000 | color);
            String mark = st == 1 ? "!" : st == 3 ? "?" : "~";
            g.drawString(font, mark, x - font.width(mark) / 2, y + bob - 4, 0xFFFFFFFF, false);
            if (MapSettings.labels && zoom >= 0.5F) {
                Component name = Component.translatable(b.nameKey());
                int tw = font.width(name);
                int lx = x - tw / 2;
                int ly = y + 9;
                UiDraw.rounded(g, lx - 4, ly - 2, lx + tw + 4, ly + 10, 5, 0xCCFFFFFF);
                g.drawString(font, name, lx, ly, INK, false);
            }
        }
    }

    private void drawCastleIcon(GuiGraphics g, int x, int y, int color) {
        int c = 0xFF000000 | color;
        g.fill(x - 6, y - 5, x + 6, y + 5, 0xFFFFFFFF);
        g.fill(x - 5, y - 2, x + 5, y + 4, c);
        for (int i = -5; i <= 3; i += 3) {
            g.fill(x + i, y - 5, x + i + 2, y - 2, c);
        }
        g.fill(x - 1, y + 1, x + 1, y + 4, 0xFFFFFFFF);
    }

    private void drawPlayer(GuiGraphics g) {
        Minecraft mc = Minecraft.getInstance();
        LocalPlayer player = mc.player;
        if (player == null || !inTrifas()) {
            return;
        }
        int x = (int) toScreenX(player.getX());
        int y = (int) toScreenY(player.getZ());
        if (x < vx1 - 10 || x > vx2 + 10 || y < vy1 - 10 || y > vy2 + 10) {
            return;
        }
        UiDraw.circle(g, x, y, 7, 0x33FFFFFF);
        double ph = (Util.getMillis() % 1800L) / 1800.0D;   // расходящиеся круги вокруг игрока
        int rr = 6 + (int) (ph * 14.0D);
        int ra = (int) ((1.0D - ph) * 150.0D);
        for (int n = 0; n < 20; n++) {
            double ang = n * Math.PI / 10.0D;
            int rx = x + (int) Math.round(Math.cos(ang) * rr);
            int ry = y + (int) Math.round(Math.sin(ang) * rr);
            g.fill(rx, ry, rx + 1, ry + 1, (ra << 24) | 0xF08AB0);
        }
        g.pose().pushPose();
        g.pose().translate(x, y, 0.0F);
        g.pose().mulPose(Axis.ZP.rotationDegrees(player.getYRot() + 180.0F));
        for (int i = 0; i < 9; i++) {
            int hw = i / 2 + 1;
            g.fill(-hw - 1, -7 + i, hw + 2, -6 + i, 0xFFFFFFFF);
        }
        for (int i = 0; i < 8; i++) {
            int hw = i / 2;
            g.fill(-hw, -6 + i, hw + 1, -5 + i, 0xFFF08AB0);
        }
        g.pose().popPose();
        UiDraw.circle(g, x, y, 3, 0xFFFFFFFF);
        UiDraw.circle(g, x, y, 2, 0xFFF08AB0);
    }

    private void drawHover(GuiGraphics g, int mouseX, int mouseY) {
        if (mouseX < vx1 || mouseX > vx2 || mouseY < vy1 || mouseY > vy2) {
            return;
        }
        Minecraft mc = Minecraft.getInstance();
        MapMarks.Mark hm = markAt(mouseX, mouseY);
        if (hm != null) {
            List<Component> lines = new ArrayList<>();
            lines.add(Component.literal(hm.name));
            lines.add(Component.translatable("gui.fateapocrypha.map.mark_hint"));
            int w = 0;
            for (Component line : lines) {
                w = Math.max(w, font.width(line));
            }
            int bx = Math.min(mouseX + 10, width - w - 14);
            int by = Math.min(mouseY + 8, height - lines.size() * 11 - 12);
            UiDraw.outlined(g, bx, by, bx + w + 10, by + lines.size() * 11 + 6, 5,
                    0xFF000000 | MapSettings.frame, 0xF5FFFBF7, 0xF5FFF1F6);
            for (int i = 0; i < lines.size(); i++) {
                g.drawString(font, lines.get(i), bx + 5, by + 4 + i * 11, i == 0 ? INK : INK_SOFT, false);
            }
            return;
        }
        for (Marker m : markers) {
            int x = (int) toScreenX(m.location().cx());
            int y = (int) toScreenY(m.location().cz());
            if (Math.abs(mouseX - x) <= 8 && Math.abs(mouseY - y) <= 8) {
                List<Component> lines = new ArrayList<>();
                lines.add(m.name());
                if (mc.player != null && inTrifas()) {
                    long dist = Math.round(Math.max(0.0D, m.location().distanceTo(mc.player.getX(), mc.player.getZ())
                            - m.location().radius()));
                    lines.add(Component.translatable("gui.fateapocrypha.map.distance", dist));
                }
                int w = 0;
                for (Component line : lines) {
                    w = Math.max(w, font.width(line));
                }
                int bx = Math.min(mouseX + 10, width - w - 14);
                int by = Math.min(mouseY + 8, height - lines.size() * 11 - 12);
                UiDraw.outlined(g, bx, by, bx + w + 10, by + lines.size() * 11 + 6, 5,
                        0xFF000000 | MapSettings.frame, 0xF5FFFBF7, 0xF5FFF1F6);
                for (int i = 0; i < lines.size(); i++) {
                    g.drawString(font, lines.get(i), bx + 5, by + 4 + i * 11, i == 0 ? INK : INK_SOFT, false);
                }
                return;
            }
        }
    }

    // ================= рамка и подписи =================
    private void drawTitle(GuiGraphics g) {
        int fx = px + 18;
        int fy = py + 14;
        int pink = 0xFFF4B6CF;
        for (int[] d : new int[][] {{0, -3}, {3, -1}, {2, 3}, {-2, 3}, {-3, -1}}) {
            UiDraw.circle(g, fx + d[0], fy + d[1], 2, pink);
        }
        UiDraw.circle(g, fx, fy, 2, 0xFFF6DC8C);
        g.drawString(font, this.title, px + 30, py + 9, INK, false);
        Component hint = Component.translatable("gui.fateapocrypha.map.hint", ClientKeys.MAP.getTranslatedKeyMessage());
        g.drawString(font, hint, px + pw - 14 - font.width(hint), py + 9, INK_SOFT, false);
        Minecraft mc = Minecraft.getInstance();
        if (mc.level != null && inTrifas()) {
            float rain = TrifasWeather.intensity(mc.level.getDayTime());
            if (rain > 0.01F) {
                drawRainIcon(g, px + pw - 14 - font.width(hint) - 22, py + 14, rain);
            }
        }
    }

    /** Облачко с падающими каплями: идёт дождь. */
    private void drawRainIcon(GuiGraphics g, int x, int y, float rain) {
        long t = Util.getMillis();
        UiDraw.circle(g, x - 4, y, 4, 0xFFC9D6EA);
        UiDraw.circle(g, x + 1, y - 2, 5, 0xFFD6E1F2);
        UiDraw.circle(g, x + 6, y, 4, 0xFFC9D6EA);
        for (int i = 0; i < 3; i++) {
            int dy = (int) ((t / 90L + i * 4L) % 9L);
            g.fill(x - 4 + i * 5, y + 5 + dy, x - 3 + i * 5, y + 8 + dy, 0xFF8FB8F0);
        }
    }

    private void drawViewportFrame(GuiGraphics g) {
        int c = 0xFF000000 | MapSettings.frame;
        int soft = UiDraw.withAlpha(MapSettings.frame, 110);
        g.fill(vx1 - 2, vy1 - 2, vx2 + 2, vy1, c);
        g.fill(vx1 - 2, vy2, vx2 + 2, vy2 + 2, c);
        g.fill(vx1 - 2, vy1, vx1, vy2, c);
        g.fill(vx2, vy1, vx2 + 2, vy2, c);
        g.fill(vx1, vy1, vx2, vy1 + 1, soft);
        g.fill(vx1, vy2 - 1, vx2, vy2, soft);
        g.fill(vx1, vy1, vx1 + 1, vy2, soft);
        g.fill(vx2 - 1, vy1, vx2, vy2, soft);
        // скруглённые уголки: фон панели закрывает острые углы карты
        int bg = 0xFFFDF3F6;
        for (int[] corner : new int[][] {{vx1, vy1, 1, 1}, {vx2, vy1, -1, 1}, {vx1, vy2, 1, -1}, {vx2, vy2, -1, -1}}) {
            for (int i = 0; i < 4; i++) {
                int len = 4 - i;
                int x1 = corner[2] > 0 ? corner[0] : corner[0] - len;
                int y1 = corner[3] > 0 ? corner[1] + i : corner[1] - i - 1;
                g.fill(x1, y1, x1 + len, y1 + 1, bg);
            }
            UiDraw.circle(g, corner[0] + corner[2] * 3, corner[1] + corner[3] * 3, 1, c);
        }
    }

    private void drawCompass(GuiGraphics g) {
        int x = vx1 + 26;
        int y = vy1 + 26;
        UiDraw.circle(g, x, y, 17, 0xAAFFFFFF);
        UiDraw.circle(g, x, y, 16, 0x55FFFFFF);
        UiDraw.diamond(g, x, y - 8, 3, 0xFFF08AB0);
        UiDraw.diamond(g, x, y + 8, 3, 0xFFD9CDE8);
        UiDraw.diamond(g, x - 8, y, 3, 0xFFD9CDE8);
        UiDraw.diamond(g, x + 8, y, 3, 0xFFD9CDE8);
        UiDraw.circle(g, x, y, 2, 0xFFFFFFFF);
        g.drawString(font, "N", x - 2, y - 24, INK, false);
    }

    private void drawScaleAndCoords(GuiGraphics g, int mouseX, int mouseY) {
        int[] options = {20, 50, 100, 200, 500, 1000, 2000};
        int blocks = options[0];
        for (int o : options) {
            if (o * zoom <= 110.0F) {
                blocks = o;
            }
        }
        int len = Math.max(8, (int) (blocks * zoom));
        int x = vx1 + 12;
        int y = vy2 - 14;
        Component label = Component.translatable("gui.fateapocrypha.map.scale", blocks);
        int tw = font.width(label);
        UiDraw.rounded(g, x - 6, y - 14, x + Math.max(len, tw) + 10, y + 8, 6, 0xB8FFFFFF);
        g.drawString(font, label, x, y - 11, INK, false);
        g.fill(x, y + 2, x + len, y + 4, INK);
        g.fill(x, y - 1, x + 2, y + 4, INK);
        g.fill(x + len - 2, y - 1, x + len, y + 4, INK);

        if (mouseX >= vx1 && mouseX <= vx2 && mouseY >= vy1 && mouseY <= vy2 && !popupContains(mouseX, mouseY)) {
            Component coords = Component.translatable("gui.fateapocrypha.map.coords",
                    Math.round(toWorldX(mouseX)), Math.round(toWorldZ(mouseY)));
            int cw = font.width(coords);
            int cxx = vx2 - cw - 14;
            int cyy = vy2 - 18;
            UiDraw.rounded(g, cxx - 6, cyy - 3, cxx + cw + 6, cyy + 11, 6, 0xB8FFFFFF);
            g.drawString(font, coords, cxx, cyy, INK, false);
        }
    }

    // ================= свои метки =================
    private void drawMarks(GuiGraphics g) {
        Minecraft mc = Minecraft.getInstance();
        long time = Util.getMillis();
        for (MapMarks.Mark m : MapMarks.list(mc)) {
            int x = (int) toScreenX(m.x);
            int y = (int) toScreenY(m.z);
            if (x < vx1 - 20 || x > vx2 + 20 || y < vy1 - 20 || y > vy2 + 20) {
                continue;
            }
            if (m == editing) {
                for (int n = 0; n < 14; n++) {
                    double ang = n * Math.PI / 7.0D + time / 700.0D;
                    int rx = x + (int) Math.round(Math.cos(ang) * 9.0D);
                    int ry = y - 3 + (int) Math.round(Math.sin(ang) * 9.0D);
                    g.fill(rx, ry, rx + 1, ry + 1, 0xFF000000 | m.color);
                }
            }
            MapMarks.drawIcon(g, m.icon, x, y - 3, m.color);
            if (MapSettings.labels && zoom >= 0.12F && !m.name.isEmpty()) {
                int tw = font.width(m.name);
                int lx = x - tw / 2;
                int ly = y + 6;
                UiDraw.rounded(g, lx - 4, ly - 2, lx + tw + 4, ly + 10, 5, 0xCCFFFFFF);
                g.drawString(font, m.name, lx, ly, INK, false);
            }
        }
    }

    private MapMarks.Mark markAt(double mx, double my) {
        for (MapMarks.Mark m : MapMarks.list(Minecraft.getInstance())) {
            if (Math.abs(mx - toScreenX(m.x)) <= 7 && Math.abs(my - (toScreenY(m.z) - 3)) <= 8) {
                return m;
            }
        }
        return null;
    }

    private static final int EDIT_W = 176;
    private static final int EDIT_H = 124;

    private int editX() {
        return (int) Math.max(vx1 + 4, Math.min(vx2 - EDIT_W - 4, toScreenX(editing.x) + 14));
    }

    private int editY() {
        return (int) Math.max(vy1 + 4, Math.min(vy2 - EDIT_H - 4, toScreenY(editing.z) - 60));
    }

    private static boolean inRect(double mx, double my, int x1, int y1, int x2, int y2) {
        return mx >= x1 && mx <= x2 && my >= y1 && my <= y2;
    }

    private void drawEdit(GuiGraphics g, int mouseX, int mouseY) {
        int ex = editX();
        int ey = editY();
        UiDraw.shadow(g, ex, ey, ex + EDIT_W, ey + EDIT_H, 9);
        UiDraw.outlined(g, ex, ey, ex + EDIT_W, ey + EDIT_H, 9, 0xFF000000 | MapSettings.frame, 0xFAFFFBF7, 0xFAFFF1F6);
        g.drawString(font, Component.translatable(editingNew ? "gui.fateapocrypha.map.mark_new"
                : "gui.fateapocrypha.map.mark_edit"), ex + 10, ey + 8, INK, false);
        // поле названия: своё, с мигающим курсором
        UiDraw.rounded(g, ex + 10, ey + 22, ex + EDIT_W - 10, ey + 38, 5, 0xFF000000 | MapSettings.frame);
        UiDraw.rounded(g, ex + 11, ey + 23, ex + EDIT_W - 11, ey + 37, 4, 0xFFFFFFFF);
        boolean blink = (Util.getMillis() / 450L) % 2L == 0L;
        String text = nameBuf.toString();
        if (text.isEmpty()) {
            g.drawString(font, Component.translatable("gui.fateapocrypha.map.mark_name"), ex + 16, ey + 26, 0xFFC4B8CB, false);
            if (blink) {
                g.fill(ex + 15, ey + 26, ex + 16, ey + 35, INK);
            }
        } else {
            g.drawString(font, text, ex + 16, ey + 26, INK, false);
            if (blink) {
                int cxp = ex + 16 + font.width(text);
                g.fill(cxp, ey + 26, cxp + 1, ey + 35, INK);
            }
        }
        for (int i = 0; i < MapMarks.ICONS.length; i++) {
            int ix = ex + 12 + 30 * i;
            int iy = ey + 46;
            boolean sel = editing.icon == i;
            UiDraw.rounded(g, ix, iy, ix + 24, iy + 22, 6, sel ? 0xFF000000 | editing.color : 0x55B9A6EC);
            UiDraw.rounded(g, ix + 1, iy + 1, ix + 23, iy + 21, 5, 0xFFFFFFFF);
            MapMarks.drawIcon(g, i, ix + 12, iy + 11, editing.color);
        }
        for (int i = 0; i < MapSettings.PALETTE.length; i++) {
            int sx = ex + 13 + 15 * i;
            int sy = ey + 74;
            int color = MapSettings.PALETTE[i];
            if (color == editing.color) {
                UiDraw.rounded(g, sx - 2, sy - 2, sx + 15, sy + 14, 5, 0xFF8F7C98);
            }
            UiDraw.rounded(g, sx - 1, sy - 1, sx + 14, sy + 13, 4, 0xFFFFFFFF);
            UiDraw.rounded(g, sx, sy, sx + 13, sy + 12, 3, 0xFF000000 | color);
        }
        boolean h1 = inRect(mouseX, mouseY, ex + 10, ey + 98, ex + 86, ey + 114);
        boolean h2 = inRect(mouseX, mouseY, ex + 90, ey + 98, ex + 166, ey + 114);
        UiDraw.rounded(g, ex + 10, ey + 98, ex + 86, ey + 114, 6, h1 ? 0xFFE58FB0 : 0xFF000000 | MapSettings.frame);
        Component ok = Component.translatable("gui.fateapocrypha.map.mark_ok");
        g.drawString(font, ok, ex + 48 - font.width(ok) / 2, ey + 102, 0xFFFFFFFF, false);
        UiDraw.rounded(g, ex + 90, ey + 98, ex + 166, ey + 114, 6, h2 ? 0xFFD9CDE8 : 0xFFEADFF3);
        Component other = Component.translatable(editingNew ? "gui.fateapocrypha.map.mark_cancel"
                : "gui.fateapocrypha.map.mark_delete");
        g.drawString(font, other, ex + 128 - font.width(other) / 2, ey + 102, INK, false);
    }

    private boolean clickEdit(double mx, double my) {
        int ex = editX();
        int ey = editY();
        if (!inRect(mx, my, ex, ey, ex + EDIT_W, ey + EDIT_H)) {
            commitEdit();   // клик вне панели сохраняет метку
            return true;
        }
        for (int i = 0; i < MapMarks.ICONS.length; i++) {
            if (inRect(mx, my, ex + 12 + 30 * i, ey + 46, ex + 36 + 30 * i, ey + 68)) {
                editing.icon = i;
                return true;
            }
        }
        for (int i = 0; i < MapSettings.PALETTE.length; i++) {
            if (inRect(mx, my, ex + 13 + 15 * i, ey + 74, ex + 26 + 15 * i, ey + 86)) {
                editing.color = MapSettings.PALETTE[i];
                return true;
            }
        }
        if (inRect(mx, my, ex + 10, ey + 98, ex + 86, ey + 114)) {
            commitEdit();
        } else if (inRect(mx, my, ex + 90, ey + 98, ex + 166, ey + 114)) {
            if (editingNew) {
                cancelEdit();
            } else {
                MapMarks.list(Minecraft.getInstance()).remove(editing);
                MapMarks.save(Minecraft.getInstance());
                cancelEdit();
            }
        }
        return true;
    }

    private void commitEdit() {
        if (editing == null) {
            return;
        }
        Minecraft mc = Minecraft.getInstance();
        String n = nameBuf.toString().trim();
        if (n.isEmpty()) {
            n = Component.translatable("gui.fateapocrypha.map.mark_default").getString() + " "
                    + (MapMarks.list(mc).size() + (editingNew ? 1 : 0));
        }
        editing.name = n;
        if (editingNew) {
            MapMarks.list(mc).add(editing);
        }
        MapMarks.save(mc);
        cancelEdit();
    }

    private void cancelEdit() {
        editing = null;
        editingNew = false;
        nameBuf.setLength(0);
    }

    // ================= список мест: быстрый переход =================
    private record Entry(Component name, double x, double z, int color, int icon) {
    }

    private static final int LIST_W = 160;
    private static final int ROW = 14;

    private List<Entry> entries() {
        List<Entry> out = new ArrayList<>();
        for (Marker m : markers) {
            out.add(new Entry(m.name(), m.location().cx(), m.location().cz(), m.color(), -1));
        }
        for (MapMarks.Mark m : MapMarks.list(Minecraft.getInstance())) {
            out.add(new Entry(Component.literal(m.name), m.x, m.z, m.color, m.icon));
        }
        return out;
    }

    private int listX() {
        return vx1 + 8;
    }

    private int listY() {
        return vy1 + 52;
    }

    private int listRows(int total) {
        return Math.max(1, Math.min(Math.min(11, total), (vy2 - listY() - 34) / ROW));
    }

    private int listHeight(int rows) {
        return 24 + rows * ROW + 6;
    }

    private boolean listContains(double mx, double my) {
        int total = entries().size();
        return inRect(mx, my, listX(), listY(), listX() + LIST_W, listY() + listHeight(listRows(total)));
    }

    private void drawList(GuiGraphics g, int mouseX, int mouseY) {
        List<Entry> es = entries();
        int rows = listRows(es.size());
        listScroll = Math.max(0, Math.min(listScroll, Math.max(0, es.size() - rows)));
        int x = listX();
        int y = listY();
        UiDraw.shadow(g, x, y, x + LIST_W, y + listHeight(rows), 8);
        UiDraw.outlined(g, x, y, x + LIST_W, y + listHeight(rows), 8, 0xFF000000 | MapSettings.frame, 0xF5FFFBF7, 0xF5FFF1F6);
        g.drawString(font, Component.translatable("gui.fateapocrypha.map.places"), x + 10, y + 8, INK, false);
        for (int i = 0; i < rows; i++) {
            Entry e = es.get(listScroll + i);
            int ry = y + 22 + i * ROW;
            boolean hover = inRect(mouseX, mouseY, x + 5, ry, x + LIST_W - 5, ry + ROW - 1);
            if (hover) {
                UiDraw.rounded(g, x + 5, ry, x + LIST_W - 5, ry + ROW - 1, 4, 0xFFFFE0EC);
            }
            if (e.icon() >= 0) {
                MapMarks.drawIcon(g, e.icon(), x + 15, ry + 6, e.color());
            } else {
                UiDraw.circle(g, x + 15, ry + 6, 4, 0xFFFFFFFF);
                UiDraw.circle(g, x + 15, ry + 6, 3, 0xFF000000 | e.color());
            }
            g.drawString(font, font.plainSubstrByWidth(e.name().getString(), LIST_W - 40), x + 26, ry + 3, INK, false);
        }
        if (es.size() > rows) {
            if (listScroll > 0) {
                g.fill(x + LIST_W - 12, y + 10, x + LIST_W - 8, y + 11, INK_SOFT);
            }
            if (listScroll + rows < es.size()) {
                g.fill(x + LIST_W - 12, y + listHeight(rows) - 6, x + LIST_W - 8, y + listHeight(rows) - 5, INK_SOFT);
            }
        }
    }

    private boolean clickList(double mx, double my) {
        List<Entry> es = entries();
        int rows = listRows(es.size());
        int y = listY();
        for (int i = 0; i < rows; i++) {
            int ry = y + 22 + i * ROW;
            if (inRect(mx, my, listX() + 5, ry, listX() + LIST_W - 5, ry + ROW - 1) && listScroll + i < es.size()) {
                Entry e = es.get(listScroll + i);
                flyTo(e.x(), e.z(), Math.max(zoomTarget, 0.45F));
                return true;
            }
        }
        return true;
    }

    // ================= план замка по этажам =================
    private void toggleCastle() {
        if (castleView) {
            leaveCastle();
            return;
        }
        Minecraft mc = Minecraft.getInstance();
        prevCx = cx;
        prevCz = cz;
        prevZoom = zoomTarget;
        castleView = true;
        popup = false;
        listOpen = false;
        LocalPlayer player = mc.player;
        castleFloor = (player != null && inTrifas() && CastlePlan.insideCastle(player.getX(), player.getZ()))
                ? CastlePlan.floorOf(player.getY()) : 2;
        float fit = (float) Math.min((vx2 - vx1 - 130) / (CastlePlan.BW + 12.0D), (vy2 - vy1 - 10) / (CastlePlan.BD + 12.0D));
        flyTo(CastlePlan.BX + CastlePlan.BW / 2.0D - 58.0D / Math.max(0.2F, fit), CastlePlan.BZ + CastlePlan.BD / 2.0D, fit);
    }

    private void leaveCastle() {
        if (!castleView) {
            return;
        }
        castleView = false;
        flyTo(prevCx, prevCz, prevZoom);
    }

    private static int shade(int rgb, float f) {
        int r = (int) (((rgb >> 16) & 0xFF) * f);
        int gg = (int) (((rgb >> 8) & 0xFF) * f);
        int b = (int) ((rgb & 0xFF) * f);
        return (r << 16) | (gg << 8) | b;
    }

    private void drawCastle(GuiGraphics g, int mouseX, int mouseY) {
        g.fill(vx1, vy1, vx2, vy2, 0xFFF4EEF7);
        if (zoom >= 0.7F) {   // чертёжная сетка через 10 блоков
            double gx0 = Math.floor(toWorldX(vx1) / 10.0D) * 10.0D;
            double gz0 = Math.floor(toWorldZ(vy1) / 10.0D) * 10.0D;
            for (double gx = gx0; gx <= toWorldX(vx2); gx += 10.0D) {
                int sx = (int) toScreenX(gx);
                g.fill(sx, vy1, sx + 1, vy2, 0x14B9A6EC);
            }
            for (double gz = gz0; gz <= toWorldZ(vy2); gz += 10.0D) {
                int sy = (int) toScreenY(gz);
                g.fill(vx1, sy, vx2, sy + 1, 0x14B9A6EC);
            }
        }
        int ax1 = (int) toScreenX(CastlePlan.BX);
        int ay1 = (int) toScreenY(CastlePlan.BZ);
        int ax2 = (int) toScreenX(CastlePlan.BX + CastlePlan.BW);
        int ay2 = (int) toScreenY(CastlePlan.BZ + CastlePlan.BD);
        g.fill(ax1, ay1, ax2, ay2, 0xFFFFFBF7);
        int wall = 0xFF000000 | MapSettings.frame;
        g.fill(ax1 - 1, ay1 - 1, ax2 + 1, ay1 + 1, wall);
        g.fill(ax1 - 1, ay2 - 1, ax2 + 1, ay2 + 1, wall);
        g.fill(ax1 - 1, ay1, ax1 + 1, ay2, wall);
        g.fill(ax2 - 1, ay1, ax2 + 1, ay2, wall);
        double wx = toWorldX(mouseX);
        double wz = toWorldZ(mouseY);
        CastlePlan.Room hovered = null;
        for (CastlePlan.Room r : CastlePlan.floor(castleFloor)) {
            drawRoom(g, r);
            if (r.kind() != CastlePlan.Kind.YARD && r.contains(wx, wz)) {
                hovered = r;
            }
        }
        LocalPlayer player = Minecraft.getInstance().player;
        if (player != null && inTrifas() && CastlePlan.insideCastle(player.getX(), player.getZ())) {
            if (CastlePlan.floorOf(player.getY()) == castleFloor) {
                drawPlayer(g);
            } else {
                UiDraw.circle(g, (int) toScreenX(player.getX()), (int) toScreenY(player.getZ()), 3, 0x66F08AB0);
            }
        }
        if (hovered != null && mouseX >= vx1 && mouseX <= vx2 && mouseY >= vy1 && mouseY <= vy2) {
            Component name = Component.translatable("plan.fateapocrypha." + hovered.id());
            int w = font.width(name);
            int bx = Math.min(mouseX + 10, vx2 - w - 14);
            int by = Math.min(mouseY + 8, vy2 - 20);
            UiDraw.outlined(g, bx, by, bx + w + 10, by + 17, 5, 0xFF000000 | MapSettings.frame, 0xF5FFFBF7, 0xF5FFF1F6);
            g.drawString(font, name, bx + 5, by + 5, INK, false);
        }
    }

    private void drawRoom(GuiGraphics g, CastlePlan.Room r) {
        int col = 0xFF000000 | r.color();
        int border = 0xFF000000 | shade(r.color(), 0.78F);
        if (r.kind() == CastlePlan.Kind.CIRCLE) {
            int cxs = (int) toScreenX(r.x1());
            int cys = (int) toScreenY(r.z1());
            int rad = (int) Math.max(2.0D, r.x2() * zoom);
            UiDraw.circle(g, cxs, cys, rad + 1, border);
            UiDraw.circle(g, cxs, cys, rad, col);
            return;
        }
        int x1 = (int) toScreenX(r.x1());
        int y1 = (int) toScreenY(r.z1());
        int x2 = Math.max(x1 + 2, (int) toScreenX(r.x2() + 1));
        int y2 = Math.max(y1 + 2, (int) toScreenY(r.z2() + 1));
        if (x2 < vx1 || x1 > vx2 || y2 < vy1 || y1 > vy2) {
            return;
        }
        if (r.kind() == CastlePlan.Kind.YARD) {
            g.fill(x1, y1, x2, y2, col);
            return;
        }
        g.fill(x1, y1, x2, y2, border);
        g.fill(x1 + 1, y1 + 1, x2 - 1, y2 - 1, col);
        if (r.kind() == CastlePlan.Kind.STAIRS) {
            for (int yy = y1 + 2; yy < y2 - 1; yy += 3) {
                g.fill(x1 + 1, yy, x2 - 1, yy + 1, border);
            }
            return;
        }
        if (zoom >= 0.5F) {
            String name = Component.translatable("plan.fateapocrypha." + r.id()).getString();
            int tw = font.width(name);
            if (tw + 4 <= x2 - x1 && y2 - y1 >= 10) {
                g.drawString(font, name, (x1 + x2) / 2 - tw / 2, (y1 + y2) / 2 - 4, INK, false);
            }
        }
    }

    private void drawCastleTabs(GuiGraphics g, int mx, int my) {
        int x = vx1 + 8;
        int y = vy1 + 52;
        LocalPlayer player = Minecraft.getInstance().player;
        int playerFloor = player != null && inTrifas() && CastlePlan.insideCastle(player.getX(), player.getZ())
                ? CastlePlan.floorOf(player.getY()) : -1;
        for (int i = CastlePlan.FLOORS - 1; i >= 0; i--) {
            int ty = y + (CastlePlan.FLOORS - 1 - i) * 19;
            boolean sel = i == castleFloor;
            boolean hov = inRect(mx, my, x, ty, x + 104, ty + 17);
            UiDraw.outlined(g, x, ty, x + 104, ty + 17, 6, 0xFF000000 | MapSettings.frame,
                    sel ? 0xFFFFE0EC : hov ? 0xFFFFF3F7 : 0xF2FFFFFF, sel ? 0xFFFFD3E3 : hov ? 0xFFFFEAF1 : 0xF2FFF4F8);
            g.drawString(font, Component.translatable("plan.fateapocrypha.floor." + CastlePlan.FLOOR_KEYS[i]),
                    x + 8, ty + 5, sel ? INK : INK_SOFT, false);
            if (i == playerFloor) {
                UiDraw.circle(g, x + 96, ty + 8, 2, 0xFFF08AB0);
            }
        }
    }

    private boolean clickTabs(double mx, double my) {
        int x = vx1 + 8;
        int y = vy1 + 52;
        for (int i = CastlePlan.FLOORS - 1; i >= 0; i--) {
            int ty = y + (CastlePlan.FLOORS - 1 - i) * 19;
            if (inRect(mx, my, x, ty, x + 104, ty + 17)) {
                castleFloor = i;
                return true;
            }
        }
        return false;
    }

    // ================= кнопки =================
    private int btnX() {
        return vx2 - BTN - 8;
    }

    private int btnY(int index) {
        return vy1 + 8 + index * (BTN + 5);
    }

    private boolean inBtn(double mx, double my, int index) {
        return mx >= btnX() && mx <= btnX() + BTN && my >= btnY(index) && my <= btnY(index) + BTN;
    }

    private static final int BTN_COUNT = 7;

    private void drawButtons(GuiGraphics g, int mouseX, int mouseY) {
        for (int i = 0; i < BTN_COUNT; i++) {
            int x = btnX();
            int y = btnY(i);
            boolean hover = inBtn(mouseX, mouseY, i);
            boolean active = (i == 0 && popup) || (i == 1 && listOpen) || (i == 2 && castleView);
            UiDraw.outlined(g, x, y, x + BTN, y + BTN, 6, 0xFF000000 | MapSettings.frame,
                    hover || active ? 0xFFFFE9F2 : 0xF2FFFFFF, hover || active ? 0xFFFFE0EC : 0xF2FFF4F8);
            int cxb = x + BTN / 2;
            int cyb = y + BTN / 2;
            switch (i) {
                case 0 -> {   // настройки: три ползунка
                    for (int r = 0; r < 3; r++) {
                        g.fill(cxb - 5, cyb - 4 + r * 4, cxb + 5, cyb - 3 + r * 4, INK);
                        UiDraw.circle(g, cxb - 3 + ((r * 5) % 7), cyb - 4 + r * 4, 1, 0xFFF08AB0);
                    }
                }
                case 1 -> {   // список мест
                    for (int r = 0; r < 3; r++) {
                        UiDraw.circle(g, cxb - 4, cyb - 4 + r * 4, 1, 0xFFF08AB0);
                        g.fill(cxb - 1, cyb - 5 + r * 4, cxb + 5, cyb - 4 + r * 4, INK);
                    }
                }
                case 2 -> {   // замок
                    g.fill(cxb - 5, cyb - 2, cxb + 5, cyb + 4, INK);
                    for (int r = -5; r <= 3; r += 3) {
                        g.fill(cxb + r, cyb - 5, cxb + r + 2, cyb - 2, INK);
                    }
                    g.fill(cxb - 1, cyb + 1, cxb + 1, cyb + 4, 0xFFFFE9F2);
                }
                case 3 -> {   // текущее задание
                    UiDraw.diamond(g, cxb, cyb, 4, 0xFFF5C95A);
                    UiDraw.diamond(g, cxb, cyb, 2, 0xFFFFFFFF);
                }
                case 4 -> {   // игрок
                    UiDraw.circle(g, cxb, cyb, 4, INK);
                    UiDraw.circle(g, cxb, cyb, 2, 0xFFFFFFFF);
                    g.fill(cxb, cyb - 7, cxb + 1, cyb - 5, INK);
                    g.fill(cxb, cyb + 5, cxb + 1, cyb + 7, INK);
                    g.fill(cxb - 7, cyb, cxb - 5, cyb + 1, INK);
                    g.fill(cxb + 5, cyb, cxb + 7, cyb + 1, INK);
                }
                case 5 -> {   // приблизить
                    g.fill(cxb - 4, cyb, cxb + 5, cyb + 1, INK);
                    g.fill(cxb, cyb - 4, cxb + 1, cyb + 5, INK);
                }
                default -> g.fill(cxb - 4, cyb, cxb + 5, cyb + 1, INK);   // отдалить
            }
        }
    }

    private void onButton(int i) {
        Minecraft mc = Minecraft.getInstance();
        switch (i) {
            case 0 -> popup = !popup;
            case 1 -> {
                listOpen = !listOpen;
                listScroll = 0;
            }
            case 2 -> toggleCastle();
            case 3 -> {
                StoryLocation quest = questLocation();
                if (quest != null) {
                    leaveCastle();
                    flyTo(quest.cx(), quest.cz(), 0.45F);
                }
            }
            case 4 -> {
                if (mc.player != null && inTrifas()) {
                    leaveCastle();
                    flyTo(mc.player.getX(), mc.player.getZ(), 0.45F);
                }
            }
            case 5 -> zoomBy(1.6D, vcx(), vcy());
            default -> zoomBy(1.0D / 1.6D, vcx(), vcy());
        }
    }

    // ================= панель настроек =================
    private int popX1() {
        return btnX() - 6 - 156;
    }

    private int popY1() {
        return vy1 + 8;
    }

    private int popHeight() {
        return 224;
    }

    private boolean popupContains(double mx, double my) {
        return popup && mx >= popX1() && mx <= popX1() + 156 && my >= popY1() && my <= popY1() + popHeight();
    }

    private int swatchX(int i) {
        return popX1() + 12 + (i % 5) * 28;
    }

    private int swatchY(int row, int i) {
        return popY1() + (row == 0 ? 24 : 70) + (i / 5) * 18;
    }

    private int toggleY(int i) {
        return popY1() + 114 + i * 17;
    }

    private void drawPopup(GuiGraphics g, int mouseX, int mouseY) {
        int x1 = popX1();
        int y1 = popY1();
        UiDraw.shadow(g, x1, y1, x1 + 156, y1 + popHeight(), 9);
        UiDraw.outlined(g, x1, y1, x1 + 156, y1 + popHeight(), 9, 0xFF000000 | MapSettings.frame, 0xFAFFFBF7, 0xFAFFF1F6);
        g.drawString(font, Component.translatable("gui.fateapocrypha.map.outline"), x1 + 12, y1 + 9, INK, false);
        g.drawString(font, Component.translatable("gui.fateapocrypha.map.frame"), x1 + 12, y1 + 55, INK, false);
        for (int row = 0; row < 2; row++) {
            int current = row == 0 ? MapSettings.outline : MapSettings.frame;
            for (int i = 0; i < MapSettings.PALETTE.length; i++) {
                int sx = swatchX(i);
                int sy = swatchY(row, i);
                int color = MapSettings.PALETTE[i];
                boolean selected = color == current;
                if (selected) {
                    UiDraw.rounded(g, sx - 3, sy - 3, sx + 17, sy + 15, 6, 0xFF8F7C98);
                }
                UiDraw.rounded(g, sx - 2, sy - 2, sx + 16, sy + 14, 5, 0xFFFFFFFF);
                UiDraw.rounded(g, sx, sy, sx + 14, sy + 12, 4, 0xFF000000 | color);
            }
        }
        String[] keys = {"contours", "labels", "roads", "grid", "minimap"};
        boolean[] values = {MapSettings.contours, MapSettings.labels, MapSettings.roads, MapSettings.grid, MapSettings.minimap};
        for (int i = 0; i < keys.length; i++) {
            int ty = toggleY(i);
            UiDraw.rounded(g, x1 + 12, ty, x1 + 24, ty + 12, 4, 0xFF000000 | MapSettings.frame);
            UiDraw.rounded(g, x1 + 13, ty + 1, x1 + 23, ty + 11, 3, values[i] ? 0xFF000000 | MapSettings.frame : 0xFFFFFFFF);
            if (values[i]) {
                UiDraw.check(g, x1 + 18, ty + 6, 0xFFFFFFFF);
            }
            g.drawString(font, Component.translatable("gui.fateapocrypha.map." + keys[i]), x1 + 30, ty + 2, INK, false);
        }
        int cy2 = toggleY(5);
        UiDraw.rounded(g, x1 + 12, cy2, x1 + 144, cy2 + 13, 5, 0xFF000000 | MapSettings.frame);
        UiDraw.rounded(g, x1 + 13, cy2 + 1, x1 + 143, cy2 + 12, 4, 0xFFFFFFFF);
        g.drawString(font, Component.translatable("gui.fateapocrypha.map.corner",
                Component.translatable("gui.fateapocrypha.map.corner." + MapSettings.minimapCorner)), x1 + 18, cy2 + 3, INK, false);
    }

    private boolean clickPopup(double mx, double my) {
        for (int row = 0; row < 2; row++) {
            for (int i = 0; i < MapSettings.PALETTE.length; i++) {
                int sx = swatchX(i);
                int sy = swatchY(row, i);
                if (mx >= sx - 2 && mx <= sx + 16 && my >= sy - 2 && my <= sy + 14) {
                    if (row == 0) {
                        MapSettings.outline = MapSettings.PALETTE[i];
                    } else {
                        MapSettings.frame = MapSettings.PALETTE[i];
                    }
                    MapSettings.save();
                    return true;
                }
            }
        }
        for (int i = 0; i < 6; i++) {
            int ty = toggleY(i);
            if (mx >= popX1() + 12 && mx <= popX1() + 144 && my >= ty - 1 && my <= ty + 13) {
                switch (i) {
                    case 0 -> MapSettings.contours = !MapSettings.contours;
                    case 1 -> MapSettings.labels = !MapSettings.labels;
                    case 2 -> MapSettings.roads = !MapSettings.roads;
                    case 3 -> MapSettings.grid = !MapSettings.grid;
                    case 4 -> MapSettings.minimap = !MapSettings.minimap;
                    default -> MapSettings.minimapCorner = (MapSettings.minimapCorner + 1) % 4;
                }
                MapSettings.save();
                return true;
            }
        }
        return true;   // клик внутри панели не двигает карту
    }

    // ================= управление =================
    private void zoomBy(double factor, double screenX, double screenY) {
        float target = (float) Math.max(minZoom(), Math.min(maxZoom(), zoomTarget * factor));
        if (target == zoomTarget) {
            return;
        }
        zoomTarget = target;
        anchoring = true;
        flying = false;
        anchorSx = screenX;
        anchorSy = screenY;
        anchorWx = toWorldX(screenX);
        anchorWz = toWorldZ(screenY);
    }

    private void flyTo(double wx, double wz, float targetZoom) {
        flyX = wx;
        flyZ = wz;
        flying = true;
        anchoring = false;
        zoomTarget = Math.max(minZoom(), Math.min(maxZoom(), targetZoom));
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double delta) {
        if (editing != null || popupContains(mouseX, mouseY)) {
            return true;
        }
        if (listOpen && !castleView && listContains(mouseX, mouseY)) {
            listScroll = Math.max(0, listScroll - (int) Math.signum(delta));
            return true;
        }
        if (mouseX >= vx1 && mouseX <= vx2 && mouseY >= vy1 && mouseY <= vy2) {
            zoomBy(Math.pow(1.22D, delta), mouseX, mouseY);
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, delta);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (editing != null) {
            return button != 0 || clickEdit(mouseX, mouseY);
        }
        if (button == 1) {
            return rightClick(mouseX, mouseY);
        }
        if (button != 0) {
            return super.mouseClicked(mouseX, mouseY, button);
        }
        if (popupContains(mouseX, mouseY)) {
            return clickPopup(mouseX, mouseY);
        }
        if (listOpen && !castleView && listContains(mouseX, mouseY)) {
            return clickList(mouseX, mouseY);
        }
        if (castleView && clickTabs(mouseX, mouseY)) {
            return true;
        }
        for (int i = 0; i < BTN_COUNT; i++) {
            if (inBtn(mouseX, mouseY, i)) {
                onButton(i);
                return true;
            }
        }
        if (mouseX >= vx1 && mouseX <= vx2 && mouseY >= vy1 && mouseY <= vy2) {
            dragging = true;
            flying = false;
            popup = false;
            return true;
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    /** Правая кнопка: новая метка на пустом месте или правка существующей. */
    private boolean rightClick(double mx, double my) {
        if (castleView || popupContains(mx, my) || mx < vx1 || mx > vx2 || my < vy1 || my > vy2
                || !MapTextures.baseReady()) {
            return true;
        }
        Minecraft mc = Minecraft.getInstance();
        MapMarks.Mark existing = markAt(mx, my);
        nameBuf.setLength(0);
        if (existing != null) {
            editing = existing;
            editingNew = false;
            nameBuf.append(existing.name);
        } else {
            int n = MapMarks.list(mc).size();
            editing = new MapMarks.Mark(toWorldX(mx), toWorldZ(my), MapSettings.PALETTE[(n * 3) % MapSettings.PALETTE.length], n % MapMarks.ICONS.length, "");
            editingNew = true;
        }
        popup = false;
        return true;
    }

    @Override
    public boolean charTyped(char c, int modifiers) {
        if (editing != null) {
            if (c >= 32 && c != 127 && nameBuf.length() < 24) {
                nameBuf.append(c);
            }
            return true;
        }
        return super.charTyped(c, modifiers);
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double dx, double dy) {
        if (editing == null && dragging && button == 0) {
            cx -= dx / zoom;
            cz -= dy / zoom;
            anchoring = false;
            clampCenter();
            return true;
        }
        return super.mouseDragged(mouseX, mouseY, button, dx, dy);
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        dragging = false;
        return super.mouseReleased(mouseX, mouseY, button);
    }

    @Override
    public boolean keyPressed(int key, int scanCode, int modifiers) {
        if (editing != null) {
            if (key == GLFW.GLFW_KEY_ENTER || key == GLFW.GLFW_KEY_KP_ENTER) {
                commitEdit();
            } else if (key == GLFW.GLFW_KEY_ESCAPE) {
                cancelEdit();
            } else if (key == GLFW.GLFW_KEY_BACKSPACE && nameBuf.length() > 0) {
                nameBuf.setLength(nameBuf.length() - 1);
            }
            return true;   // клавиши в поле названия не закрывают карту
        }
        if (ClientKeys.MAP.matches(key, scanCode)) {
            onClose();
            return true;
        }
        double step = 80.0D / zoom;
        switch (key) {
            case GLFW.GLFW_KEY_EQUAL, GLFW.GLFW_KEY_KP_ADD -> zoomBy(1.4D, vcx(), vcy());
            case GLFW.GLFW_KEY_MINUS, GLFW.GLFW_KEY_KP_SUBTRACT -> zoomBy(1.0D / 1.4D, vcx(), vcy());
            case GLFW.GLFW_KEY_LEFT -> cx -= step;
            case GLFW.GLFW_KEY_RIGHT -> cx += step;
            case GLFW.GLFW_KEY_UP -> cz -= step;
            case GLFW.GLFW_KEY_DOWN -> cz += step;
            default -> {
                return super.keyPressed(key, scanCode, modifiers);
            }
        }
        flying = false;
        clampCenter();
        return true;
    }
}
