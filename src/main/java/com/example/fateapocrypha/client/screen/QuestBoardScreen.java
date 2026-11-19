package com.example.fateapocrypha.client.screen;

import com.example.fateapocrypha.client.ClientSideQuests;
import com.example.fateapocrypha.client.UiDraw;
import com.example.fateapocrypha.network.BoardActionPacket;
import com.example.fateapocrypha.network.ModNetwork;
import com.example.fateapocrypha.story.SideBoard;
import com.example.fateapocrypha.story.SideQuest;
import com.example.fateapocrypha.story.SideQuestManager;
import com.example.fateapocrypha.story.StoryRewards;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

/**
 * Окно доски объявлений: деревянный щит с приколотыми листками-заданиями. Слева листки, справа выбранное задание
 * с описанием, целью, наградой и кнопкой «Взять» или «Сдать».
 */
public class QuestBoardScreen extends Screen {
    private static final int WOOD_BORDER = 0xFF9A7545;
    private static final int WOOD_TOP = 0xFF5A4128;
    private static final int WOOD_BOTTOM = 0xFF3F2C1A;
    private static final int PAPER = 0xFFEADFC0;
    private static final int PAPER_SEL = 0xFFF6EDCF;
    private static final int PAPER_EDGE = 0xFFB59F6E;
    private static final int INK = 0xFF3B2F22;
    private static final int INK_SOFT = 0xFF7A6A50;
    private static final int NOTE_H = 44;
    private static final int NOTE_STEP = 52;

    private final SideBoard board;
    private int selected = 0;
    private int px;
    private int py;
    private int pw;
    private int ph;
    private int lw;

    public QuestBoardScreen(SideBoard board) {
        super(Component.translatable(board.nameKey()));
        this.board = board;
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    @Override
    protected void init() {
        pw = Math.min(width - 30, 640);
        ph = Math.min(height - 30, 340);
        px = (width - pw) / 2;
        py = (height - ph) / 2;
        lw = Math.min(220, pw * 38 / 100);
    }

    /** Задания, которые видны на этой доске: сдать сюда, новые, идущие, выполненные. */
    private List<SideQuest> entries() {
        List<SideQuest> list = new ArrayList<>();
        int[] order = {SideQuestManager.READY, SideQuestManager.ACTIVE, SideQuestManager.AVAILABLE, SideQuestManager.DONE};
        for (int st : order) {
            for (SideQuest q : SideQuest.values()) {
                if (ClientSideQuests.state(q) != st) {
                    continue;
                }
                boolean deliverHere = q.type() == SideQuest.Type.DELIVER && q.target() == board && st == SideQuestManager.ACTIVE;
                if (q.giver() == board || deliverHere) {
                    list.add(q);
                }
            }
        }
        return list;
    }

    private static boolean inside(double mx, double my, int x1, int y1, int x2, int y2) {
        return mx >= x1 && mx < x2 && my >= y1 && my < y2;
    }

    /** Что можно сделать с заданием здесь: 0 ничего, 1 взять, 2 сдать. */
    private int action(SideQuest q) {
        int st = ClientSideQuests.state(q);
        if (q.type() == SideQuest.Type.DELIVER && q.target() == board && st == SideQuestManager.ACTIVE) {
            return 2;
        }
        if (q.giver() != board) {
            return 0;
        }
        if (st == SideQuestManager.AVAILABLE) {
            return 1;
        }
        if (st == SideQuestManager.READY) {
            return 2;
        }
        if (st == SideQuestManager.ACTIVE && q.type() == SideQuest.Type.FETCH && ClientSideQuests.have(q) >= q.count()) {
            return 2;
        }
        return 0;
    }

    private Component buttonText(SideQuest q) {
        int st = ClientSideQuests.state(q);
        int act = action(q);
        if (act == 1) {
            return Component.translatable("gui.fateapocrypha.board_take");
        }
        if (act == 2) {
            return Component.translatable(q.type() == SideQuest.Type.DELIVER ? "gui.fateapocrypha.board_deliver"
                    : "gui.fateapocrypha.board_turn_in");
        }
        if (st == SideQuestManager.DONE) {
            return Component.translatable("gui.fateapocrypha.done");
        }
        if (st == SideQuestManager.ACTIVE && q.type() == SideQuest.Type.FETCH) {
            return Component.translatable("gui.fateapocrypha.board_missing");
        }
        return Component.translatable("gui.fateapocrypha.side_state.active");
    }

    // ---------- отрисовка ----------
    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partial) {
        g.fillGradient(0, 0, width, height, 0xB0070912, 0xD0070912);
        UiDraw.shadow(g, px, py, px + pw, py + ph, 12);
        UiDraw.outlined(g, px, py, px + pw, py + ph, 12, WOOD_BORDER, WOOD_TOP, WOOD_BOTTOM);
        // доски щита: тонкие вертикальные швы
        for (int x = px + 70; x < px + pw - 10; x += 70) {
            g.fill(x, py + 8, x + 1, py + ph - 8, 0x33000000);
        }
        // гвозди по углам
        for (int[] n : new int[][] {{12, 12}, {pw - 13, 12}, {12, ph - 13}, {pw - 13, ph - 13}}) {
            UiDraw.circle(g, px + n[0], py + n[1], 3, 0xFF2A1E12);
            UiDraw.circle(g, px + n[0], py + n[1], 2, 0xFFB8A27A);
        }

        Component title = Component.translatable(board.nameKey());
        g.pose().pushPose();
        g.pose().translate(px + pw / 2.0F, py + 14, 0.0F);
        g.pose().scale(1.4F, 1.4F, 1.0F);
        g.drawCenteredString(font, title, 0, 0, 0xFFF2E2B8);
        g.pose().popPose();
        UiDraw.fadeLine(g, px + 24, py + 36, pw - 48, 0xFFE6C25A, 0.5F);

        List<SideQuest> entries = entries();
        if (entries.isEmpty()) {
            g.drawCenteredString(font, Component.translatable("gui.fateapocrypha.board_empty"), px + pw / 2, py + ph / 2, 0xFFD8C9A0);
            return;
        }
        selected = Math.max(0, Math.min(selected, entries.size() - 1));
        renderNotes(g, entries, mouseX, mouseY);
        renderDetails(g, entries.get(selected), mouseX, mouseY);
    }

    private void renderNotes(GuiGraphics g, List<SideQuest> entries, int mouseX, int mouseY) {
        int x = px + 20;
        for (int i = 0; i < entries.size() && 48 + (i + 1) * NOTE_STEP <= ph; i++) {
            SideQuest q = entries.get(i);
            int st = ClientSideQuests.state(q);
            int y = py + 48 + i * NOTE_STEP;
            boolean sel = i == selected;
            boolean hover = inside(mouseX, mouseY, x, y, x + lw, y + NOTE_H);
            UiDraw.outlined(g, x, y, x + lw, y + NOTE_H, 3, sel ? 0xFFE6C25A : PAPER_EDGE, sel || hover ? PAPER_SEL : PAPER, PAPER);
            // кнопка-булавка
            UiDraw.circle(g, x + lw / 2, y + 4, 3, 0xFF9E2B2B);
            UiDraw.circle(g, x + lw / 2 - 1, y + 3, 1, 0xFFE58A8A);
            String name = font.plainSubstrByWidth(Component.translatable(q.titleKey()).getString(), lw - 16);
            g.drawString(font, name, x + 8, y + 12, INK, false);
            String stateKey = switch (st) {
                case SideQuestManager.AVAILABLE -> "gui.fateapocrypha.side_state.available";
                case SideQuestManager.ACTIVE -> "gui.fateapocrypha.side_state.active";
                case SideQuestManager.READY -> "gui.fateapocrypha.side_state.ready";
                default -> "gui.fateapocrypha.done";
            };
            int color = st == SideQuestManager.DONE ? 0xFF3F7A4C : st == SideQuestManager.AVAILABLE ? 0xFF9A6B10 : 0xFF3E5788;
            g.drawString(font, Component.translatable(stateKey), x + 8, y + 25, color, false);
            g.drawString(font, Component.translatable("gui.fateapocrypha.side_type." + q.type().name().toLowerCase()),
                    x + lw - 8 - font.width(Component.translatable("gui.fateapocrypha.side_type." + q.type().name().toLowerCase())),
                    y + 25, INK_SOFT, false);
        }
    }

    private void renderDetails(GuiGraphics g, SideQuest q, int mouseX, int mouseY) {
        int rx = px + lw + 40;
        int rw = pw - lw - 60;
        int top = py + 48;
        int bottom = py + ph - 20;
        UiDraw.outlined(g, rx, top, rx + rw, bottom, 3, PAPER_EDGE, PAPER_SEL, PAPER);
        UiDraw.circle(g, rx + rw / 2, top + 4, 3, 0xFF9E2B2B);
        UiDraw.circle(g, rx + rw / 2 - 1, top + 3, 1, 0xFFE58A8A);

        g.pose().pushPose();
        g.pose().translate(rx + 12, top + 14, 0.0F);
        g.pose().scale(1.3F, 1.3F, 1.0F);
        g.drawString(font, Component.translatable(q.titleKey()), 0, 0, INK, false);
        g.pose().popPose();
        g.fill(rx + 12, top + 32, rx + rw - 12, top + 33, PAPER_EDGE);

        // описание
        List<FormattedCharSequence> lines = font.split(Component.translatable(q.descKey()), rw - 24);
        int textBottom = bottom - 108;
        int maxLines = Math.max(1, (textBottom - (top + 40)) / 11);
        for (int i = 0; i < lines.size() && i < maxLines; i++) {
            g.drawString(font, lines.get(i), rx + 12, top + 40 + i * 11, INK, false);
        }

        // цель
        int oy = bottom - 100;
        g.drawString(font, Component.translatable("gui.fateapocrypha.board_goal"), rx + 12, oy, INK_SOFT, false);
        List<FormattedCharSequence> goal = font.split(goalText(q), rw - 24);
        for (int i = 0; i < goal.size() && i < 2; i++) {
            g.drawString(font, goal.get(i), rx + 12, oy + 11 + i * 11, 0xFF6B4A12, false);
        }

        // награды
        int ry = bottom - 66;
        g.drawString(font, Component.translatable("gui.fateapocrypha.rewards"), rx + 12, ry, INK_SOFT, false);
        StoryRewards.Reward reward = q.reward();
        List<ItemStack> slots = new ArrayList<>(reward.items());
        if (reward.levels() > 0) {
            slots.add(new ItemStack(Items.EXPERIENCE_BOTTLE, reward.levels()));
        }
        ItemStack hovered = ItemStack.EMPTY;
        for (int i = 0; i < slots.size(); i++) {
            int sx = rx + 12 + i * 28;
            int sy = ry + 11;
            boolean hover = inside(mouseX, mouseY, sx, sy, sx + 24, sy + 24);
            UiDraw.outlined(g, sx, sy, sx + 24, sy + 24, 4, hover ? 0xFFE6C25A : PAPER_EDGE, 0xFFD9CBA5, 0xFFCDBD94);
            g.renderItem(slots.get(i), sx + 4, sy + 4);
            g.renderItemDecorations(font, slots.get(i), sx + 4, sy + 4);
            if (hover) {
                hovered = slots.get(i);
            }
        }

        // кнопка
        int bx1 = rx + rw - 12 - 120;
        int by1 = bottom - 30;
        int act = action(q);
        boolean enabled = act != 0;
        boolean hover = enabled && inside(mouseX, mouseY, bx1, by1, bx1 + 120, by1 + 22);
        int border = enabled ? (hover ? 0xFFE6C25A : 0xFF8A7430) : 0xFF7A7058;
        int topColor = enabled ? (hover ? 0xFF6E5A2A : 0xFF584822) : 0xFF8F8566;
        int bottomColor = enabled ? (hover ? 0xFF54431C : 0xFF40351A) : 0xFF7A7058;
        UiDraw.outlined(g, bx1, by1, bx1 + 120, by1 + 22, 6, border, topColor, bottomColor);
        Component label = buttonText(q);
        g.drawCenteredString(font, label, bx1 + 60, by1 + 7, enabled ? 0xFFFFF3CF : 0xFFD9D0B8);

        if (!hovered.isEmpty()) {
            g.renderTooltip(font, hovered, mouseX, mouseY);
        }
    }

    private Component goalText(SideQuest q) {
        int st = ClientSideQuests.state(q);
        if (st == SideQuestManager.DONE) {
            return Component.translatable("gui.fateapocrypha.status_done");
        }
        if (st == SideQuestManager.AVAILABLE) {
            return Component.translatable("gui.fateapocrypha.board_goal_new");
        }
        switch (q.type()) {
            case FETCH:
                return Component.translatable("side.fateapocrypha.obj.fetch", new ItemStack(q.item()).getHoverName(),
                        Math.min(ClientSideQuests.have(q), q.count()), q.count());
            case EXPLORE:
                if (st == SideQuestManager.READY) {
                    return Component.translatable("side.fateapocrypha.obj.return", Component.translatable(q.giver().nameKey()));
                }
                int[] p = q.explorePoint();
                return Component.translatable("side.fateapocrypha.obj.explore", p[0], p[1]);
            default:
                return Component.translatable("side.fateapocrypha.obj.deliver", Component.translatable(q.target().nameKey()));
        }
    }

    // ---------- ввод ----------
    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        List<SideQuest> entries = entries();
        int x = px + 20;
        for (int i = 0; i < entries.size() && 48 + (i + 1) * NOTE_STEP <= ph; i++) {
            int y = py + 48 + i * NOTE_STEP;
            if (inside(mouseX, mouseY, x, y, x + lw, y + NOTE_H)) {
                selected = i;
                return true;
            }
        }
        if (!entries.isEmpty()) {
            SideQuest q = entries.get(Math.max(0, Math.min(selected, entries.size() - 1)));
            int rx = px + lw + 40;
            int rw = pw - lw - 60;
            int bx1 = rx + rw - 12 - 120;
            int by1 = py + ph - 20 - 30;
            int act = action(q);
            if (act != 0 && inside(mouseX, mouseY, bx1, by1, bx1 + 120, by1 + 22)) {
                ModNetwork.CHANNEL.sendToServer(new BoardActionPacket(board.ordinal(), q.ordinal(), act == 1 ? 0 : 1));
                return true;
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }
}
