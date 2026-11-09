package com.example.fateapocrypha.client;

import net.minecraft.client.KeyMapping;
import org.lwjgl.glfw.GLFW;

/** Клавиши мода. Пропуск диалога: по умолчанию R (можно сменить в настройках управления). */
public class ClientKeys {
    public static final KeyMapping SKIP_DIALOGUE = new KeyMapping(
            "key.fateapocrypha.skip_dialogue", GLFW.GLFW_KEY_R, "key.categories.fateapocrypha");

    /** Три приказа Командных заклинаний: держать клавишу, чтобы применить. */
    public static final KeyMapping SEAL_TELEPORT = new KeyMapping(
            "key.fateapocrypha.seal_teleport", GLFW.GLFW_KEY_Z, "key.categories.fateapocrypha");
    public static final KeyMapping SEAL_BUFF = new KeyMapping(
            "key.fateapocrypha.seal_buff", GLFW.GLFW_KEY_X, "key.categories.fateapocrypha");
    public static final KeyMapping SEAL_PHANTASM = new KeyMapping(
            "key.fateapocrypha.seal_phantasm", GLFW.GLFW_KEY_C, "key.categories.fateapocrypha");

    /** Журнал заданий: по умолчанию J. */
    public static final KeyMapping JOURNAL = new KeyMapping(
            "key.fateapocrypha.journal", GLFW.GLFW_KEY_J, "key.categories.fateapocrypha");
}
