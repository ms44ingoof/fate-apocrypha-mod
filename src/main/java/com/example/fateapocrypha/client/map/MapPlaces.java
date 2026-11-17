package com.example.fateapocrypha.client.map;

import com.example.fateapocrypha.client.ClientQuestState;
import com.example.fateapocrypha.story.StoryChapter;
import com.example.fateapocrypha.story.StoryLocation;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;

/** Открытые игроком места мира: для карты, мини-карты и списка быстрого перехода. */
final class MapPlaces {
    record Place(StoryLocation location, Component name, int color, boolean castle) {
        double x() {
            return location.cx();
        }

        double z() {
            return location.cz();
        }
    }

    private MapPlaces() {
    }

    static int color(StoryLocation l) {
        return switch (l) {
            case FOREST -> 0x7FCF9B;
            case RED_CAMP -> 0xF08A8A;
            case CHAPEL -> 0xB9A6EC;
            case RUINS -> 0xB8B0C0;
            case STREET, LONDON -> 0xF5B07A;
            case PLAIN -> 0x8FD3D6;
            case FARM -> 0xF0D58A;
            case GARDENS -> 0xF4A6C8;
            case GRAIL -> 0xF7D97A;
            case REVERSE, DREAM -> 0xA895E0;
            default -> 0xE8A0BF;
        };
    }

    static List<Place> discovered(Minecraft mc) {
        List<Place> out = new ArrayList<>();
        Set<StoryLocation> known = new HashSet<>();
        StoryChapter current = ClientQuestState.chapter();
        for (StoryChapter chapter : StoryChapter.values()) {
            if (chapter.ordinal() <= current.ordinal()) {
                StoryLocation l = StoryLocation.forChapter(chapter);
                if (l != null) {
                    known.add(l.primary());
                }
            }
        }
        for (StoryLocation l : StoryLocation.values()) {
            if (l.primary() != l) {
                continue;
            }
            boolean castle = l == StoryLocation.CASTLE_LAB;
            if (!castle && !known.contains(l) && !MapSettings.isSeen(mc, l)) {
                continue;
            }
            Component name = castle ? Component.translatable("gui.fateapocrypha.map.castle")
                    : Component.translatable(l.nameKey());
            out.add(new Place(l, name, color(l), castle));
        }
        return out;
    }

    /** Место текущего задания или null. */
    static StoryLocation quest() {
        StoryChapter chapter = ClientQuestState.chapter();
        if (chapter == StoryChapter.TO_BE_CONTINUED || ClientQuestState.started()) {
            return null;
        }
        StoryLocation l = StoryLocation.forChapter(chapter);
        return l == null ? null : l.primary();
    }
}
