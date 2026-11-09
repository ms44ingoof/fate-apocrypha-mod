package com.example.fateapocrypha.client;

import com.example.fateapocrypha.story.StoryChapter;

/** Клиентская копия прогресса заданий (приходит с сервера в QuestSyncPacket). */
public class ClientQuestState {
    private static int chapter = 0;
    private static boolean started = false;
    private static boolean seals = false;

    public static void update(int newChapter, boolean newStarted, boolean newSeals) {
        chapter = newChapter;
        started = newStarted;
        seals = newSeals;
    }

    /** Печати Командных заклинаний проявились (показывать их на экране и принимать клавиши). */
    public static boolean hasSeals() {
        return seals;
    }

    public static StoryChapter chapter() {
        return StoryChapter.byId(chapter);
    }

    /** Сцена текущей главы уже идёт (метка "иди сюда" не нужна). */
    public static boolean started() {
        return started;
    }
}
