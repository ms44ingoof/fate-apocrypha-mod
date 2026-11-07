package com.example.fateapocrypha.story;

import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;

/** Сюжетные NPC. slim = тонкие руки (Alex) в скине. */
public enum NpcType {
    ASTOLFO("astolfo", true, Items.GOLDEN_SWORD),
    CHIRON("chiron", false, Items.BOW),
    JEANNE("jeanne", true, Items.WHITE_BANNER),
    MORDRED("mordred", false, Items.IRON_SWORD),
    SEMIRAMIS("semiramis", false, Items.AIR),
    KAIRI("kairi", false, Items.AIR),
    SHIRO("shiro", false, Items.AIR),
    FRANKENSTEIN("frankenstein", false, Items.AIR),
    CAULES("caules", false, Items.AIR),
    SIEGFRIED("siegfried", false, Items.DIAMOND_SWORD),
    GORDES("gordes", false, Items.AIR),
    CELENIKE("celenike", false, Items.AIR),
    SPARTACUS("spartacus", false, Items.IRON_AXE),
    JACK("jack", false, Items.IRON_SWORD),
    ATALANTA("atalanta", false, Items.BOW),
    AVICEBRON("avicebron", false, Items.AIR),
    ROCHE("roche", false, Items.AIR),
    FARMER("farmer", false, Items.IRON_HOE),
    DARNIC("darnic", false, Items.AIR),
    VLAD("vlad", false, Items.IRON_SWORD),
    FIORE("fiore", false, Items.AIR),
    REIKA("reika", false, Items.AIR),
    ACHILLES("achilles", false, Items.IRON_SWORD),
    KARNA("karna", false, Items.TRIDENT),
    JUSTICE("justice", true, Items.AIR),
    ZIG("zig", false, Items.AIR);

    private final String id;
    private final boolean slim;
    private final Item heldItem;

    NpcType(String id, boolean slim, Item heldItem) {
        this.id = id;
        this.slim = slim;
        this.heldItem = heldItem;
    }

    public String id() {
        return id;
    }

    public boolean slim() {
        return slim;
    }

    public Item heldItem() {
        return heldItem;
    }

    public static NpcType byId(String id) {
        for (NpcType type : values()) {
            if (type.id.equals(id)) {
                return type;
            }
        }
        return ASTOLFO;
    }
}
