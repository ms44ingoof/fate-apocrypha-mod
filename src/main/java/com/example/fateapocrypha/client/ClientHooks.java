package com.example.fateapocrypha.client;

import com.example.fateapocrypha.client.screen.SealScreen;
import com.example.fateapocrypha.client.screen.SummonScreen;
import net.minecraft.client.Minecraft;

/** Точки входа в клиентский код (вызываются через DistExecutor). */
public class ClientHooks {
    public static void openSealScreen() {
        Minecraft.getInstance().setScreen(new SealScreen());
    }

    public static void openSummonScreen() {
        Minecraft.getInstance().setScreen(new SummonScreen());
    }
}
