package com.example.fateapocrypha.world;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.Container;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.entity.BlockEntity;

/**
 * Наполняет сундуки и бочки построек ресурсами. Таблицы добычи заданы в коде (ванильные предметы,
 * без сюжетных), у каждого типа постройки свой набор: дома, лагеря, руины, башни, святилища, а в замке
 * кузница, оружейная, кухня, мастерская мага, библиотека, спальни и сокровищница.
 */
public final class LootFiller {
    public enum Kind {
        HOUSE, CAMP, RUIN, TOWER, SHRINE, SMITHY, ARMORY, KITCHEN, WORKSHOP, LIBRARY, BEDROOM, TREASURE
    }

    private record Entry(Item item, int min, int max, int weight) {
    }

    private static final Map<Kind, List<Entry>> TABLES = new EnumMap<>(Kind.class);
    private static final Map<Kind, int[]> ROLLS = new EnumMap<>(Kind.class);   // сколько стопок: min, max

    private static Entry e(Item item, int min, int max, int weight) {
        return new Entry(item, min, max, weight);
    }

    private static void table(Kind kind, int minRolls, int maxRolls, Entry... entries) {
        List<Entry> list = new ArrayList<>();
        for (Entry entry : entries) {
            list.add(entry);
        }
        TABLES.put(kind, list);
        ROLLS.put(kind, new int[] {minRolls, maxRolls});
    }

    static {
        table(Kind.HOUSE, 3, 6,
                e(Items.BREAD, 2, 5, 10), e(Items.APPLE, 1, 3, 8), e(Items.CARROT, 2, 5, 7),
                e(Items.POTATO, 2, 5, 7), e(Items.COOKED_BEEF, 1, 3, 5), e(Items.COAL, 2, 6, 8),
                e(Items.TORCH, 4, 12, 8), e(Items.STICK, 3, 8, 6), e(Items.WHEAT, 3, 6, 6),
                e(Items.LEATHER, 1, 3, 4), e(Items.IRON_INGOT, 1, 3, 4), e(Items.GOLD_NUGGET, 2, 6, 3),
                e(Items.PAPER, 1, 3, 3), e(Items.WHITE_WOOL, 1, 3, 3), e(Items.BOOK, 1, 2, 2),
                e(Items.EMERALD, 1, 2, 1));
        table(Kind.CAMP, 3, 6,
                e(Items.ARROW, 4, 12, 10), e(Items.COOKED_PORKCHOP, 1, 3, 8), e(Items.BREAD, 2, 4, 8),
                e(Items.STRING, 2, 5, 7), e(Items.FLINT, 2, 5, 6), e(Items.TORCH, 4, 10, 6),
                e(Items.BONE, 2, 4, 5), e(Items.LEATHER, 1, 3, 5), e(Items.IRON_INGOT, 1, 3, 5),
                e(Items.COOKED_MUTTON, 1, 3, 5), e(Items.IRON_SWORD, 1, 1, 2), e(Items.BOW, 1, 1, 2),
                e(Items.COMPASS, 1, 1, 1), e(Items.GOLD_INGOT, 1, 2, 2));
        table(Kind.RUIN, 3, 6,
                e(Items.GOLD_INGOT, 1, 3, 8), e(Items.IRON_INGOT, 2, 4, 8), e(Items.EMERALD, 1, 2, 5),
                e(Items.DIAMOND, 1, 1, 2), e(Items.REDSTONE, 3, 8, 7), e(Items.LAPIS_LAZULI, 3, 8, 7),
                e(Items.BONE, 2, 5, 7), e(Items.ROTTEN_FLESH, 2, 5, 6), e(Items.ENDER_PEARL, 1, 1, 2),
                e(Items.GOLDEN_APPLE, 1, 1, 1), e(Items.IRON_PICKAXE, 1, 1, 2), e(Items.NAME_TAG, 1, 1, 2),
                e(Items.BOOK, 1, 2, 3));
        table(Kind.TOWER, 3, 6,
                e(Items.ARROW, 8, 16, 10), e(Items.BOW, 1, 1, 3), e(Items.SPYGLASS, 1, 1, 2),
                e(Items.COMPASS, 1, 1, 2), e(Items.TORCH, 4, 10, 6), e(Items.COOKED_BEEF, 2, 4, 6),
                e(Items.BREAD, 2, 4, 6), e(Items.IRON_INGOT, 1, 3, 5), e(Items.IRON_HELMET, 1, 1, 2),
                e(Items.LEATHER_BOOTS, 1, 1, 2), e(Items.MAP, 1, 1, 2), e(Items.GOLD_INGOT, 1, 2, 2));
        table(Kind.SHRINE, 2, 4,
                e(Items.GOLD_INGOT, 1, 3, 8), e(Items.LAPIS_LAZULI, 3, 7, 7), e(Items.EMERALD, 1, 2, 5),
                e(Items.CANDLE, 2, 4, 6), e(Items.AMETHYST_SHARD, 2, 6, 6), e(Items.BOOK, 1, 1, 3),
                e(Items.EXPERIENCE_BOTTLE, 1, 3, 5), e(Items.GOLDEN_APPLE, 1, 1, 1));
        table(Kind.SMITHY, 3, 6,
                e(Items.IRON_INGOT, 3, 8, 10), e(Items.COAL, 4, 10, 9), e(Items.IRON_NUGGET, 4, 12, 6),
                e(Items.COPPER_INGOT, 2, 6, 5), e(Items.FLINT, 2, 5, 4), e(Items.GOLD_INGOT, 1, 3, 3),
                e(Items.IRON_PICKAXE, 1, 1, 3), e(Items.IRON_AXE, 1, 1, 3), e(Items.IRON_SHOVEL, 1, 1, 3),
                e(Items.IRON_SWORD, 1, 1, 3), e(Items.DIAMOND, 1, 1, 1));
        table(Kind.ARMORY, 3, 6,
                e(Items.IRON_SWORD, 1, 1, 6), e(Items.IRON_HELMET, 1, 1, 4), e(Items.IRON_CHESTPLATE, 1, 1, 4),
                e(Items.IRON_LEGGINGS, 1, 1, 4), e(Items.IRON_BOOTS, 1, 1, 4), e(Items.SHIELD, 1, 1, 4),
                e(Items.BOW, 1, 1, 4), e(Items.CROSSBOW, 1, 1, 3), e(Items.ARROW, 12, 24, 10),
                e(Items.CHAINMAIL_HELMET, 1, 1, 2), e(Items.GOLDEN_SWORD, 1, 1, 1), e(Items.DIAMOND_SWORD, 1, 1, 1));
        table(Kind.KITCHEN, 4, 7,
                e(Items.BREAD, 3, 6, 10), e(Items.COOKED_BEEF, 2, 4, 8), e(Items.COOKED_PORKCHOP, 2, 4, 8),
                e(Items.COOKED_CHICKEN, 2, 4, 7), e(Items.COOKED_MUTTON, 2, 4, 6), e(Items.BAKED_POTATO, 3, 6, 7),
                e(Items.CARROT, 3, 6, 6), e(Items.APPLE, 2, 4, 6), e(Items.GOLDEN_CARROT, 1, 2, 3),
                e(Items.PUMPKIN_PIE, 1, 3, 4), e(Items.CAKE, 1, 1, 1), e(Items.SUGAR, 2, 5, 4),
                e(Items.EGG, 2, 6, 4), e(Items.HONEY_BOTTLE, 1, 2, 3), e(Items.COOKIE, 4, 10, 4));
        table(Kind.WORKSHOP, 3, 6,
                e(Items.REDSTONE, 3, 8, 8), e(Items.LAPIS_LAZULI, 3, 8, 8), e(Items.GLASS_BOTTLE, 2, 5, 6),
                e(Items.BLAZE_POWDER, 1, 3, 4), e(Items.ENDER_PEARL, 1, 2, 3), e(Items.AMETHYST_SHARD, 2, 5, 5),
                e(Items.GLOWSTONE_DUST, 2, 6, 5), e(Items.EXPERIENCE_BOTTLE, 1, 3, 4), e(Items.QUARTZ, 2, 6, 5),
                e(Items.BOOK, 1, 3, 5), e(Items.SPIDER_EYE, 1, 3, 4), e(Items.GUNPOWDER, 2, 5, 4),
                e(Items.NETHER_WART, 2, 5, 3), e(Items.EMERALD, 1, 2, 2), e(Items.DIAMOND, 1, 1, 1));
        table(Kind.LIBRARY, 3, 6,
                e(Items.BOOK, 2, 6, 10), e(Items.PAPER, 3, 8, 9), e(Items.INK_SAC, 1, 4, 6),
                e(Items.FEATHER, 2, 5, 6), e(Items.WRITABLE_BOOK, 1, 1, 3), e(Items.MAP, 1, 1, 3),
                e(Items.COMPASS, 1, 1, 2), e(Items.CLOCK, 1, 1, 2), e(Items.LAPIS_LAZULI, 2, 6, 5),
                e(Items.EXPERIENCE_BOTTLE, 1, 2, 3));
        table(Kind.BEDROOM, 2, 5,
                e(Items.WHITE_WOOL, 1, 3, 6), e(Items.BREAD, 1, 3, 7), e(Items.APPLE, 1, 3, 6),
                e(Items.GOLD_NUGGET, 3, 8, 7), e(Items.CLOCK, 1, 1, 1), e(Items.PAPER, 1, 3, 5),
                e(Items.TORCH, 3, 8, 6), e(Items.EMERALD, 1, 1, 2), e(Items.LEATHER, 1, 3, 4),
                e(Items.GOLD_INGOT, 1, 2, 2), e(Items.IRON_INGOT, 1, 3, 4));
        table(Kind.TREASURE, 5, 9,
                e(Items.GOLD_INGOT, 4, 10, 9), e(Items.DIAMOND, 1, 3, 5), e(Items.EMERALD, 2, 5, 7),
                e(Items.GOLD_BLOCK, 1, 2, 3), e(Items.GOLDEN_APPLE, 1, 2, 3), e(Items.ENDER_PEARL, 1, 2, 3),
                e(Items.EXPERIENCE_BOTTLE, 2, 6, 6), e(Items.NETHERITE_SCRAP, 1, 1, 1),
                e(Items.DIAMOND_SWORD, 1, 1, 1), e(Items.DIAMOND_PICKAXE, 1, 1, 1),
                e(Items.GOLDEN_CARROT, 2, 5, 5), e(Items.NAME_TAG, 1, 2, 3), e(Items.LAPIS_LAZULI, 4, 9, 5));
    }

    private LootFiller() {
    }

    /** Кладёт в сундук или бочку по позиции случайный набор из таблицы типа. Если контейнера там нет, ничего не делает. */
    public static void fill(ServerLevel level, BlockPos pos, Random rnd, Kind kind) {
        BlockEntity be = level.getBlockEntity(pos);
        if (!(be instanceof Container container)) {
            return;
        }
        List<Entry> table = TABLES.get(kind);
        int[] rolls = ROLLS.get(kind);
        int total = 0;
        for (Entry entry : table) {
            total += entry.weight();
        }
        int count = rolls[0] + rnd.nextInt(rolls[1] - rolls[0] + 1);
        int size = container.getContainerSize();
        for (int i = 0; i < count; i++) {
            int pick = rnd.nextInt(total);
            Entry chosen = table.get(0);
            for (Entry entry : table) {
                pick -= entry.weight();
                if (pick < 0) {
                    chosen = entry;
                    break;
                }
            }
            int amount = chosen.min() + rnd.nextInt(chosen.max() - chosen.min() + 1);
            ItemStack stack = new ItemStack(chosen.item(), Math.min(amount, chosen.item().getMaxStackSize()));
            int start = rnd.nextInt(size);
            for (int s = 0; s < size; s++) {
                int slot = (start + s) % size;
                if (container.getItem(slot).isEmpty()) {
                    container.setItem(slot, stack);
                    break;
                }
            }
        }
        be.setChanged();
    }

    public static void fill(ServerLevel level, int x, int y, int z, Random rnd, Kind kind) {
        fill(level, new BlockPos(x, y, z), rnd, kind);
    }
}
