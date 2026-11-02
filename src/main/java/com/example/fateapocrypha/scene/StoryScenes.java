package com.example.fateapocrypha.scene;

import com.example.fateapocrypha.ModItems;
import com.example.fateapocrypha.story.NpcType;
import com.example.fateapocrypha.story.StoryChapter;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.AABB;

/**
 * Сценарии глав. Диалоги лежат в lang-файлах: dialogue.fateapocrypha.<сцена>.<номер>.
 * Координаты берутся из StoryWorld (замени их, если построишь свои локации).
 */
public class StoryScenes {
    private static final double Y = StoryWorld.Y0 + 1;

    private static final String SIEG = "npc.fateapocrypha.sieg";
    private static final String ASTOLFO = "npc.fateapocrypha.astolfo";
    private static final String CHIRON = "npc.fateapocrypha.chiron";
    private static final String UNKNOWN = "npc.fateapocrypha.unknown";

    private static Step say(String speaker, String line) {
        return Steps.say(speaker, "dialogue.fateapocrypha." + line, 120);
    }

    private static AABB zone(double x1, double z1, double x2, double z2) {
        return new AABB(x1, StoryWorld.Y0 - 1, z1, x2, StoryWorld.Y0 + 10, z2);
    }

    public static List<Step> build(StoryChapter chapter) {
        return switch (chapter) {
            case LAB_TANK -> labTank();
            case LAB_WALK -> labWalk();
            case CORRIDOR -> corridor();
            case CHIRON -> chiron();
            case ESCAPE -> escape();
            case FOREST -> forest();
            case TO_BE_CONTINUED -> List.of();
        };
    }

    // 1. Пробуждение в резервуаре
    private static List<Step> labTank() {
        List<Step> s = new ArrayList<>();
        s.add(Steps.fade(0, 40, 60, false));
        s.add(Steps.run(c -> StoryWorld.repairTank(c.trifas())));
        s.add(Steps.teleport(107.5, Y, 107.5, 0.0F));
        s.add(Steps.wait(50));
        s.add(Steps.title("story.fateapocrypha.lab_tank.title", "story.fateapocrypha.lab_tank.subtitle"));
        s.add(Steps.wait(60));
        s.add(say(SIEG, "lab_tank.1"));
        s.add(say(SIEG, "lab_tank.2"));
        s.add(say(SIEG, "lab_tank.3"));
        s.add(Steps.waitUntil(c -> StoryWorld.isTankBroken(c.trifas())));
        s.add(Steps.wait(10));
        s.add(say(SIEG, "lab_tank.4"));
        s.add(Steps.advance(StoryChapter.LAB_WALK));
        return s;
    }

    // 2. Медленно идём по лаборатории
    private static List<Step> labWalk() {
        List<Step> s = new ArrayList<>();
        s.add(Steps.teleport(112.5, Y, 107.5, -90.0F));
        s.add(Steps.wait(20));
        s.add(say(SIEG, "lab_walk.1"));
        s.add(say(SIEG, "lab_walk.2"));
        s.add(Steps.waitInZone(zone(122, 105, 128, 110)));
        s.add(say(SIEG, "lab_walk.3"));
        s.add(Steps.waitInZone(zone(136, 100, 142, 115)));
        s.add(say(SIEG, "lab_walk.4"));
        s.add(Steps.waitInZone(zone(145, 105, 149, 110)));
        s.add(say(SIEG, "lab_walk.5"));
        s.add(Steps.fade(40, 20, 40, true));
        s.add(Steps.advance(StoryChapter.CORRIDOR));
        return s;
    }

    // 3. Коридор: падаем, нас находит Астольфо
    private static List<Step> corridor() {
        List<Step> s = new ArrayList<>();
        s.add(Steps.fade(0, 20, 40, false));
        s.add(Steps.teleport(102.5, Y, 302.5, -90.0F));
        s.add(Steps.effect(MobEffects.MOVEMENT_SLOWDOWN, 600, 10));
        s.add(Steps.effect(MobEffects.JUMP, 600, 200));
        s.add(Steps.effect(MobEffects.BLINDNESS, 80, 0));
        s.add(Steps.effect(MobEffects.CONFUSION, 160, 0));
        s.add(Steps.wait(50));
        s.add(say(SIEG, "corridor.1"));
        s.add(Steps.wait(20));
        s.add(say(UNKNOWN, "corridor.2"));
        s.add(Steps.spawnNpc("astolfo", NpcType.ASTOLFO, 126.5, Y, 302.5, 90.0F));
        s.add(Steps.npcWalk("astolfo", 106.5, Y, 302.5, 1.4D, 400, false));
        s.add(say(ASTOLFO, "corridor.3"));
        s.add(say(SIEG, "corridor.4"));
        s.add(say(ASTOLFO, "corridor.5"));
        s.add(Steps.fade(30, 20, 40, true));
        s.add(Steps.despawnAllNpcs());
        s.add(Steps.advance(StoryChapter.CHIRON));
        return s;
    }

    // 4. Астольфо относит нас к Хирону
    private static List<Step> chiron() {
        List<Step> s = new ArrayList<>();
        s.add(Steps.fade(0, 20, 40, false));
        s.add(Steps.clearEffects());
        s.add(Steps.teleport(109.5, Y, 409.5, 90.0F));
        s.add(Steps.spawnNpc("chiron", NpcType.CHIRON, 105.5, Y, 407.5, -90.0F));
        s.add(Steps.spawnNpc("astolfo", NpcType.ASTOLFO, 108.5, Y, 411.5, 180.0F));
        s.add(Steps.wait(50));
        s.add(say(CHIRON, "chiron.1"));
        s.add(say(SIEG, "chiron.2"));
        s.add(say(CHIRON, "chiron.3"));
        s.add(say(ASTOLFO, "chiron.4"));
        s.add(say(CHIRON, "chiron.5"));
        s.add(say(CHIRON, "chiron.6"));
        s.add(Steps.effect(MobEffects.REGENERATION, 400, 1));
        s.add(Steps.run(c -> c.player.setHealth(c.player.getMaxHealth())));
        s.add(say(SIEG, "chiron.7"));
        s.add(Steps.title("story.fateapocrypha.time_skip.title", "story.fateapocrypha.time_skip.subtitle"));
        s.add(Steps.fade(30, 60, 40, true));
        s.add(Steps.despawnAllNpcs());
        s.add(Steps.advance(StoryChapter.ESCAPE));
        return s;
    }

    // 5. Побег из замка
    private static List<Step> escape() {
        List<Step> s = new ArrayList<>();
        s.add(Steps.fade(0, 30, 40, false));
        s.add(Steps.run(c -> StoryWorld.closeGate(c.trifas())));
        s.add(Steps.teleport(110.5, Y, 409.5, -90.0F));
        s.add(Steps.spawnNpc("chiron", NpcType.CHIRON, 106.5, Y, 405.5, -90.0F));
        s.add(Steps.wait(60));
        s.add(Steps.spawnNpc("astolfo", NpcType.ASTOLFO, 118.5, Y, 407.5, 90.0F));
        s.add(Steps.npcWalk("astolfo", 111.5, Y, 407.5, 1.5D, 200, false));
        s.add(say(ASTOLFO, "escape.1"));
        s.add(say(CHIRON, "escape.2"));
        s.add(say(ASTOLFO, "escape.3"));
        s.add(say(ASTOLFO, "escape.4"));
        s.add(say(SIEG, "escape.5"));
        s.add(say(ASTOLFO, "escape.6"));
        s.add(say(ASTOLFO, "escape.7"));
        s.add(Steps.give(() -> new ItemStack(ModItems.ASTOLFO_SWORD.get())));
        s.add(Steps.sound(SoundEvents.ITEM_PICKUP));
        s.add(say(SIEG, "escape.8"));
        s.add(Steps.npcWalk("astolfo", 150.5, Y, 407.5, 1.2D, 600, true));
        s.add(say(ASTOLFO, "escape.9"));
        s.add(Steps.npcWalk("astolfo", 164.5, Y, 407.5, 1.2D, 400, true));
        s.add(say(ASTOLFO, "escape.10"));
        s.add(Steps.spawnGuards(new double[][] {
                {172.5, Y, 403.5}, {173.5, Y, 407.5}, {172.5, Y, 411.5}}));
        s.add(Steps.waitGuardsDead());
        s.add(say(ASTOLFO, "escape.11"));
        s.add(Steps.npcWalk("astolfo", 188.5, Y, 407.5, 1.2D, 600, true));
        s.add(say(ASTOLFO, "escape.12"));
        s.add(Steps.run(c -> StoryWorld.openGate(c.trifas())));
        s.add(Steps.sound(SoundEvents.ANVIL_BREAK));
        s.add(say(ASTOLFO, "escape.13"));
        s.add(Steps.npcWalk("astolfo", 198.5, Y, 407.5, 1.5D, 400, true));
        s.add(Steps.waitInZone(zone(192, 403, 206, 412)));
        s.add(Steps.fade(30, 20, 40, true));
        s.add(Steps.despawnAllNpcs());
        s.add(Steps.advance(StoryChapter.FOREST));
        return s;
    }

    // 6. В лесу: Командные заклинания
    private static List<Step> forest() {
        List<Step> s = new ArrayList<>();
        s.add(Steps.fade(0, 30, 40, false));
        s.add(Steps.teleport(505.5, Y, 430.5, -90.0F));
        s.add(Steps.spawnNpc("astolfo", NpcType.ASTOLFO, 507.5, Y, 430.5, 90.0F));
        s.add(Steps.wait(60));
        s.add(say(ASTOLFO, "forest.1"));
        s.add(say(SIEG, "forest.2"));
        s.add(say(ASTOLFO, "forest.3"));
        s.add(say(ASTOLFO, "forest.4"));
        s.add(Steps.give(() -> new ItemStack(ModItems.COMMAND_SEAL.get())));
        s.add(Steps.give(() -> new ItemStack(ModItems.SUMMON_CATALYST.get())));
        s.add(Steps.sound(SoundEvents.BEACON_ACTIVATE));
        s.add(say(SIEG, "forest.5"));
        s.add(say(ASTOLFO, "forest.6"));
        s.add(say(ASTOLFO, "forest.7"));
        s.add(Steps.title("story.fateapocrypha.to_be_continued.title", "story.fateapocrypha.to_be_continued.subtitle"));
        s.add(Steps.wait(100));
        s.add(Steps.despawnAllNpcs());
        s.add(Steps.advance(StoryChapter.TO_BE_CONTINUED));
        return s;
    }
}
