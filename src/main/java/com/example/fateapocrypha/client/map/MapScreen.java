package com.example.fateapocrypha.client.map;

import com.example.fateapocrypha.ModDimensions;
import com.example.fateapocrypha.client.ClientKeys;
import com.example.fateapocrypha.client.ClientQuestState;
import com.example.fateapocrypha.client.UiDraw;
import com.example.fateapocrypha.story.StoryChapter;
import com.example.fateapocrypha.story.StoryLocation;
import com.example.fateapocrypha.world.TrifasRoads;
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
    private final List<Marker> markers = new ArrayList<>();

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
    }

    @Override
    public void removed() {
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
        if (MapTextures.baseReady()) {
            drawTerrain(g);
            if (MapSettings.grid) {
                drawGrid(g);
            }
            if (MapSettings.roads) {
                drawRoads(g);
            }
            drawMarkers(g, mouseX, mouseY);
            drawPlayer(g);
        } else {
            drawLoading(g);
        }
        g.disableScissor();

        drawViewportFrame(g);
        drawCompass(g);
        drawScaleAndCoords(g, mouseX, mouseY);
        drawButtons(g, mouseX, mouseY);
        if (popup) {
            drawPopup(g, mouseX, mouseY);
        }
        if (MapTextures.baseReady() && !popupContains(mouseX, mouseY)) {
            drawHover(g, mouseX, mouseY);
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
        double spacing = 5.0D;
        for (double[] seg : TrifasRoads.segments()) {
            double ax = toScreenX(seg[0]);
            double ay = toScreenY(seg[1]);
            double bx = toScreenX(seg[2]);
            double by = toScreenY(seg[3]);
            if (Math.max(ax, bx) < vx1 || Math.min(ax, bx) > vx2 || Math.max(ay, by) < vy1 || Math.min(ay, by) > vy2) {
                continue;
            }
            double len = Math.hypot(bx - ax, by - ay);
            int n = Math.max(1, (int) (len / spacing));
            for (int i = 0; i <= n; i += 1) {
                if ((i & 1) == 1) {
                    continue;   // пунктир
                }
                double t = i / (double) n;
                int x = (int) (ax + (bx - ax) * t);
                int y = (int) (ay + (by - ay) * t);
                g.fill(x, y + 1, x + dot, y + 1 + dot, 0x55A07C80);
                g.fill(x, y, x + dot, y + dot, 0xF2FFF6E4);
            }
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
            if (m.castle()) {
                drawCastleIcon(g, x, y, m.color());
            } else {
                UiDraw.circle(g, x, y, 5, 0xFFFFFFFF);
                UiDraw.circle(g, x, y, 4, 0xFF000000 | m.color());
                UiDraw.circle(g, x, y, 1, 0xFFFFFFFF);
            }
            if (isQuest) {
                UiDraw.diamond(g, x, y - 12, 3, 0xFFF5C95A);
                UiDraw.diamond(g, x, y - 12, 1, 0xFFFFFFFF);
            }
            if (MapSettings.labels) {
                int tw = font.width(m.name());
                int lx = x - tw / 2;
                int ly = y + 8;
                UiDraw.rounded(g, lx - 4, ly - 2, lx + tw + 4, ly + 10, 5, 0xCCFFFFFF);
                g.drawString(font, m.name(), lx, ly, INK, false);
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

    private void drawButtons(GuiGraphics g, int mouseX, int mouseY) {
        for (int i = 0; i < 5; i++) {
            int x = btnX();
            int y = btnY(i);
            boolean hover = inBtn(mouseX, mouseY, i);
            boolean active = i == 0 && popup;
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
                case 1 -> {   // текущее задание
                    UiDraw.diamond(g, cxb, cyb, 4, 0xFFF5C95A);
                    UiDraw.diamond(g, cxb, cyb, 2, 0xFFFFFFFF);
                }
                case 2 -> {   // игрок
                    UiDraw.circle(g, cxb, cyb, 4, INK);
                    UiDraw.circle(g, cxb, cyb, 2, 0xFFFFFFFF);
                    g.fill(cxb, cyb - 7, cxb + 1, cyb - 5, INK);
                    g.fill(cxb, cyb + 5, cxb + 1, cyb + 7, INK);
                    g.fill(cxb - 7, cyb, cxb - 5, cyb + 1, INK);
                    g.fill(cxb + 5, cyb, cxb + 7, cyb + 1, INK);
                }
                case 3 -> {   // приблизить
                    g.fill(cxb - 4, cyb, cxb + 5, cyb + 1, INK);
                    g.fill(cxb, cyb - 4, cxb + 1, cyb + 5, INK);
                }
                default -> g.fill(cxb - 4, cyb, cxb + 5, cyb + 1, INK);   // отдалить
            }
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
        return 188;
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
        String[] keys = {"contours", "labels", "roads", "grid"};
        boolean[] values = {MapSettings.contours, MapSettings.labels, MapSettings.roads, MapSettings.grid};
        for (int i = 0; i < keys.length; i++) {
            int ty = toggleY(i);
            UiDraw.rounded(g, x1 + 12, ty, x1 + 24, ty + 12, 4, 0xFF000000 | MapSettings.frame);
            UiDraw.rounded(g, x1 + 13, ty + 1, x1 + 23, ty + 11, 3, values[i] ? 0xFF000000 | MapSettings.frame : 0xFFFFFFFF);
            if (values[i]) {
                UiDraw.check(g, x1 + 18, ty + 6, 0xFFFFFFFF);
            }
            g.drawString(font, Component.translatable("gui.fateapocrypha.map." + keys[i]), x1 + 30, ty + 2, INK, false);
        }
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
        for (int i = 0; i < 4; i++) {
            int ty = toggleY(i);
            if (mx >= popX1() + 12 && mx <= popX1() + 144 && my >= ty - 1 && my <= ty + 13) {
                switch (i) {
                    case 0 -> MapSettings.contours = !MapSettings.contours;
                    case 1 -> MapSettings.labels = !MapSettings.labels;
                    case 2 -> MapSettings.roads = !MapSettings.roads;
                    default -> MapSettings.grid = !MapSettings.grid;
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
        if (popupContains(mouseX, mouseY)) {
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
        if (button != 0) {
            return super.mouseClicked(mouseX, mouseY, button);
        }
        if (popupContains(mouseX, mouseY)) {
            return clickPopup(mouseX, mouseY);
        }
        for (int i = 0; i < 5; i++) {
            if (inBtn(mouseX, mouseY, i)) {
                Minecraft mc = Minecraft.getInstance();
                switch (i) {
                    case 0 -> popup = !popup;
                    case 1 -> {
                        StoryLocation quest = questLocation();
                        if (quest != null) {
                            flyTo(quest.cx(), quest.cz(), 0.45F);
                        }
                    }
                    case 2 -> {
                        if (mc.player != null && inTrifas()) {
                            flyTo(mc.player.getX(), mc.player.getZ(), 0.45F);
                        }
                    }
                    case 3 -> zoomBy(1.6D, vcx(), vcy());
                    default -> zoomBy(1.0D / 1.6D, vcx(), vcy());
                }
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

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double dx, double dy) {
        if (dragging && button == 0) {
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
