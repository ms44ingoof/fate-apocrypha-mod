package com.example.fateapocrypha.client.screen;

import com.example.fateapocrypha.client.ClientKeys;
import com.example.fateapocrypha.client.ClientQuestState;
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

/** Журнал заданий: список глав слева, описание, место и награды справа. */
public class QuestJournalScreen extends Screen {
    private static final int GOLD = 0xFFE6C25A;
    private static final int PANEL = 0xF2171B27;
    private static final int CARD = 0xFF272E42;
    private static final int CARD_SELECTED = 0xFF3A4562;
    private static final int GREEN = 0xFF7BD389;

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
        pw = Math.min(width - 30, 780);
        ph = Math.min(height - 30, 420);
        px = (width - pw) / 2;
        py = (height - ph) / 2;
        lw = Math.min(260, pw * 36 / 100);
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
        return Math.max(1, (ph - 100) / 38);
    }

    private int listTop() {
        return py + 78;
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partial) {
        g.fill(0, 0, width, height, 0xA6080A10);
        g.fill(px, py, px + pw, py + ph, PANEL);
        g.fill(px, py, px + pw, py + 2, GOLD);
        g.fill(px, py + ph - 2, px + pw, py + ph, GOLD);
        g.fill(px, py, px + 1, py + ph, 0x66E6C25A);
        g.fill(px + pw - 1, py, px + pw, py + ph, 0x66E6C25A);

        // шапка
        g.drawString(font, Component.translatable("gui.fateapocrypha.journal"), px + 16, py + 12, GOLD, true);
        String[] tabs = {"gui.fateapocrypha.tab_main", "gui.fateapocrypha.tab_side", "gui.fateapocrypha.tab_world"};
        int tabX = px + pw / 2 - 120;
        for (int i = 0; i < tabs.length; i++) {
            Component label = Component.translatable(tabs[i]);
            int tw = font.width(label);
            g.drawString(font, label, tabX, py + 12, i == 0 ? 0xFFFFFFFF : 0xFF6F7690, false);
            if (i == 0) {
                g.fill(tabX - 2, py + 25, tabX + tw + 2, py + 27, GOLD);
            }
            tabX += tw + 26;
        }
        g.fill(px + 12, py + 34, px + pw - 12, py + 35, 0x55E6C25A);

        renderList(g, mouseX, mouseY);
        renderDetails(g, mouseX, mouseY);

        Component hint = Component.translatable("gui.fateapocrypha.close", ClientKeys.JOURNAL.getTranslatedKeyMessage());
        g.drawString(font, hint, px + pw - font.width(hint) - 14, py + ph - 14, 0xFF8A90A8, false);
    }

    private void renderList(GuiGraphics g, int mouseX, int mouseY) {
        int x = px + 14;
        // заголовок раздела
        diamond(g, x + 4, py + 52, 4, GOLD);
        g.drawString(font, Component.translatable("gui.fateapocrypha.main_quests"), x + 14, py + 48, 0xFFFFFFFF, true);

        StoryChapter sel = StoryChapter.byId(selected);
        g.fill(x, py + 62, x + lw, py + 74, 0xFF2E2A1C);
        g.fill(x, py + 62, x + 2, py + 74, GOLD);
        g.drawString(font, Component.translatable(sel.actKey()), x + 8, py + 64, GOLD, false);

        List<StoryChapter> entries = entries();
        int rows = visibleRows();
        scroll = Math.max(0, Math.min(scroll, Math.max(0, entries.size() - rows)));
        int current = currentIndex();
        for (int r = 0; r < rows && r + scroll < entries.size(); r++) {
            StoryChapter ch = entries.get(r + scroll);
            int y = listTop() + r * 38;
            boolean isSel = ch.ordinal() == selected;
            boolean hover = mouseX >= x && mouseX <= x + lw && mouseY >= y && mouseY < y + 34;
            g.fill(x, y, x + lw, y + 34, isSel ? CARD_SELECTED : hover ? 0xFF303853 : CARD);
            if (isSel) {
                g.fill(x, y, x + lw, y + 1, 0xFFFFFFFF);
                g.fill(x, y + 33, x + lw, y + 34, 0xFFFFFFFF);
                g.fill(x, y, x + 1, y + 34, 0xFFFFFFFF);
                g.fill(x + lw - 1, y, x + lw, y + 34, 0xFFFFFFFF);
            }
            boolean done = ch.ordinal() < current || StoryChapter.TO_BE_CONTINUED == ClientQuestState.chapter();
            diamond(g, x + 12, y + 17, 5, done ? GREEN : GOLD);
            String title = font.plainSubstrByWidth(Component.translatable(ch.titleKey()).getString(), lw - 40);
            g.drawString(font, title, x + 26, y + 6, 0xFFFFFFFF, true);
            Component status = Component.translatable(done ? "gui.fateapocrypha.done" : "gui.fateapocrypha.current");
            g.drawString(font, status, x + 26, y + 20, done ? GREEN : GOLD, false);
        }
        if (entries.size() > rows) {
            int trackH = rows * 38 - 4;
            int thumbH = Math.max(14, trackH * rows / entries.size());
            int maxScroll = entries.size() - rows;
            int thumbY = listTop() + (trackH - thumbH) * scroll / Math.max(1, maxScroll);
            g.fill(x + lw + 4, listTop(), x + lw + 6, listTop() + trackH, 0xFF22283A);
            g.fill(x + lw + 4, thumbY, x + lw + 6, thumbY + thumbH, GOLD);
        }
    }

    private void renderDetails(GuiGraphics g, int mouseX, int mouseY) {
        StoryChapter ch = StoryChapter.byId(selected);
        int rx = px + lw + 34;
        int rw = pw - lw - 52;
        int current = currentIndex();
        boolean done = ch.ordinal() < current || StoryChapter.TO_BE_CONTINUED == ClientQuestState.chapter();

        // название главы
        g.pose().pushPose();
        g.pose().translate(rx, py + 46, 0.0F);
        g.pose().scale(1.5F, 1.5F, 1.0F);
        g.drawString(font, Component.translatable(ch.titleKey()), 0, 0, 0xFFFFFFFF, true);
        g.pose().popPose();
        g.drawString(font, Component.translatable(ch.actKey()), rx, py + 66, 0xFFB8A878, false);
        g.fill(rx, py + 78, rx + rw, py + 79, 0x66E6C25A);

        // место: "Направляйтесь в ..."
        int y = py + 88;
        StoryLocation location = StoryLocation.forChapter(ch);
        if (location != null) {
            g.fill(rx, y, rx + rw, y + 18, 0xFF222838);
            g.fill(rx, y, rx + 2, y + 18, GOLD);
            diamond(g, rx + 12, y + 9, 4, GOLD);
            g.drawString(font, Component.translatable("gui.fateapocrypha.go_to", Component.translatable(location.nameKey())),
                    rx + 22, y + 5, 0xFFFFFFFF, false);
            y += 26;
        }

        // описание
        List<FormattedCharSequence> lines = font.split(Component.translatable(ch.descKey()), rw);
        int maxLines = Math.max(1, (py + ph - 118 - y) / 11);
        for (int i = 0; i < lines.size() && i < maxLines; i++) {
            g.drawString(font, lines.get(i), rx, y + i * 11, 0xFFD6DAE6, false);
        }

        // награды
        int ry = py + ph - 104;
        g.drawString(font, Component.translatable("gui.fateapocrypha.rewards"), rx, ry, 0xFFB8A878, false);
        StoryRewards.Reward reward = StoryRewards.of(ch);
        List<ItemStack> slots = new ArrayList<>(reward.items());
        if (reward.levels() > 0) {
            slots.add(new ItemStack(Items.EXPERIENCE_BOTTLE, reward.levels()));
        }
        ItemStack hovered = ItemStack.EMPTY;
        for (int i = 0; i < slots.size(); i++) {
            int sx = rx + i * 30;
            int sy = ry + 14;
            g.fill(sx, sy, sx + 26, sy + 26, 0xFF4A5675);
            g.fill(sx + 1, sy + 1, sx + 25, sy + 25, done ? 0xFF2F3A52 : 0xFF3B4764);
            g.renderItem(slots.get(i), sx + 5, sy + 5);
            g.renderItemDecorations(font, slots.get(i), sx + 5, sy + 5);
            if (mouseX >= sx && mouseX < sx + 26 && mouseY >= sy && mouseY < sy + 26) {
                hovered = slots.get(i);
            }
        }
        if (slots.isEmpty()) {
            g.drawString(font, Component.translatable("gui.fateapocrypha.no_rewards"), rx, ry + 20, 0xFF6F7690, false);
        }

        // статус
        int by = py + ph - 44;
        int color;
        Component text;
        if (done) {
            color = 0xFF1F3B2A;
            text = Component.translatable("gui.fateapocrypha.status_done");
        } else if (ch.ordinal() == current && !ClientQuestState.started() && location != null) {
            color = 0xFF3B3320;
            text = Component.translatable("gui.fateapocrypha.status_goal", Component.translatable(ch.objectiveKey()));
        } else {
            color = 0xFF202C42;
            text = Component.translatable("gui.fateapocrypha.status_progress", Component.translatable(ch.objectiveKey()));
        }
        g.fill(rx, by, rx + rw, by + 22, color);
        g.fill(rx, by, rx + 2, by + 22, done ? GREEN : GOLD);
        List<FormattedCharSequence> status = font.split(text, rw - 16);
        g.drawString(font, status.isEmpty() ? FormattedCharSequence.EMPTY : status.get(0), rx + 10, by + 7, 0xFFFFFFFF, false);

        if (!hovered.isEmpty()) {
            g.renderTooltip(font, hovered, mouseX, mouseY);
        }
    }

    private static void diamond(GuiGraphics g, int cx, int cy, int r, int color) {
        g.pose().pushPose();
        g.pose().translate(cx, cy, 0.0F);
        g.pose().mulPose(com.mojang.math.Axis.ZP.rotationDegrees(45.0F));
        g.fill(-r, -r, r, r, color);
        g.pose().popPose();
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        int x = px + 14;
        List<StoryChapter> entries = entries();
        int rows = visibleRows();
        for (int r = 0; r < rows && r + scroll < entries.size(); r++) {
            int y = listTop() + r * 38;
            if (mouseX >= x && mouseX <= x + lw && mouseY >= y && mouseY < y + 34) {
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
