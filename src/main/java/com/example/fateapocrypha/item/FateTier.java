package com.example.fateapocrypha.item;

import com.example.fateapocrypha.ModItems;
import java.util.function.Supplier;
import net.minecraft.world.item.Tier;
import net.minecraft.world.item.crafting.Ingredient;

/** Материал легендарного оружия: прочнее незерита, чинится святой сталью. */
public enum FateTier implements Tier {
    SACRED(2800, 9.0F, 4.0F, 4, 25, () -> Ingredient.of(ModItems.HOLY_STEEL.get()));

    private final int uses;
    private final float speed;
    private final float damage;
    private final int level;
    private final int enchantment;
    private final Supplier<Ingredient> repair;

    FateTier(int uses, float speed, float damage, int level, int enchantment, Supplier<Ingredient> repair) {
        this.uses = uses;
        this.speed = speed;
        this.damage = damage;
        this.level = level;
        this.enchantment = enchantment;
        this.repair = repair;
    }

    @Override
    public int getUses() {
        return uses;
    }

    @Override
    public float getSpeed() {
        return speed;
    }

    @Override
    public float getAttackDamageBonus() {
        return damage;
    }

    @Override
    public int getLevel() {
        return level;
    }

    @Override
    public int getEnchantmentValue() {
        return enchantment;
    }

    @Override
    public Ingredient getRepairIngredient() {
        return repair.get();
    }
}
