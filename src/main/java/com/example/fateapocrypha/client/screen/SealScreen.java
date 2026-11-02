package com.example.fateapocrypha.client.screen;

import com.example.fateapocrypha.entity.ServantEntity;
import com.example.fateapocrypha.network.ModNetwork;
import com.example.fateapocrypha.network.SealActionPacket;
import com.example.fateapocrypha.servant.ServantActions;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.chat.Component;

/** Меню Командного заклинания. */
public class SealScreen extends Screen {
    private Button teleportButton;
    private Button buffButton;
    private Button phantasmButton;

    public SealScreen() {
        super(Component.translatable("screen.fateapocrypha.seal.title"));
    }

    @Override
    protected void init() {
        int x = this.width / 2 - 100;
        int y = this.height / 2 - 35;
        teleportButton = addRenderableWidget(Button.builder(
                Component.translatable("screen.fateapocrypha.seal.teleport"),
                b -> send(ServantActions.ACTION_TELEPORT)).bounds(x, y, 200, 20).build());
        buffButton = addRenderableWidget(Button.builder(
                Component.translatable("screen.fateapocrypha.seal.buff"),
                b -> send(ServantActions.ACTION_BUFF)).bounds(x, y + 28, 200, 20).build());
        phantasmButton = addRenderableWidget(Button.builder(
                Component.translatable("screen.fateapocrypha.seal.phantasm"),
                b -> send(ServantActions.ACTION_PHANTASM)).bounds(x, y + 56, 200, 20).build());
    }

    private void send(int action) {
        ModNetwork.CHANNEL.sendToServer(new SealActionPacket(action));
        this.onClose();
    }

    private ServantEntity findOwnServant() {
        LocalPlayer player = Minecraft.getInstance().player;
        if (player == null || Minecraft.getInstance().level == null) {
            return null;
        }
        List<ServantEntity> servants = Minecraft.getInstance().level.getEntitiesOfClass(
                ServantEntity.class, player.getBoundingBox().inflate(256.0D), s -> s.isOwnedBy(player));
        return servants.isEmpty() ? null : servants.get(0);
    }

    private static Component label(String key, int remainingTicks) {
        Component base = Component.translatable(key);
        if (remainingTicks <= 0) {
            return base;
        }
        return base.copy().append(Component.translatable("screen.fateapocrypha.seal.cooldown",
                (remainingTicks + 19) / 20));
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        ServantEntity servant = findOwnServant();
        int buff = servant == null ? 0 : servant.getBuffRemaining();
        int phantasm = servant == null ? 0 : servant.getPhantasmRemaining();
        buffButton.setMessage(label("screen.fateapocrypha.seal.buff", buff));
        buffButton.active = buff <= 0;
        phantasmButton.setMessage(label("screen.fateapocrypha.seal.phantasm", phantasm));
        phantasmButton.active = phantasm <= 0;

        this.renderBackground(graphics);
        graphics.drawCenteredString(this.font, this.title, this.width / 2, this.height / 2 - 60, 0xFFFFFF);
        super.render(graphics, mouseX, mouseY, partialTick);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
