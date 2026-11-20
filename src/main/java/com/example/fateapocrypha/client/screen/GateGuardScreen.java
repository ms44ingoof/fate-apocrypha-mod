package com.example.fateapocrypha.client.screen;

import com.example.fateapocrypha.client.UiDraw;
import com.example.fateapocrypha.network.GateActionPacket;
import com.example.fateapocrypha.network.ModNetwork;
import com.example.fateapocrypha.world.CastleGate;
import java.util.List;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;

/** Окно привратника: слова стражника и кнопка «Открыть ворота» или «Закрыть ворота». */
public class GateGuardScreen extends Screen {
    private final int state;
    private int px;
    private int py;
    private int pw;
    private int ph;

    public GateGuardScreen(int state) {
        super(Component.translatable("gate.fateapocrypha.title"));
        this.state = state;
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    @Override
    protected void init() {
        pw = Math.min(width - 30, 340);
        ph = 170;
        px = (width - pw) / 2;
        py = (height - ph) / 2;
    }

    private boolean canAct() {
        return state != CastleGate.STATE_BUSY;
    }

    private boolean wantsOpen() {
        return state == CastleGate.STATE_CLOSED;
    }

    private int bx1() {
        return px + 20;
    }

    private int by1() {
        return py + ph - 42;
    }

    private int bw() {
        return pw - 40 - 90;
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partial) {
        g.fillGradient(0, 0, width, height, 0xB0070912, 0xD0070912);
        UiDraw.shadow(g, px, py, px + pw, py + ph, 12);
        UiDraw.outlined(g, px, py, px + pw, py + ph, 12, 0xFF9A7545, 0xFF4A3622, 0xFF30221A);
        g.drawCenteredString(font, Component.translatable("gate.fateapocrypha.title"), px + pw / 2, py + 14, 0xFFF2E2B8);
        UiDraw.fadeLine(g, px + 24, py + 30, pw - 48, 0xFFE6C25A, 0.5F);

        String speech = state == CastleGate.STATE_OPEN ? "gate.fateapocrypha.speech_open"
                : state == CastleGate.STATE_BUSY ? "gate.fateapocrypha.speech_busy" : "gate.fateapocrypha.speech_closed";
        List<FormattedCharSequence> lines = font.split(Component.translatable(speech), pw - 48);
        for (int i = 0; i < lines.size() && i < 4; i++) {
            g.drawString(font, lines.get(i), px + 24, py + 42 + i * 11, 0xFFE6DCC0, false);
        }
        Component status = Component.translatable(state == CastleGate.STATE_OPEN ? "gate.fateapocrypha.state_open"
                : state == CastleGate.STATE_BUSY ? "gate.fateapocrypha.state_busy" : "gate.fateapocrypha.state_closed");
        g.drawString(font, status, px + 24, py + ph - 62, state == CastleGate.STATE_OPEN ? 0xFF7BD389 : 0xFFF5C95A, false);

        // главная кнопка и «Уйти»
        boolean hover = canAct() && mouseX >= bx1() && mouseX < bx1() + bw() && mouseY >= by1() && mouseY < by1() + 24;
        int border = canAct() ? (hover ? 0xFFE6C25A : 0xFF8A7430) : 0xFF6A6252;
        int top = canAct() ? (hover ? 0xFF6E5A2A : 0xFF584822) : 0xFF4A4538;
        int bottom = canAct() ? (hover ? 0xFF54431C : 0xFF40351A) : 0xFF3A362C;
        UiDraw.outlined(g, bx1(), by1(), bx1() + bw(), by1() + 24, 7, border, top, bottom);
        Component label = Component.translatable(canAct() ? (wantsOpen() ? "gate.fateapocrypha.open" : "gate.fateapocrypha.close")
                : "gate.fateapocrypha.wait");
        g.drawCenteredString(font, label, bx1() + bw() / 2, by1() + 8, canAct() ? 0xFFFFF3CF : 0xFFB8B09A);

        int lx = bx1() + bw() + 10;
        boolean hoverLeave = mouseX >= lx && mouseX < lx + 70 && mouseY >= by1() && mouseY < by1() + 24;
        UiDraw.outlined(g, lx, by1(), lx + 70, by1() + 24, 7, hoverLeave ? 0xFFE6C25A : 0xFF6A5A44, 0xFF3A2C1E, 0xFF2A2016);
        g.drawCenteredString(font, Component.translatable("gate.fateapocrypha.leave"), lx + 35, by1() + 8, 0xFFE6DCC0);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (canAct() && mouseX >= bx1() && mouseX < bx1() + bw() && mouseY >= by1() && mouseY < by1() + 24) {
            ModNetwork.CHANNEL.sendToServer(new GateActionPacket(wantsOpen()));
            onClose();
            return true;
        }
        int lx = bx1() + bw() + 10;
        if (mouseX >= lx && mouseX < lx + 70 && mouseY >= by1() && mouseY < by1() + 24) {
            onClose();
            return true;
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }
}
