package com.example.fateapocrypha.story;

import com.example.fateapocrypha.network.ModNetwork;
import com.example.fateapocrypha.network.OpenBoardPacket;
import com.example.fateapocrypha.network.SideQuestSyncPacket;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.network.PacketDistributor;

/**
 * Прогресс побочных заданий игрока. Хранится вместе с остальным прогрессом (переживает смерть).
 * Состояние: LOCKED (глава сюжета ещё не наступила), AVAILABLE (висит на доске), ACTIVE (взято),
 * READY (цель разведки достигнута, можно сдавать), DONE.
 * В NBT пишутся только ACTIVE, READY и DONE: LOCKED и AVAILABLE считаются по текущей главе.
 */
public final class SideQuestManager {
    public static final int LOCKED = 0;
    public static final int AVAILABLE = 1;
    public static final int ACTIVE = 2;
    public static final int READY = 3;
    public static final int DONE = 4;

    private static final String PERSISTED = "PlayerPersisted";
    private static final String KEY = "fateapocrypha_side";

    private SideQuestManager() {
    }

    private static CompoundTag store(Player player) {
        CompoundTag data = player.getPersistentData();
        if (!data.contains(PERSISTED, Tag.TAG_COMPOUND)) {
            data.put(PERSISTED, new CompoundTag());
        }
        CompoundTag persisted = data.getCompound(PERSISTED);
        if (!persisted.contains(KEY, Tag.TAG_COMPOUND)) {
            persisted.put(KEY, new CompoundTag());
        }
        return persisted.getCompound(KEY);
    }

    public static int state(Player player, SideQuest quest) {
        int stored = store(player).getInt(quest.id());
        if (stored >= ACTIVE) {
            return stored;
        }
        return StoryManager.getChapter(player).ordinal() >= quest.unlock().ordinal() ? AVAILABLE : LOCKED;
    }

    private static void setState(Player player, SideQuest quest, int state) {
        store(player).putInt(quest.id(), state);
        sync(player);
    }

    /** Состояния всех заданий по порядку SideQuest.values() для клиента. */
    public static byte[] states(Player player) {
        SideQuest[] all = SideQuest.values();
        byte[] out = new byte[all.length];
        for (int i = 0; i < all.length; i++) {
            out[i] = (byte) state(player, all[i]);
        }
        return out;
    }

    public static void sync(Player player) {
        if (player instanceof ServerPlayer sp) {
            ModNetwork.CHANNEL.send(PacketDistributor.PLAYER.with(() -> sp), new SideQuestSyncPacket(states(player)));
        }
    }

    // ---------- предметы ----------
    public static int countItem(Player player, Item item) {
        int total = 0;
        for (ItemStack stack : player.getInventory().items) {
            if (stack.is(item)) {
                total += stack.getCount();
            }
        }
        return total;
    }

    private static void consume(Player player, Item item, int amount) {
        int left = amount;
        for (ItemStack stack : player.getInventory().items) {
            if (left <= 0) {
                break;
            }
            if (stack.is(item)) {
                int take = Math.min(left, stack.getCount());
                stack.shrink(take);
                left -= take;
            }
        }
    }

    // ---------- работа с доской ----------
    /** Игрок нажал на доску: отправляем актуальные состояния и открываем окно доски. */
    public static void open(ServerPlayer player, SideBoard board) {
        sync(player);
        ModNetwork.CHANNEL.send(PacketDistributor.PLAYER.with(() -> player), new OpenBoardPacket(board.ordinal()));
    }

    /** Игрок рядом с доской (проверка на сервере для пакетов из окна). */
    public static boolean near(ServerPlayer player, SideBoard board) {
        return Math.hypot(player.getX() - board.x(), player.getZ() - board.z()) <= 12.0D
                && Math.abs(player.getY() - board.y()) <= 12.0D;
    }

    /** Взять новое задание с этой доски. */
    public static void accept(ServerPlayer player, SideBoard board, SideQuest q) {
        if (q.giver() != board || state(player, q) != AVAILABLE) {
            return;
        }
        setState(player, q, ACTIVE);
        player.sendSystemMessage(Component.translatable("side.fateapocrypha.accepted",
                Component.translatable(q.titleKey())).withStyle(ChatFormatting.GOLD));
        player.sendSystemMessage(Component.translatable(q.startKey()).withStyle(ChatFormatting.GRAY, ChatFormatting.ITALIC));
        player.sendSystemMessage(objectiveText(player, q).copy().withStyle(ChatFormatting.YELLOW));
        player.level().playSound(null, player.blockPosition(), SoundEvents.BOOK_PAGE_TURN,
                SoundSource.PLAYERS, 1.0F, 1.0F);
    }

    /** Сдать задание на этой доске: посылку, найденное при разведке или принесённые предметы. */
    public static void turnIn(ServerPlayer player, SideBoard board, SideQuest q) {
        int st = state(player, q);
        if (q.type() == SideQuest.Type.DELIVER && q.target() == board && st == ACTIVE) {
            complete(player, q);
            return;
        }
        if (q.giver() != board) {
            return;
        }
        if (st == READY) {
            complete(player, q);
        } else if (st == ACTIVE && q.type() == SideQuest.Type.FETCH) {
            int have = countItem(player, q.item());
            if (have >= q.count()) {
                consume(player, q.item(), q.count());
                complete(player, q);
            } else {
                player.sendSystemMessage(Component.translatable("side.fateapocrypha.not_enough",
                        Component.translatable(q.titleKey()), new ItemStack(q.item()).getHoverName(),
                        have, q.count()).withStyle(ChatFormatting.YELLOW));
            }
        }
    }

    /** Короткая цель задания для чата. */
    public static Component objectiveText(Player player, SideQuest q) {
        int st = state(player, q);
        switch (q.type()) {
            case FETCH:
                return Component.translatable("side.fateapocrypha.obj.fetch", new ItemStack(q.item()).getHoverName(),
                        Math.min(countItem(player, q.item()), q.count()), q.count());
            case EXPLORE:
                if (st == READY) {
                    return Component.translatable("side.fateapocrypha.obj.return", Component.translatable(q.giver().nameKey()));
                }
                int[] p = q.explorePoint();
                return Component.translatable("side.fateapocrypha.obj.explore", p[0], p[1]);
            default:
                return Component.translatable("side.fateapocrypha.obj.deliver", Component.translatable(q.target().nameKey()));
        }
    }

    private static void complete(ServerPlayer player, SideQuest q) {
        setState(player, q, DONE);
        player.sendSystemMessage(Component.translatable("side.fateapocrypha.completed",
                Component.translatable(q.titleKey())).withStyle(ChatFormatting.GREEN));
        player.sendSystemMessage(Component.translatable(q.doneKey()).withStyle(ChatFormatting.GRAY, ChatFormatting.ITALIC));
        StoryRewards.Reward reward = q.reward();
        List<String> parts = new ArrayList<>();
        for (ItemStack stack : reward.items()) {
            ItemStack give = stack.copy();
            parts.add(give.getCount() + "x " + give.getHoverName().getString());
            if (!player.getInventory().add(give)) {
                player.drop(give, false);
            }
        }
        if (reward.levels() > 0) {
            player.giveExperienceLevels(reward.levels());
            parts.add(Component.translatable("story.fateapocrypha.reward_levels", reward.levels()).getString());
        }
        player.sendSystemMessage(Component.translatable("side.fateapocrypha.reward", String.join(", ", parts))
                .withStyle(ChatFormatting.GREEN));
        player.level().playSound(null, player.blockPosition(), SoundEvents.PLAYER_LEVELUP, SoundSource.PLAYERS, 1.0F, 1.2F);
    }

    /** Раз в секунду: разведка засчитывается, когда игрок дошёл до точки. */
    public static void tick(ServerPlayer player) {
        for (SideQuest q : SideQuest.values()) {
            if (q.type() != SideQuest.Type.EXPLORE || state(player, q) != ACTIVE) {
                continue;
            }
            int[] p = q.explorePoint();
            if (Math.hypot(player.getX() - p[0], player.getZ() - p[1]) <= q.radius()) {
                setState(player, q, READY);
                player.sendSystemMessage(Component.translatable("side.fateapocrypha.reached",
                        Component.translatable(q.titleKey()), Component.translatable(q.giver().nameKey()))
                        .withStyle(ChatFormatting.AQUA));
                player.level().playSound(null, player.blockPosition(), SoundEvents.EXPERIENCE_ORB_PICKUP,
                        SoundSource.PLAYERS, 1.0F, 1.4F);
            }
        }
    }
}
