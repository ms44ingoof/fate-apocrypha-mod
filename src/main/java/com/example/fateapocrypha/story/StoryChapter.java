package com.example.fateapocrypha.story;

import java.util.Locale;

/** Главы сюжета. Порядок в enum = порядок прохождения. */
public enum StoryChapter {
    PROLOGUE,
    SUMMONING,
    ROMANIA,
    MILLENNIUM_CASTLE;

    public String titleKey() {
        return "story.fateapocrypha." + name().toLowerCase(Locale.ROOT) + ".title";
    }

    public String objectiveKey() {
        return "story.fateapocrypha." + name().toLowerCase(Locale.ROOT) + ".objective";
    }

    public static StoryChapter byId(int id) {
        StoryChapter[] all = values();
        return all[Math.max(0, Math.min(id, all.length - 1))];
    }
}
