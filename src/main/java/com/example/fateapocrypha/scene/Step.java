package com.example.fateapocrypha.scene;

/** Один шаг сцены. begin() вызывается один раз, update() - каждый тик, пока не вернёт true. */
public abstract class Step {
    public void begin(SceneContext ctx) {
    }

    public abstract boolean update(SceneContext ctx);
}
