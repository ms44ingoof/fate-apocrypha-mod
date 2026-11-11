package com.example.fateapocrypha.story;

import com.example.fateapocrypha.ModItems;
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

    private static ItemStack item(net.minecraft.world.item.Item item, int count) {
        return new ItemStack(item, count);
    }

    /**
     * Награды подобраны под крафт оружия: у каждого легендарного оружия есть уникальный компонент из сюжета
     * (Бальмунг: чешуя и кровь Фафнира из снов; Кларент: осколки Мордред; Васави Шакти: ядра молнии; копьё Астольфо: перья
     * Гиппогрифа; Знамя Жанны: святая ткань). Святая сталь и кристаллы праны общие для всех рецептов.
     */
    public static Reward of(StoryChapter chapter) {
        switch (chapter) {
            case LAB_TANK: return r(1);
            case LAB_WALK: return r(1, item(Items.BREAD, 4));
            case CORRIDOR: return r(1);
            case CHIRON: return r(2, item(Items.COOKED_BEEF, 6), item(ModItems.PRANA_CRYSTAL.get(), 1));
            case ESCAPE: return r(3, item(Items.COOKED_BEEF, 8), item(Items.IRON_INGOT, 4), item(ModItems.PRANA_CRYSTAL.get(), 2));
            case FOREST: return r(4, item(Items.GOLDEN_APPLE, 1), item(ModItems.HOLY_STEEL.get(), 2));
            case FARM: return r(4, item(Items.BREAD, 8), item(Items.EMERALD, 3), item(ModItems.HOLY_CLOTH.get(), 2));
            case DREAM_FAFNIR: return r(5, item(ModItems.DRAGON_SCALE.get(), 3), item(Items.BLAZE_POWDER, 2));
            case INTERLUDE_RED: return r(2, item(ModItems.CRIMSON_SHARD.get(), 1), item(ModItems.PRANA_CRYSTAL.get(), 1));
            case INTERLUDE_SHIRO:
            case INTERLUDE_FRANKENSTEIN:
            case INTERLUDE_SPARTACUS:
            case INTERLUDE_AVICEBRON:
            case INTERLUDE_DARNIC:
            case INTERLUDE_GRAIL:
            case INTERLUDE_ACHILLES:
                return r(2, item(Items.EMERALD, 2), item(ModItems.PRANA_CRYSTAL.get(), 1));
            case INTERLUDE_SEMIRAMIS: return r(2, item(ModItems.CRIMSON_SHARD.get(), 1), item(ModItems.PRANA_CRYSTAL.get(), 1));
            case INTERLUDE_PUCELLE: return r(2, item(ModItems.HOLY_CLOTH.get(), 1), item(ModItems.PRANA_CRYSTAL.get(), 1));
            case FRANKEN_LEGACY: return r(5, item(ModItems.LIGHTNING_CORE.get(), 2), item(Items.LIGHTNING_ROD, 1));
            case CELENIKE: return r(5, item(ModItems.HIPPOGRIFF_FEATHER.get(), 2), item(Items.IRON_HELMET, 1), item(Items.IRON_BOOTS, 1));
            case JACK: return r(6, item(ModItems.HOLY_STEEL.get(), 2), item(Items.IRON_CHESTPLATE, 1), item(Items.IRON_LEGGINGS, 1));
            case GOLEM_ADAM: return r(8, item(Items.DIAMOND, 3), item(Items.GOLDEN_CARROT, 8), item(ModItems.PRANA_CRYSTAL.get(), 3));
            case RESISTANCE: return r(5, item(Items.GOLDEN_CARROT, 8), item(Items.ARROW, 32), item(ModItems.PRANA_CRYSTAL.get(), 2));
            case NIGHT_TALK: return r(3, item(Items.CAKE, 1), item(ModItems.HOLY_CLOTH.get(), 1));
            case DREAM_FAFNIR_2: return r(5, item(ModItems.FAFNIR_BLOOD.get(), 2), item(Items.FIRE_CHARGE, 6));
            case GARDENS_ASSAULT: return r(6, item(Items.GOLDEN_APPLE, 2), item(Items.DIAMOND, 2), item(ModItems.HOLY_STEEL.get(), 2));
            case KARNA: return r(10, item(Items.DIAMOND, 4), item(Items.GOLDEN_APPLE, 1), item(ModItems.LIGHTNING_CORE.get(), 1));
            case SHIRO_FINAL: return r(15, item(Items.TOTEM_OF_UNDYING, 1), item(Items.DIAMOND, 6), item(ModItems.PRANA_CRYSTAL.get(), 4));
            case GRAIL_INSIDE: return r(5, item(Items.NETHER_STAR, 1));
            case EPILOGUE: return r(20, item(Items.EMERALD, 16), item(Items.ENCHANTED_GOLDEN_APPLE, 1));
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
