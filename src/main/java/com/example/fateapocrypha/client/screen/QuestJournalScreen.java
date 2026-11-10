package com.example.fateapocrypha.client.screen;

import com.example.fateapocrypha.client.ClientKeys;
import com.example.fateapocrypha.client.ClientQuestState;
import com.example.fateapocrypha.client.UiDraw;
import com.example.fateapocrypha.story.StoryChapter;
import com.example.fateapocrypha.story.StoryLocation;
import com.example.fateapocrypha.story.StoryRewards;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

/** Журнал заданий: скруглённые панели, список глав слева, описание, место и награды справа. */
public class QuestJournalScreen extends Screen {
    private static final int GOLD = 0xFFE6C25A;
    private static final int GREEN = 0xFF7BD389;
    private static final int ROW_H = 38;
    private static final int ROW_STEP = 43;

    private int selected;
    private int scroll = 0;
    private int px;
    private int py;
    private int pw;
    private int ph;
    private int lw;

    public QuestJournalScreen() {
        super(Component.translatable("gui.fateapocrypha.journal"));
        this.selected = currentIndex();
    }

    private static int currentIndex() {
        StoryChapter current = ClientQuestState.chapter();
        return current == StoryChapter.TO_BE_CONTINUED ? StoryChapter.TO_BE_CONTINUED.ordinal() - 1 : current.ordinal();
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    @Override
    protected void init() {
        pw = Math.min(width - 30, 800);
        ph = Math.min(height - 30, 430);
        px = (width - pw) / 2;
        py = (height - ph) / 2;
        lw = Math.min(268, pw * 36 / 100);
    }

    /** Главы от текущей к первой. */
    private List<StoryChapter> entries() {
        List<StoryChapter> list = new ArrayList<>();
        for (int i = currentIndex(); i >= 0; i--) {
            list.add(StoryChapter.byId(i));
        }
        return list;
    }

    private int visibleRows() {
        return Math.max(1, (ph - 112) / ROW_STEP);
    }

    private int listTop() {
        return py + 88;
    }

    private static boolean inside(double mx, double my, int x1, int y1, int x2, int y2) {
        return mx >= x1 && mx < x2 && my >= y1 && my < y2;
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partial) {
        g.fillGradient(0, 0, width, height, 0xB0070912, 0xD0070912);
        UiDraw.shadow(g, px, py, px + pw, py + ph, 12);
        UiDraw.outlined(g, px, py, px + pw, py + ph, 12, 0x99E6C25A, 0xF41F2538, 0xF4131726);

        renderHeader(g, mouseX, mouseY);
        renderList(g, mouseX, mouseY);
        renderDetails(g, mouseX, mouseY);

        Component keyName = ClientKeys.JOURNAL.getTranslatedKeyMessage();
        Component hint = Component.translatable("gui.fateapocrypha.close", keyName);
        g.drawString(font, hint, px + pw - font.width(hint) - 16, py + ph - 15, 0xFF8A90A8, false);
    }

    private void renderHeader(GuiGraphics g, int mouseX, int mouseY) {
        UiDraw.diamond(g, px + 22, py + 21, 4, GOLD);
        g.drawString(font, Component.translatable("gui.fateapocrypha.journal"), px + 34, py + 16, 0xFFFFFFFF, true);

        // вкладки-«таблетки»
        String[] tabs = {"gui.fateapocrypha.tab_main", "gui.fateapocrypha.tab_side", "gui.fateapocrypha.tab_world"};
        int total = 0;
        int[] widths = new int[tabs.length];
        for (int i = 0; i < tabs.length; i++) {
            widths[i] = font.width(Component.translatable(tabs[i])) + 22;
            total += widths[i] + 6;
        }
        int x = px + pw / 2 - total / 2;
        for (int i = 0; i < tabs.length; i++) {
            boolean active = i == 0;
            boolean hover = inside(mouseX, mouseY, x, py + 10, x + widths[i], py + 30);
            if (active) {
                UiDraw.outlined(g, x, py + 10, x + widths[i], py + 30, 10, 0xFFE6C25A, 0xFF5A4A22, 0xFF3A2F16);
            } else {
                UiDraw.outlined(g, x, py + 10, x + widths[i], py + 30, 10, hover ? 0xFF55607F : 0xFF2E3550,
                        hover ? 0xFF2C3450 : 0xFF232A3E, 0xFF1B2133);
            }
            Component label = Component.translatable(tabs[i]);
            g.drawString(font, label, x + 11, py + 16, active ? 0xFFFFFFFF : 0xFF7A819C, false);
            x += widths[i] + 6;
        }

        // кнопка закрытия
        int cx1 = px + pw - 32;
        boolean hoverClose = inside(mouseX, mouseY, cx1, py + 10, cx1 + 20, py + 30);
        UiDraw.outlined(g, cx1, py + 10, cx1 + 20, py + 30, 10, hoverClose ? 0xFFE6C25A : 0xFF3A4260,
                hoverClose ? 0xFF3A4260 : 0xFF262D44, 0xFF1B2133);
        g.drawCenteredString(font, "x", cx1 + 10, py + 16, hoverClose ? 0xFFFFFFFF : 0xFFB0B6CC);
        UiDraw.fadeLine(g, px + 18, py + 42, pw - 36, GOLD, 0.45F);
    }

    private void renderList(GuiGraphics g, int mouseX, int mouseY) {
        int x = px + 16;
        UiDraw.diamond(g, x + 5, py + 56, 4, GOLD);
        g.drawString(font, Component.translatable("gui.fateapocrypha.main_quests"), x + 16, py + 52, 0xFFFFFFFF, true);

        StoryChapter sel = StoryChapter.byId(selected);
        UiDraw.outlined(g, x, py + 68, x + lw, py + 82, 7, 0x66E6C25A, 0xFF332D1B, 0xFF282314);
        g.drawString(font, Component.translatable(sel.actKey()), x + 9, py + 71, GOLD, false);

        List<StoryChapter> entries = entries();
        int rows = visibleRows();
        scroll = Math.max(0, Math.min(scroll, Math.max(0, entries.size() - rows)));
        int current = currentIndex();
        boolean allDone = StoryChapter.TO_BE_CONTINUED == ClientQuestState.chapter();
        for (int r = 0; r < rows && r + scroll < entries.size(); r++) {
            StoryChapter ch = entries.get(r + scroll);
            int y = listTop() + r * ROW_STEP;
            boolean isSel = ch.ordinal() == selected;
            boolean hover = inside(mouseX, mouseY, x, y, x + lw, y + ROW_H);
            boolean done = ch.ordinal() < current || allDone;

            int border = isSel ? 0xFFE6C25A : hover ? 0xFF55607F : 0xFF2E3550;
            int top = isSel ? 0xFF46557D : hover ? 0xFF363F5E : 0xFF2B3249;
            int bottom = isSel ? 0xFF313C5C : hover ? 0xFF2B3452 : 0xFF222838;
            UiDraw.outlined(g, x, y, x + lw, y + ROW_H, 8, border, top, bottom);
            if (isSel) {
                UiDraw.rounded(g, x + 4, y + 8, x + 7, y + ROW_H - 8, 1, GOLD);
            }

            // значок состояния
            int ix = x + 21;
            int iy = y + ROW_H / 2;
            UiDraw.circle(g, ix, iy, 8, 0xFF151A28);
            if (done) {
                UiDraw.circle(g, ix, iy, 7, 0xFF1F4A31);
                UiDraw.check(g, ix, iy - 1, GREEN);
            } else {
                UiDraw.circle(g, ix, iy, 7, 0xFF4A3D17);
                UiDraw.circle(g, ix, iy, 3, GOLD);
            }

            String title = font.plainSubstrByWidth(Component.translatable(ch.titleKey()).getString(), lw - 52);
            g.drawString(font, title, x + 36, y + 8, 0xFFFFFFFF, true);
            Component status = Component.translatable(done ? "gui.fateapocrypha.done" : "gui.fateapocrypha.current");
            g.drawString(font, status, x + 36, y + 21, done ? GREEN : GOLD, false);
        }
        if (entries.size() > rows) {
            int trackH = rows * ROW_STEP - 5;
            int thumbH = Math.max(18, trackH * rows / entries.size());
            int maxScroll = entries.size() - rows;
            int thumbY = listTop() + (trackH - thumbH) * scroll / Math.max(1, maxScroll);
            UiDraw.rounded(g, x + lw + 5, listTop(), x + lw + 8, listTop() + trackH, 1, 0xFF1C2132);
            UiDraw.rounded(g, x + lw + 5, thumbY, x + lw + 8, thumbY + thumbH, 1, GOLD);
        }
    }

    private void renderDetails(GuiGraphics g, int mouseX, int mouseY) {
        StoryChapter ch = StoryChapter.byId(selected);
        int rx = px + lw + 42;
        int rw = pw - lw - 62;
        int current = currentIndex();
        boolean done = ch.ordinal() < current || StoryChapter.TO_BE_CONTINUED == ClientQuestState.chapter();

        g.pose().pushPose();
        g.pose().translate(rx, py + 52, 0.0F);
        g.pose().scale(1.5F, 1.5F, 1.0F);
        g.drawString(font, Component.translatable(ch.titleKey()), 0, 0, 0xFFFFFFFF, true);
        g.pose().popPose();
        g.drawString(font, Component.translatable(ch.actKey()), rx, py + 73, 0xFFB8A878, false);
        UiDraw.fadeLine(g, rx, py + 86, rw, GOLD, 0.5F);

        // место: «Направляйтесь: ...»
        int y = py + 96;
        StoryLocation location = StoryLocation.forChapter(ch);
        if (location != null) {
            UiDraw.outlined(g, rx, y, rx + rw, y + 24, 8, 0x66E6C25A, 0xFF283048, 0xFF1E2438);
            UiDraw.circle(g, rx + 14, y + 12, 6, GOLD);
            UiDraw.circle(g, rx + 14, y + 12, 2, 0xFF1E2438);
            g.drawString(font, Component.translatable("gui.fateapocrypha.go_to", Component.translatable(location.nameKey())),
                    rx + 28, y + 8, 0xFFFFFFFF, false);
            y += 32;
        }

        // описание в мягкой карточке
        int descTop = y;
        int descBottom = py + ph - 124;
        if (descBottom - descTop > 20) {
            UiDraw.rounded(g, rx, descTop, rx + rw, descBottom, 8, 0x33000000);
            List<FormattedCharSequence> lines = font.split(Component.translatable(ch.descKey()), rw - 20);
            int maxLines = Math.max(1, (descBottom - descTop - 14) / 11);
            for (int i = 0; i < lines.size() && i < maxLines; i++) {
                g.drawString(font, lines.get(i), rx + 10, descTop + 8 + i * 11, 0xFFD6DAE6, false);
            }
        }

        // награды
        int ry = py + ph - 112;
        UiDraw.diamond(g, rx + 4, ry + 4, 3, GOLD);
        g.drawString(font, Component.translatable("gui.fateapocrypha.rewards"), rx + 14, ry, 0xFFB8A878, false);
        StoryRewards.Reward reward = StoryRewards.of(ch);
        List<ItemStack> slots = new ArrayList<>(reward.items());
        if (reward.levels() > 0) {
            slots.add(new ItemStack(Items.EXPERIENCE_BOTTLE, reward.levels()));
        }
        ItemStack hovered = ItemStack.EMPTY;
        for (int i = 0; i < slots.size(); i++) {
            int sx = rx + i * 34;
            int sy = ry + 14;
            boolean hover = inside(mouseX, mouseY, sx, sy, sx + 30, sy + 30);
            UiDraw.outlined(g, sx, sy, sx + 30, sy + 30, 7, hover ? 0xFFE6C25A : 0xFF5A6690,
                    done ? 0xFF2F3A52 : 0xFF3F4B6E, done ? 0xFF222B40 : 0xFF2D3652);
            g.renderItem(slots.get(i), sx + 7, sy + 7);
            g.renderItemDecorations(font, slots.get(i), sx + 7, sy + 7);
            if (hover) {
                hovered = slots.get(i);
            }
        }
        if (slots.isEmpty()) {
            g.drawString(font, Component.translatable("gui.fateapocrypha.no_rewards"), rx, ry + 22, 0xFF6F7690, false);
        }

        // статус
        int by = py + ph - 48;
        int borderColor;
        int topColor;
        int bottomColor;
        Component text;
        if (done) {
            borderColor = 0xFF2F6B47;
            topColor = 0xFF22442F;
            bottomColor = 0xFF193323;
            text = Component.translatable("gui.fateapocrypha.status_done");
        } else if (ch.ordinal() == current && !ClientQuestState.started() && location != null) {
            borderColor = 0xFF8A7430;
            topColor = 0xFF40371C;
            bottomColor = 0xFF302914;
            text = Component.translatable("gui.fateapocrypha.status_goal", Component.translatable(ch.objectiveKey()));
        } else {
            borderColor = 0xFF3E5788;
            topColor = 0xFF263553;
            bottomColor = 0xFF1B2640;
            text = Component.translatable("gui.fateapocrypha.status_progress", Component.translatable(ch.objectiveKey()));
        }
        UiDraw.outlined(g, rx, by, rx + rw, by + 26, 9, borderColor, topColor, bottomColor);
        UiDraw.circle(g, rx + 14, by + 13, 5, done ? GREEN : GOLD);
        List<FormattedCharSequence> status = font.split(text, rw - 40);
        g.drawString(font, status.isEmpty() ? FormattedCharSequence.EMPTY : status.get(0), rx + 28, by + 9, 0xFFFFFFFF, false);

        if (!hovered.isEmpty()) {
            g.renderTooltip(font, hovered, mouseX, mouseY);
        }
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        int cx1 = px + pw - 32;
        if (inside(mouseX, mouseY, cx1, py + 10, cx1 + 20, py + 30)) {
            onClose();
            return true;
        }
        int x = px + 16;
        List<StoryChapter> entries = entries();
        int rows = visibleRows();
        for (int r = 0; r < rows && r + scroll < entries.size(); r++) {
            int y = listTop() + r * ROW_STEP;
            if (inside(mouseX, mouseY, x, y, x + lw, y + ROW_H)) {
                selected = entries.get(r + scroll).ordinal();
                return true;
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double delta) {
        scroll -= (int) Math.signum(delta);
        return true;
    }

    @Override
    public boolean keyPressed(int key, int scanCode, int modifiers) {
        if (ClientKeys.JOURNAL.matches(key, scanCode)) {
            onClose();
            return true;
        }
        return super.keyPressed(key, scanCode, modifiers);
    }
}
