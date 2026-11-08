package com.example.fateapocrypha.client;

import net.minecraft.client.KeyMapping;
import org.lwjgl.glfw.GLFW;

/** Клавиши мода. Пропуск диалога: по умолчанию R (можно сменить в настройках управления). */
public class ClientKeys {
    public static final KeyMapping SKIP_DIALOGUE = new KeyMapping(
            "key.fateapocrypha.skip_dialogue", GLFW.GLFW_KEY_R, "key.categories.fateapocrypha");

    /** Журнал заданий: по умолчанию J. */
    public static final KeyMapping JOURNAL = new KeyMapping(
            "key.fateapocrypha.journal", GLFW.GLFW_KEY_J, "key.categories.fateapocrypha");
}
