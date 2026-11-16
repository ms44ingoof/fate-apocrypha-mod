package com.example.fateapocrypha.entity;

/** Существо с собственной анимацией действия (каст, поклон, атака...). Имя действия синхронизируется с клиентом. */
public interface AnimatedActor {
    /** Имя текущего действия или пустая строка. */
    String getAnim();

    /** Возраст существа (tickCount) в момент начала действия: от него считается прогресс анимации. */
    int getAnimStartAge();
}
