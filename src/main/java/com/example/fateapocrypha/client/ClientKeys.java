package com.example.fateapocrypha.client;

import net.minecraft.client.KeyMapping;
import org.lwjgl.glfw.GLFW;

/** Клавиши мода. Пропуск диалога: по умолчанию R (можно сменить в настройках управления). */
public class ClientKeys {
    public static final KeyMapping SKIP_DIALOGUE = new KeyMapping(
            "key.fateapocrypha.skip_dialogue", GLFW.GLFW_KEY_R, "key.categories.fateapocrypha");
}
