package com.example.fateapocrypha.client;

import com.example.fateapocrypha.client.screen.GateGuardScreen;
import net.minecraft.client.Minecraft;

/** Открывает окно привратника на клиенте. */
public final class ClientGate {
    private ClientGate() {
    }

    public static void open(int state) {
        Minecraft.getInstance().setScreen(new GateGuardScreen(state));
    }
}
