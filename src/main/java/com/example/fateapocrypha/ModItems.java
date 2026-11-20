package com.example.fateapocrypha;

import com.example.fateapocrypha.item.CommandSealItem;
import com.example.fateapocrypha.item.FateTier;
import com.example.fateapocrypha.item.FateWeaponItem;
import com.example.fateapocrypha.item.SummonCatalystItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.SwordItem;
import net.minecraft.world.item.Tiers;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public class ModItems {
    public static final DeferredRegister<Item> ITEMS =
            DeferredRegister.create(ForgeRegistries.ITEMS, FateApocrypha.MODID);

    public static final RegistryObject<Item> COMMAND_SEAL = ITEMS.register("command_seal",
            () -> new CommandSealItem(new Item.Properties().stacksTo(1)));

    public static final RegistryObject<Item> SUMMON_CATALYST = ITEMS.register("summon_catalyst",
            () -> new SummonCatalystItem(new Item.Properties().stacksTo(16)));

    public static final RegistryObject<Item> ASTOLFO_SWORD = ITEMS.register("astolfo_sword",
            () -> new SwordItem(Tiers.IRON, 3, -2.4F, new Item.Properties()));

    // ---------- материалы: награды за сюжет и компоненты крафта ----------
    private static RegistryObject<Item> material(String id) {
        return ITEMS.register(id, () -> new Item(new Item.Properties()));
    }

    public static final RegistryObject<Item> PRANA_CRYSTAL = material("prana_crystal");
    public static final RegistryObject<Item> HOLY_STEEL = material("holy_steel");
    public static final RegistryObject<Item> DRAGON_SCALE = material("dragon_scale");
    public static final RegistryObject<Item> FAFNIR_BLOOD = material("fafnir_blood");
    public static final RegistryObject<Item> CRIMSON_SHARD = material("crimson_shard");
    public static final RegistryObject<Item> LIGHTNING_CORE = material("lightning_core");
    public static final RegistryObject<Item> HIPPOGRIFF_FEATHER = material("hippogriff_feather");
    public static final RegistryObject<Item> HOLY_CLOTH = material("holy_cloth");
    public static final RegistryObject<Item> BLACK_STEEL = material("black_steel");
    public static final RegistryObject<Item> BLOOD_THORN = material("blood_thorn");
    public static final RegistryObject<Item> PELIAN_ASH = material("pelian_ash");
    public static final RegistryObject<Item> SILVER_BOWSTRING = material("silver_bowstring");
    public static final RegistryObject<Item> GARDENS_GEM = material("gardens_gem");
    public static final RegistryObject<Item> FOG_ESSENCE = material("fog_essence");
    public static final RegistryObject<Item> BROKEN_CHAIN = material("broken_chain");

    // ---------- легендарное оружие ----------
    private static RegistryObject<Item> weapon(String id, int damage, float speed, FateWeaponItem.Ability ability) {
        return weapon(id, FateTier.SACRED, damage, speed, ability);
    }

    private static RegistryObject<Item> weapon(String id, FateTier tier, int damage, float speed, FateWeaponItem.Ability ability) {
        return ITEMS.register(id, () -> new FateWeaponItem(tier, damage, speed,
                new Item.Properties().stacksTo(1).rarity(Rarity.EPIC).fireResistant(), ability));
    }

    public static final RegistryObject<Item> BALMUNG = weapon("balmung", 5, -2.8F, FateWeaponItem.Ability.DRAGON_SLASH);
    public static final RegistryObject<Item> CLARENT = weapon("clarent", 4, -2.4F, FateWeaponItem.Ability.BLOOD_ARTHUR);
    public static final RegistryObject<Item> VASAVI_SHAKTI = weapon("vasavi_shakti", 4, -2.6F, FateWeaponItem.Ability.VASAVI);
    public static final RegistryObject<Item> ARGALIA_LANCE = weapon("argalia_lance", 2, -2.0F, FateWeaponItem.Ability.ARGALIA);
    public static final RegistryObject<Item> LA_PUCELLE = weapon("la_pucelle", 0, -2.4F, FateWeaponItem.Ability.BANNER);

    // Оружие Слуг Чёрных и Красных: у лёгких клинков теневой материал (быстрее, чинится чёрной сталью)
    public static final RegistryObject<Item> BLACK_KEYS = weapon("black_keys", FateTier.SHADOW, 2, -1.2F, FateWeaponItem.Ability.BLACK_KEYS);
    public static final RegistryObject<Item> RIPPER_KNIFE = weapon("ripper_knife", FateTier.SHADOW, 1, -1.0F, FateWeaponItem.Ability.FOG);
    public static final RegistryObject<Item> SPARTACUS_BLADE = weapon("spartacus_blade", FateTier.SHADOW, 4, -3.0F, FateWeaponItem.Ability.REBEL);
    public static final RegistryObject<Item> KAZIKLI_BEY = weapon("kazikli_bey", 2, -2.6F, FateWeaponItem.Ability.IMPALE);
    public static final RegistryObject<Item> ACHILLES_SPEAR = weapon("achilles_spear", 2, -2.2F, FateWeaponItem.Ability.HEEL);
    public static final RegistryObject<Item> ATALANTA_BOW = weapon("atalanta_bow", 0, -2.0F, FateWeaponItem.Ability.PHOEBUS);
    public static final RegistryObject<Item> SEMIRAMIS_STAFF = weapon("semiramis_staff", 0, -2.8F, FateWeaponItem.Ability.GARDENS);
}
