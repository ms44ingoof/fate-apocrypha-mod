package com.example.fateapocrypha.story;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

/** Награды за завершение глав. Одни и те же данные показывает журнал и выдаёт сервер. */
public final class StoryRewards {
    private StoryRewards() {
    }

    public record Reward(List<ItemStack> items, int levels) {
        public boolean isEmpty() {
            return items.isEmpty() && levels <= 0;
        }
    }

    private static Reward r(int levels, ItemStack... stacks) {
        List<ItemStack> list = new ArrayList<>();
        for (ItemStack s : stacks) {
            list.add(s);
        }
        return new Reward(list, levels);
    }

    public static Reward of(StoryChapter chapter) {
        switch (chapter) {
            case LAB_TANK: return r(1);
            case LAB_WALK: return r(1, new ItemStack(Items.BREAD, 4));
            case CORRIDOR: return r(1);
            case CHIRON: return r(2, new ItemStack(Items.COOKED_BEEF, 6));
            case ESCAPE: return r(3, new ItemStack(Items.COOKED_BEEF, 8), new ItemStack(Items.IRON_INGOT, 4));
            case FOREST: return r(4, new ItemStack(Items.GOLDEN_APPLE, 1), new ItemStack(Items.IRON_INGOT, 6));
            case FARM: return r(4, new ItemStack(Items.BREAD, 8), new ItemStack(Items.EMERALD, 3));
            case DREAM_FAFNIR: return r(5, new ItemStack(Items.BLAZE_POWDER, 3));
            case INTERLUDE_RED:
            case INTERLUDE_SHIRO:
            case INTERLUDE_FRANKENSTEIN:
            case INTERLUDE_SPARTACUS:
            case INTERLUDE_AVICEBRON:
            case INTERLUDE_DARNIC:
            case INTERLUDE_GRAIL:
            case INTERLUDE_ACHILLES:
            case INTERLUDE_SEMIRAMIS:
            case INTERLUDE_PUCELLE:
                return r(2, new ItemStack(Items.EMERALD, 2));
            case FRANKEN_LEGACY: return r(5, new ItemStack(Items.LIGHTNING_ROD, 1), new ItemStack(Items.COPPER_INGOT, 8));
            case CELENIKE: return r(5, new ItemStack(Items.IRON_HELMET, 1), new ItemStack(Items.IRON_BOOTS, 1));
            case JACK: return r(6, new ItemStack(Items.IRON_CHESTPLATE, 1), new ItemStack(Items.IRON_LEGGINGS, 1));
            case GOLEM_ADAM: return r(8, new ItemStack(Items.DIAMOND, 3), new ItemStack(Items.GOLDEN_CARROT, 8));
            case RESISTANCE: return r(5, new ItemStack(Items.GOLDEN_CARROT, 8), new ItemStack(Items.ARROW, 32));
            case NIGHT_TALK: return r(3, new ItemStack(Items.CAKE, 1));
            case DREAM_FAFNIR_2: return r(5, new ItemStack(Items.FIRE_CHARGE, 6));
            case GARDENS_ASSAULT: return r(6, new ItemStack(Items.GOLDEN_APPLE, 2), new ItemStack(Items.DIAMOND, 2));
            case KARNA: return r(10, new ItemStack(Items.DIAMOND, 4), new ItemStack(Items.GOLDEN_APPLE, 1));
            case SHIRO_FINAL: return r(15, new ItemStack(Items.TOTEM_OF_UNDYING, 1), new ItemStack(Items.DIAMOND, 6));
            case GRAIL_INSIDE: return r(5, new ItemStack(Items.NETHER_STAR, 1));
            case EPILOGUE: return r(20, new ItemStack(Items.EMERALD, 16), new ItemStack(Items.ENCHANTED_GOLDEN_APPLE, 1));
            default: return r(0);
        }
    }

    /** Выдаёт награду за завершённую главу. */
    public static void grant(ServerPlayer player, StoryChapter completed) {
        Reward reward = of(completed);
        if (reward.isEmpty()) {
            return;
        }
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
        player.sendSystemMessage(Component.translatable("story.fateapocrypha.reward",
                Component.translatable(completed.titleKey()), String.join(", ", parts)).withStyle(ChatFormatting.GREEN));
        player.level().playSound(null, player.blockPosition(), SoundEvents.EXPERIENCE_ORB_PICKUP,
                SoundSource.PLAYERS, 1.0F, 1.0F);
    }
}
