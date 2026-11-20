package com.example.fateapocrypha.story;

import com.example.fateapocrypha.ModItems;
import com.example.fateapocrypha.world.TrifasRoads;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

/**
 * Побочные задания. Берутся и сдаются у досок объявлений (SideBoard) у ворот локаций.
 * Три вида: FETCH (принести предметы к своей доске), EXPLORE (дойти до точки на дороге и вернуться к доске),
 * DELIVER (взять посылку у одной доски и сдать у другой). Задание открывается с нужной главы сюжета.
 */
public enum SideQuest {
    CASTLE_RATIONS("castle_rations", Type.FETCH, StoryChapter.FOREST, SideBoard.CASTLE, SideBoard.CASTLE,
            () -> Items.BREAD, 12, null),
    FOREST_BERRIES("forest_berries", Type.FETCH, StoryChapter.FOREST, SideBoard.FOREST, SideBoard.FOREST,
            () -> Items.SWEET_BERRIES, 10, null),
    FARM_HARVEST("farm_harvest", Type.FETCH, StoryChapter.DREAM_FAFNIR, SideBoard.FARM, SideBoard.FARM,
            () -> Items.WHEAT, 16, null),
    CAMP_SCOUT("camp_scout", Type.EXPLORE, StoryChapter.FRANKEN_LEGACY, SideBoard.CAMP, SideBoard.CAMP,
            null, 0, StoryLocation.STREET),
    CHAPEL_CANDLES("chapel_candles", Type.FETCH, StoryChapter.FRANKEN_LEGACY, SideBoard.CHAPEL, SideBoard.CHAPEL,
            () -> Items.CANDLE, 4, null),
    STREET_LETTER("street_letter", Type.DELIVER, StoryChapter.JACK, SideBoard.STREET, SideBoard.LONDON,
            null, 0, null),
    LONDON_PARCEL("london_parcel", Type.DELIVER, StoryChapter.GOLEM_ADAM, SideBoard.LONDON, SideBoard.RUINS,
            null, 0, null),
    RUINS_SURVEY("ruins_survey", Type.EXPLORE, StoryChapter.GOLEM_ADAM, SideBoard.RUINS, SideBoard.RUINS,
            null, 0, StoryLocation.PLAIN);

    public enum Type { FETCH, EXPLORE, DELIVER }

    private final String id;
    private final Type type;
    private final StoryChapter unlock;
    private final SideBoard giver;
    private final SideBoard target;
    private final Supplier<Item> item;
    private final int count;
    private final StoryLocation toward;
    private int[] point;

    SideQuest(String id, Type type, StoryChapter unlock, SideBoard giver, SideBoard target,
              Supplier<Item> item, int count, StoryLocation toward) {
        this.id = id;
        this.type = type;
        this.unlock = unlock;
        this.giver = giver;
        this.target = target;
        this.item = item;
        this.count = count;
        this.toward = toward;
    }

    public String id() {
        return id;
    }

    public Type type() {
        return type;
    }

    /** Глава, с которой задание появляется на доске. */
    public StoryChapter unlock() {
        return unlock;
    }

    /** Доска, где задание берут (и сдают, если это не доставка). */
    public SideBoard giver() {
        return giver;
    }

    /** Доска, где задание сдают (у FETCH и EXPLORE совпадает с giver). */
    public SideBoard target() {
        return target;
    }

    /** Предмет для FETCH (иначе null). */
    public Item item() {
        return item == null ? null : item.get();
    }

    public int count() {
        return count;
    }

    public String titleKey() {
        return "side.fateapocrypha." + id + ".title";
    }

    public String descKey() {
        return "side.fateapocrypha." + id + ".desc";
    }

    public String startKey() {
        return "side.fateapocrypha." + id + ".start";
    }

    public String doneKey() {
        return "side.fateapocrypha." + id + ".done";
    }

    /** Для EXPLORE: радиус цели в блоках. */
    public int radius() {
        return 30;
    }

    /** Для EXPLORE: точка на дороге в 150 блоках от ворот локации доски (x, z). */
    public int[] explorePoint() {
        if (point == null) {
            int[] p = null;
            for (TrifasRoads.Opening o : TrifasRoads.openings(giver.location())) {
                if (o.to() == toward) {
                    p = new int[] {(int) Math.round(o.gx() + o.ux() * 150.0D), (int) Math.round(o.gz() + o.uz() * 150.0D)};
                    break;
                }
            }
            point = p != null ? p : new int[] {giver.x(), giver.z()};
        }
        return point;
    }

    /** Награда: те же предметы и уровни показывает журнал и выдаёт сервер. */
    public StoryRewards.Reward reward() {
        List<ItemStack> list = new ArrayList<>();
        int levels;
        switch (this) {
            case CASTLE_RATIONS -> {
                levels = 3;
                list.add(new ItemStack(Items.COOKED_BEEF, 6));
                list.add(new ItemStack(ModItems.PRANA_CRYSTAL.get(), 1));
            }
            case FOREST_BERRIES -> {
                levels = 3;
                list.add(new ItemStack(Items.GOLDEN_APPLE, 1));
                list.add(new ItemStack(ModItems.PRANA_CRYSTAL.get(), 1));
            }
            case FARM_HARVEST -> {
                levels = 4;
                list.add(new ItemStack(Items.GOLDEN_CARROT, 4));
                list.add(new ItemStack(ModItems.HOLY_CLOTH.get(), 1));
            }
            case CAMP_SCOUT -> {
                levels = 4;
                list.add(new ItemStack(Items.ARROW, 16));
                list.add(new ItemStack(ModItems.PRANA_CRYSTAL.get(), 1));
            }
            case CHAPEL_CANDLES -> {
                levels = 4;
                list.add(new ItemStack(ModItems.HOLY_STEEL.get(), 1));
                list.add(new ItemStack(Items.EMERALD, 2));
            }
            case STREET_LETTER -> {
                levels = 5;
                list.add(new ItemStack(Items.EMERALD, 3));
                list.add(new ItemStack(ModItems.CRIMSON_SHARD.get(), 1));
            }
            case LONDON_PARCEL -> {
                levels = 5;
                list.add(new ItemStack(Items.DIAMOND, 1));
                list.add(new ItemStack(ModItems.LIGHTNING_CORE.get(), 1));
            }
            default -> {
                levels = 6;
                list.add(new ItemStack(Items.DIAMOND, 1));
                list.add(new ItemStack(ModItems.DRAGON_SCALE.get(), 1));
            }
        }
        return new StoryRewards.Reward(list, levels);
    }
}
