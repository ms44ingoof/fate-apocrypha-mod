package com.example.fateapocrypha.story;

import net.minecraft.ChatFormatting;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.player.Player;
import java.util.UUID;

/** Хранит и меняет прогресс игрока. Данные переживают смерть игрока. */
public class StoryManager {
    private static final String PERSISTED = "PlayerPersisted";
    private static final String KEY = "fateapocrypha_chapter";
    private static final String SERVANT = "fateapocrypha_servant";
    private static final String KIT = "fateapocrypha_kit";

    private static CompoundTag persisted(Player player) {
        CompoundTag data = player.getPersistentData();
        if (!data.contains(PERSISTED, Tag.TAG_COMPOUND)) {
            data.put(PERSISTED, new CompoundTag());
        }
        return data.getCompound(PERSISTED);
    }

    public static StoryChapter getChapter(Player player) {
        return StoryChapter.byId(persisted(player).getInt(KEY));
    }

    public static void setChapter(Player player, StoryChapter chapter) {
        persisted(player).putInt(KEY, chapter.ordinal());
    }

    /** Переводит игрока на главу, только если она дальше текущей. */
    public static void advanceTo(Player player, StoryChapter chapter) {
        if (player.level().isClientSide) {
            return;
        }
        if (getChapter(player).ordinal() < chapter.ordinal()) {
            setChapter(player, chapter);
            player.sendSystemMessage(Component.translatable("story.fateapocrypha.new_chapter",
                    Component.translatable(chapter.titleKey())).withStyle(ChatFormatting.GOLD));
            player.sendSystemMessage(Component.translatable(chapter.objectiveKey()).withStyle(ChatFormatting.YELLOW));
            player.level().playSound(null, player.blockPosition(), SoundEvents.PLAYER_LEVELUP,
                    SoundSource.PLAYERS, 1.0F, 1.0F);
        }
    }

    public static UUID getServantId(Player player) {
        CompoundTag tag = persisted(player);
        return tag.hasUUID(SERVANT) ? tag.getUUID(SERVANT) : null;
    }

    public static void setServantId(Player player, UUID id) {
        persisted(player).putUUID(SERVANT, id);
    }

    public static boolean hasReceivedKit(Player player) {
        return persisted(player).getBoolean(KIT);
    }

    public static void setReceivedKit(Player player) {
        persisted(player).putBoolean(KIT, true);
    }

    public static void showObjective(Player player) {
        StoryChapter chapter = getChapter(player);
        player.sendSystemMessage(Component.translatable("story.fateapocrypha.chapter_header",
                Component.translatable(chapter.titleKey())).withStyle(ChatFormatting.GOLD));
        player.sendSystemMessage(Component.translatable(chapter.objectiveKey()).withStyle(ChatFormatting.YELLOW));
    }
}
