package com.example.fateapocrypha.client.screen;

import com.example.fateapocrypha.network.ModNetwork;
import com.example.fateapocrypha.network.SummonPacket;
import com.example.fateapocrypha.servant.ServantType;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

/** Меню катализатора: выбор Слуги для призыва. */
public class SummonScreen extends Screen {
    public SummonScreen() {
        super(Component.translatable("screen.fateapocrypha.summon.title"));
    }

    @Override
    protected void init() {
        ServantType[] types = ServantType.values();
        int x = this.width / 2 - 100;
        int y = this.height / 2 - (types.length * 28) / 2 + 8;
        for (int i = 0; i < types.length; i++) {
            final int id = i;
            addRenderableWidget(Button.builder(Component.translatable(types[i].nameKey()),
                    b -> {
                        ModNetwork.CHANNEL.sendToServer(new SummonPacket(id));
                        this.onClose();
                    }).bounds(x, y + i * 28, 200, 20).build());
        }
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        this.renderBackground(graphics);
        graphics.drawCenteredString(this.font, this.title, this.width / 2,
                this.height / 2 - (ServantType.values().length * 28) / 2 - 10, 0xFFFFFF);
        super.render(graphics, mouseX, mouseY, partialTick);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
