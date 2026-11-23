package com.example.fateapocrypha.client;

import net.minecraftforge.common.ForgeConfigSpec;

/** Клиентские настройки мода (файл config/fateapocrypha-client.toml). */
public final class ClientConfig {
    public static final ForgeConfigSpec SPEC;
    /** Рисовать Слуг через GeckoLib (true) или прежней моделью игрока с процедурной анимацией (false). Нужен перезапуск игры. */
    public static final ForgeConfigSpec.BooleanValue GECKO_SERVANTS;

    static {
        ForgeConfigSpec.Builder b = new ForgeConfigSpec.Builder();
        b.push("rendering");
        GECKO_SERVANTS = b.comment("Draw servants with GeckoLib (true) or the classic player model with procedural animation (false). Restart required.")
                .define("geckoServants", true);
        b.pop();
        SPEC = b.build();
    }

    private ClientConfig() {
    }
}
