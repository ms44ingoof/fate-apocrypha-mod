package com.example.fateapocrypha.scene;

import com.example.fateapocrypha.ModItems;
import com.example.fateapocrypha.story.NpcType;
import com.example.fateapocrypha.story.StoryChapter;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.item.ItemStack;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/**
 * Сценарии глав. Диалоги лежат в lang-файлах: dialogue.fateapocrypha.<сцена>.<номер>.
 * Координаты берутся из StoryWorld (замени их, если построишь свои локации).
 */
public class StoryScenes {
    private static final double Y = StoryWorld.Y0 + 1;
    private static final double FX = StoryWorld.FOREST_CX;
    private static final double FZ = StoryWorld.FOREST_CZ;

    private static final String SIEG = "npc.fateapocrypha.sieg";
    private static final String ASTOLFO = "npc.fateapocrypha.astolfo";
    private static final String CHIRON = "npc.fateapocrypha.chiron";
    private static final String JEANNE = "npc.fateapocrypha.jeanne";
    private static final String UNKNOWN = "npc.fateapocrypha.unknown";
    private static final String KAIRI = "npc.fateapocrypha.kairi";
    private static final String MORDRED = "npc.fateapocrypha.mordred";
    private static final String SHIRO = "npc.fateapocrypha.shiro";
    private static final String SEMIRAMIS = "npc.fateapocrypha.semiramis";
    private static final String FRANKENSTEIN = "npc.fateapocrypha.frankenstein";
    private static final String CAULES = "npc.fateapocrypha.caules";

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
            case INTERLUDE_RED -> interludeRed();
            case INTERLUDE_SHIRO -> interludeShiro();
            case INTERLUDE_FRANKENSTEIN -> interludeFrankenstein();
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

    // 5. Побег из замка (гомункулы нас не останавливают)
    private static List<Step> escape() {
        List<Step> s = new ArrayList<>();
        s.add(Steps.fade(0, 30, 40, false));
        s.add(Steps.run(c -> StoryWorld.ensureForest(c.trifas())));   // лес строится, пока экран тёмный
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
        s.add(Steps.give(() -> new ItemStack(ModItems.ASTOLFO_SWORD.get())));
        s.add(Steps.sound(SoundEvents.ITEM_PICKUP));
        s.add(say(SIEG, "escape.5"));
        s.add(Steps.npcWalk("astolfo", 150.5, Y, 407.5, 1.3D, 600, true));
        s.add(say(ASTOLFO, "escape.6"));
        s.add(Steps.npcWalk("astolfo", 169.5, Y, 407.5, 1.3D, 400, true));
        s.add(say(ASTOLFO, "escape.7"));
        s.add(Steps.npcWalk("astolfo", 188.5, Y, 407.5, 1.3D, 400, true));
        s.add(say(ASTOLFO, "escape.8"));
        s.add(Steps.run(c -> StoryWorld.openGate(c.trifas())));
        s.add(Steps.sound(SoundEvents.ANVIL_BREAK));
        s.add(say(ASTOLFO, "escape.9"));
        s.add(Steps.npcWalk("astolfo", 198.5, Y, 407.5, 1.5D, 400, true));
        s.add(Steps.waitInZone(zone(192, 403, 206, 412)));
        s.add(Steps.fade(30, 20, 40, true));
        s.add(Steps.despawnAllNpcs());
        s.add(Steps.advance(StoryChapter.FOREST));
        return s;
    }

    // 6. Лес: бой с големами, Жанна даёт имя
    private static List<Step> forest() {
        List<Step> s = new ArrayList<>();
        s.add(Steps.fade(0, 30, 40, false));
        s.add(Steps.run(c -> StoryWorld.ensureForest(c.trifas())));
        s.add(Steps.teleport(FX + 0.5, Y, FZ + 0.5, -90.0F));
        s.add(Steps.spawnNpc("astolfo", NpcType.ASTOLFO, FX + 2.5, Y, FZ + 0.5, 90.0F));
        s.add(Steps.wait(60));
        s.add(say(ASTOLFO, "forest.1"));
        s.add(say(SIEG, "forest.2"));
        s.add(say(ASTOLFO, "forest.3"));
        s.add(say(ASTOLFO, "forest.4"));
        s.add(Steps.golemFight(new double[][] {
                {FX + 8.5, Y, FZ + 0.5}, {FX - 5.5, Y, FZ - 5.5}, {FX - 5.5, Y, FZ + 6.5}}));
        s.add(say(ASTOLFO, "forest.5"));
        s.add(Steps.spawnNpc("jeanne", NpcType.JEANNE, FX + 0.5, Y, FZ - 8.5, 0.0F));
        s.add(Steps.npcWalk("jeanne", FX - 0.5, Y, FZ - 2.5, 1.3D, 300, false));
        s.add(say(JEANNE, "forest.6"));
        s.add(say(ASTOLFO, "forest.7"));
        s.add(say(JEANNE, "forest.8"));
        s.add(say(SIEG, "forest.9"));
        s.add(say(JEANNE, "forest.10"));
        s.add(say(SIEG, "forest.11"));
        s.add(say(JEANNE, "forest.12"));
        s.add(say(SIEG, "forest.13"));
        s.add(say(ASTOLFO, "forest.14"));
        s.add(Steps.give(() -> new ItemStack(ModItems.COMMAND_SEAL.get())));
        s.add(Steps.give(() -> new ItemStack(ModItems.SUMMON_CATALYST.get())));
        s.add(Steps.sound(SoundEvents.BEACON_ACTIVATE));
        s.add(say(JEANNE, "forest.15"));
        s.add(say(JEANNE, "forest.16"));
        s.add(Steps.wait(40));
        s.add(Steps.fade(30, 10, 5, true));
        s.add(Steps.despawnAllNpcs());
        s.add(Steps.advance(StoryChapter.INTERLUDE_RED));
        return s;
    }

    // ================= Интерлюдии: вид от третьего лица =================
    private static Vec3 v(double x, double y, double z) {
        return new Vec3(x, y, z);
    }

    private static Step cam(Vec3 fromPos, Vec3 fromLook, Vec3 toPos, Vec3 toLook, int ticks) {
        return Steps.cameraMove(fromPos, fromLook, toPos, toLook, ticks);
    }

    private static Step seq(Step... steps) {
        return Steps.sequence(java.util.Arrays.asList(steps));
    }

    private static Step par(Step... steps) {
        return Steps.parallel(steps);
    }

    /** Затемнение, телепорт невидимого игрока на сцену, включение камеры, титр интерлюдии. */
    private static void interludeStart(List<Step> s, String key, double playerX, double playerZ,
                                       Vec3 camPos, Vec3 camLook) {
        s.add(Steps.fade(0, 10, 50, true));
        s.add(Steps.run(c -> StoryWorld.ensureStages(c.trifas())));
        s.add(Steps.teleport(playerX, Y, playerZ, 0.0F));
        s.add(Steps.cinematicStart(camPos, camLook));
        s.add(Steps.title("story.fateapocrypha." + key + ".title", "story.fateapocrypha." + key + ".subtitle"));
        s.add(Steps.wait(60));
    }

    private static void interludeEnd(List<Step> s, StoryChapter next) {
        s.add(Steps.fade(30, 20, 5, true));
        s.add(Steps.despawnAllNpcs());
        s.add(Steps.cinematicEnd());
        s.add(Steps.advance(next));
    }

    // Интерлюдия 1: Мордред и Кайри Шишиго (Красная фракция)
    private static List<Step> interludeRed() {
        final double cx = StoryWorld.STAGE_RED_X + 0.5;
        final double cz = StoryWorld.STAGE_RED_Z + 0.5;
        List<Step> s = new ArrayList<>();
        interludeStart(s, "interlude_red", cx + 12, cz + 12, v(cx + 10, Y + 4.5, cz + 10), v(cx - 2, Y + 1.5, cz));
        s.add(Steps.spawnNpc("mordred", NpcType.MORDRED, cx + 2.0, Y, cz - 2.0, 135.0F));
        s.add(Steps.spawnNpc("kairi", NpcType.KAIRI, cx - 10.0, Y, cz + 7.0, -45.0F));

        // Общий план: Кайри подходит к костру
        s.add(par(
                cam(v(cx + 10, Y + 4.5, cz + 10), v(cx - 2, Y + 1.5, cz),
                        v(cx + 6, Y + 3.0, cz + 6), v(cx - 1.5, Y + 1.5, cz + 1.5), 200),
                seq(Steps.wait(20),
                        Steps.npcWalk("kairi", cx - 3.0, Y, cz + 3.5, 0.9D, 300, false))));

        // Разговор: двойной план, крупный план Мордред, крупный план Кайри, отъезд
        s.add(par(
                seq(
                        cam(v(cx + 6, Y + 2.2, cz + 3), v(cx - 0.5, Y + 1.6, cz + 1.2),
                                v(cx + 4, Y + 2.0, cz + 1.5), v(cx - 0.5, Y + 1.6, cz + 1.2), 330),
                        cam(v(cx + 5, Y + 2.0, cz - 5), v(cx + 2, Y + 1.7, cz - 2),
                                v(cx + 4.5, Y + 1.9, cz - 4), v(cx + 2, Y + 1.7, cz - 2), 330),
                        cam(v(cx - 7, Y + 2.2, cz + 7), v(cx - 3, Y + 1.7, cz + 3.5),
                                v(cx - 6, Y + 2.0, cz + 6), v(cx - 3, Y + 1.7, cz + 3.5), 330),
                        cam(v(cx - 4, Y + 2.5, cz + 7), v(cx, Y + 1.5, cz),
                                v(cx + 7, Y + 5.5, cz + 11), v(cx, Y + 1.5, cz), 220)),
                seq(say(KAIRI, "red.1"), say(MORDRED, "red.2"), say(KAIRI, "red.3"),
                        say(MORDRED, "red.4"), say(KAIRI, "red.5"), say(MORDRED, "red.6"),
                        say(KAIRI, "red.7"), say(MORDRED, "red.8"), say(KAIRI, "red.9"),
                        say(MORDRED, "red.10"))));
        interludeEnd(s, StoryChapter.INTERLUDE_SHIRO);
        return s;
    }

    // Интерлюдия 2: Широ Котомине раскрывает свою цель
    private static List<Step> interludeShiro() {
        final double cx = StoryWorld.STAGE_CHAPEL_X + 0.5;
        List<Step> s = new ArrayList<>();
        interludeStart(s, "interlude_shiro", cx, 893.5, v(cx, Y + 4.5, 893.0), v(cx, Y + 1.8, 912.0));
        s.add(Steps.spawnNpc("shiro", NpcType.SHIRO, cx, Y, 912.5, 180.0F));
        s.add(Steps.spawnNpc("semiramis", NpcType.SEMIRAMIS, cx, Y, 896.5, 0.0F));

        // Пролёт по нефу; Семирамис идёт к Широ
        s.add(par(
                cam(v(cx, Y + 4.5, 893.0), v(cx, Y + 1.8, 912.0), v(cx, Y + 2.8, 901.0), v(cx, Y + 1.8, 911.5), 220),
                seq(Steps.wait(20), Steps.npcWalk("semiramis", cx + 1.0, Y, 908.5, 0.9D, 300, false))));

        s.add(par(
                seq(
                        cam(v(798.5, Y + 2.1, 906.5), v(cx, Y + 1.7, 912.3),
                                v(799.2, Y + 2.0, 907.8), v(cx, Y + 1.7, 912.3), 350),
                        cam(v(803.5, Y + 2.0, 913.5), v(cx + 1.0, Y + 1.7, 908.5),
                                v(803.0, Y + 1.9, 912.5), v(cx + 1.0, Y + 1.7, 908.5), 350),
                        cam(v(806.5, Y + 2.5, 903.0), v(cx + 0.3, Y + 1.7, 910.5),
                                v(805.0, Y + 2.5, 906.0), v(cx + 0.3, Y + 1.7, 910.5), 330),
                        cam(v(cx, Y + 3.0, 902.0), v(cx, Y + 2.0, 911.0),
                                v(cx, Y + 6.0, 896.0), v(cx, Y + 2.5, 911.5), 330)),
                seq(say(SHIRO, "shiro.1"), say(SEMIRAMIS, "shiro.2"), say(SHIRO, "shiro.3"),
                        say(SEMIRAMIS, "shiro.4"), say(SHIRO, "shiro.5"), say(SEMIRAMIS, "shiro.6"),
                        say(SHIRO, "shiro.7"), say(SEMIRAMIS, "shiro.8"), say(SHIRO, "shiro.9"))));
        interludeEnd(s, StoryChapter.INTERLUDE_FRANKENSTEIN);
        return s;
    }

    // Интерлюдия 3: Жертва Франкенштейн (Каулес и Мордред)
    private static List<Step> interludeFrankenstein() {
        final double cx = StoryWorld.STAGE_RUINS_X + 0.5;
        final double cz = StoryWorld.STAGE_RUINS_Z + 0.5;
        List<Step> s = new ArrayList<>();
        interludeStart(s, "interlude_frankenstein", cx, cz + 12, v(cx - 4, Y + 6, cz + 10), v(cx - 11, Y + 1.5, cz));
        s.add(Steps.spawnNpc("caules", NpcType.CAULES, cx - 13.0, Y, cz, -90.0F));
        s.add(Steps.spawnNpc("frank", NpcType.FRANKENSTEIN, cx - 10.0, Y, cz, -90.0F));
        s.add(Steps.spawnNpc("mordred", NpcType.MORDRED, cx + 15.0, Y, cz, 90.0F));

        // 1. Каулес и Франкенштейн
        s.add(par(
                seq(
                        cam(v(cx - 4, Y + 6, cz + 10), v(cx - 11, Y + 1.5, cz),
                                v(cx - 8, Y + 3.5, cz + 7), v(cx - 11.5, Y + 1.6, cz), 160),
                        cam(v(cx - 7.5, Y + 2.0, cz + 3.5), v(cx - 11.5, Y + 1.7, cz),
                                v(cx - 8.5, Y + 1.9, cz + 3.0), v(cx - 11.5, Y + 1.7, cz), 160)),
                seq(say(CAULES, "frank.1"), say(FRANKENSTEIN, "frank.2"))));

        // 2. Появляется Мордред
        s.add(par(
                cam(v(cx + 6, Y + 3.0, cz + 6), v(cx + 12, Y + 1.7, cz),
                        v(cx + 2, Y + 2.6, cz + 6), v(cx - 4, Y + 1.7, cz), 180),
                Steps.npcWalk("mordred", cx + 2.0, Y, cz, 1.1D, 300, false)));
        s.add(par(
                seq(
                        cam(v(cx - 3, Y + 2.2, cz + 5), v(cx + 2, Y + 1.7, cz),
                                v(cx - 2, Y + 2.2, cz + 4.5), v(cx + 2, Y + 1.7, cz), 250),
                        cam(v(cx - 12, Y + 2.0, cz + 5), v(cx - 10, Y + 1.7, cz),
                                v(cx - 11, Y + 1.9, cz + 4.5), v(cx - 10, Y + 1.7, cz), 250)),
                seq(say(MORDRED, "frank.3"), say(CAULES, "frank.4"),
                        say(FRANKENSTEIN, "frank.5"), say(MORDRED, "frank.6"))));

        // 3. Бой: Мордред бросается вперёд, Франкенштейн отвечает молниями
        s.add(say(FRANKENSTEIN, "frank.7"));
        s.add(par(
                cam(v(cx - 2, Y + 4.5, cz + 9), v(cx - 6, Y + 1.5, cz),
                        v(cx - 6, Y + 3.5, cz + 8), v(cx - 8, Y + 1.5, cz), 70),
                Steps.npcWalk("mordred", cx - 7.5, Y, cz, 2.0D, 120, false)));
        s.add(Steps.npcSwing("mordred"));
        s.add(Steps.npcHurt("frank"));
        s.add(Steps.lightning(cx - 10.0, Y, cz));
        s.add(Steps.particles(ParticleTypes.ELECTRIC_SPARK, cx - 8.5, Y + 1.0, cz, 80, 1.5D));
        s.add(Steps.wait(8));
        s.add(Steps.lightning(cx - 7.0, Y, cz + 2.0));
        s.add(Steps.npcHurt("mordred"));
        s.add(Steps.wait(8));
        s.add(Steps.lightning(cx - 8.0, Y, cz - 2.5));
        s.add(Steps.wait(8));
        s.add(Steps.lightning(cx - 9.5, Y, cz + 1.5));
        s.add(Steps.particles(ParticleTypes.ELECTRIC_SPARK, cx - 7.5, Y + 1.0, cz, 100, 2.0D));
        s.add(Steps.wait(20));
        s.add(par(
                cam(v(cx - 3, Y + 2.0, cz + 4), v(cx - 7.5, Y + 1.6, cz),
                        v(cx - 4, Y + 1.9, cz + 3.5), v(cx - 7.5, Y + 1.6, cz), 120),
                seq(say(MORDRED, "frank.8"))));

        // 4. Гибель Франкенштейн
        s.add(par(
                seq(
                        cam(v(cx - 10, Y + 2.2, cz + 4.5), v(cx - 10.5, Y + 0.9, cz),
                                v(cx - 10.5, Y + 1.8, cz + 3.0), v(cx - 10.5, Y + 0.9, cz), 300),
                        cam(v(cx - 13, Y + 2.4, cz + 4), v(cx - 13, Y + 1.6, cz),
                                v(cx - 12.5, Y + 2.0, cz + 3), v(cx - 12.5, Y + 1.6, cz), 150)),
                seq(say(MORDRED, "frank.9"), say(FRANKENSTEIN, "frank.10"), say(CAULES, "frank.11"))));
        s.add(Steps.npcDie("frank"));
        s.add(par(
                cam(v(cx - 13, Y + 2.2, cz + 3.5), v(cx - 13, Y + 1.6, cz),
                        v(cx - 12, Y + 3.0, cz + 6), v(cx - 12.5, Y + 2.0, cz), 220),
                seq(Steps.wait(40), say(CAULES, "frank.12"))));

        // Конец интерлюдий: возвращаемся к Зигу
        s.add(Steps.fade(30, 20, 40, true));
        s.add(Steps.despawnAllNpcs());
        s.add(Steps.cinematicEnd());
        s.add(Steps.teleport(StoryWorld.FOREST_CX + 0.5, Y, StoryWorld.FOREST_CZ + 0.5, -90.0F));
        s.add(Steps.title("story.fateapocrypha.to_be_continued.title", "story.fateapocrypha.to_be_continued.subtitle"));
        s.add(Steps.wait(100));
        s.add(Steps.advance(StoryChapter.TO_BE_CONTINUED));
        return s;
    }
}
