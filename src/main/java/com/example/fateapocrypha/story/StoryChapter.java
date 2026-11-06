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
    INTERLUDE_RED,
    INTERLUDE_SHIRO,
    INTERLUDE_FRANKENSTEIN,
    INTERLUDE_SPARTACUS,
    CELENIKE,
    JACK,
    INTERLUDE_AVICEBRON,
    GOLEM_ADAM,
    INTERLUDE_DARNIC,
    INTERLUDE_GRAIL,
    RESISTANCE,
    TO_BE_CONTINUED;

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
