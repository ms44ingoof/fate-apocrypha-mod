package com.example.fateapocrypha.story;

import java.util.UUID;
import net.minecraft.ChatFormatting;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.player.Player;

/** Хранит и меняет прогресс игрока. Данные переживают смерть игрока. */
public class StoryManager {
    private static final String PERSISTED = "PlayerPersisted";
    private static final String KEY = "fateapocrypha_chapter";
    private static final String SERVANT = "fateapocrypha_servant";
    private static final String STARTED = "fateapocrypha_started";
    private static final String RETURN = "fateapocrypha_return";
    private static final String SEALS = "fateapocrypha_seals";

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
        persisted(player).putBoolean(STARTED, false);
        sync(player);
    }

    private static void sync(Player player) {
        if (player instanceof ServerPlayer serverPlayer) {
            QuestSync.send(serverPlayer);
        }
    }

    /** Запоминает место, откуда игрок ушёл в интерлюдию или сон (повторный вызов ничего не меняет). */
    public static void rememberPosition(ServerPlayer player) {
        CompoundTag data = persisted(player);
        if (data.contains(RETURN, Tag.TAG_COMPOUND) || !player.level().dimension().equals(com.example.fateapocrypha.ModDimensions.TRIFAS)) {
            return;
        }
        CompoundTag pos = new CompoundTag();
        pos.putDouble("x", player.getX());
        pos.putDouble("y", player.getY());
        pos.putDouble("z", player.getZ());
        pos.putFloat("yaw", player.getYRot());
        pos.putFloat("pitch", player.getXRot());
        data.put(RETURN, pos);
    }

    /** Забирает запомненное место (null, если его нет). */
    public static CompoundTag takeReturn(Player player) {
        CompoundTag data = persisted(player);
        if (!data.contains(RETURN, Tag.TAG_COMPOUND)) {
            return null;
        }
        CompoundTag pos = data.getCompound(RETURN);
        data.remove(RETURN);
        return pos;
    }

    /** Печати Командных заклинаний проявились на руке Мастера (Z, X, C). */
    public static boolean hasSeals(Player player) {
        return persisted(player).getBoolean(SEALS);
    }

    public static void setSeals(Player player, boolean value) {
        persisted(player).putBoolean(SEALS, value);
        sync(player);
    }

    /** Сцена текущей главы уже запускалась (игрок пришёл в нужную локацию). */
    public static boolean isStarted(Player player) {
        return persisted(player).getBoolean(STARTED);
    }

    public static void setStarted(Player player, boolean started) {
        persisted(player).putBoolean(STARTED, started);
        sync(player);
    }

    /** Строка "Место: ... (X, Z)" для глав, которые начинаются при входе в локацию. */
    public static void sendLocationHint(Player player, StoryChapter chapter) {
        StoryLocation location = StoryLocation.forChapter(chapter);
        if (location != null) {
            player.sendSystemMessage(Component.translatable("story.fateapocrypha.location_hint",
                    Component.translatable(location.nameKey()), location.cx(), location.cz())
                    .withStyle(ChatFormatting.AQUA));
        }
    }

    /** Переводит игрока на главу (если она дальше текущей) и запускает её сцену. */
    public static void advanceTo(Player player, StoryChapter chapter) {
        if (player.level().isClientSide) {
            return;
        }
        if (getChapter(player).ordinal() < chapter.ordinal()) {
            StoryChapter completed = getChapter(player);
            setChapter(player, chapter);
            if (player instanceof ServerPlayer rewarded) {
                StoryRewards.grant(rewarded, completed);
            }
            player.sendSystemMessage(Component.translatable("story.fateapocrypha.new_chapter",
                    Component.translatable(chapter.titleKey())).withStyle(ChatFormatting.GOLD));
            player.sendSystemMessage(Component.translatable(chapter.objectiveKey()).withStyle(ChatFormatting.YELLOW));
            player.level().playSound(null, player.blockPosition(), SoundEvents.PLAYER_LEVELUP,
                    SoundSource.PLAYERS, 1.0F, 1.0F);
            if (player instanceof ServerPlayer serverPlayer) {
                StoryDirector.startScene(serverPlayer);
            }
        }
    }

    public static UUID getServantId(Player player) {
        CompoundTag tag = persisted(player);
        return tag.hasUUID(SERVANT) ? tag.getUUID(SERVANT) : null;
    }

    public static void setServantId(Player player, UUID id) {
        persisted(player).putUUID(SERVANT, id);
    }

    public static void showObjective(Player player) {
        StoryChapter chapter = getChapter(player);
        player.sendSystemMessage(Component.translatable("story.fateapocrypha.chapter_header",
                Component.translatable(chapter.titleKey())).withStyle(ChatFormatting.GOLD));
        player.sendSystemMessage(Component.translatable(chapter.objectiveKey()).withStyle(ChatFormatting.YELLOW));
        sync(player);
    }
}
