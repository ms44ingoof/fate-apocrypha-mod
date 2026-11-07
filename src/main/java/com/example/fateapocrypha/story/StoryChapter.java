package com.example.fateapocrypha.story;

import java.util.Locale;

/** Главы сюжета. Порядок в enum = порядок прохождения. */
public enum StoryChapter {
    LAB_TANK,
    LAB_WALK,
    CORRIDOR,
    CHIRON,
    ESCAPE,
    FOREST,
    FARM,
    DREAM_FAFNIR,
    INTERLUDE_RED,
    INTERLUDE_SHIRO,
    INTERLUDE_FRANKENSTEIN,
    INTERLUDE_SPARTACUS,
    FRANKEN_LEGACY,
    CELENIKE,
    JACK,
    INTERLUDE_AVICEBRON,
    GOLEM_ADAM,
    INTERLUDE_DARNIC,
    INTERLUDE_GRAIL,
    RESISTANCE,
    NIGHT_TALK,
    DREAM_FAFNIR_2,
    GARDENS_ASSAULT,
    INTERLUDE_ACHILLES,
    KARNA,
    INTERLUDE_SEMIRAMIS,
    INTERLUDE_PUCELLE,
    SHIRO_FINAL,
    GRAIL_INSIDE,
    EPILOGUE,
    TO_BE_CONTINUED;   // после эпилога: свободная игра

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
