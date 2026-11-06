package com.example.fateapocrypha.scene;

import com.example.fateapocrypha.ModItems;
import com.example.fateapocrypha.servant.ServantType;
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
    private static final String SIEGFRIED = "npc.fateapocrypha.siegfried";
    private static final String GORDES = "npc.fateapocrypha.gordes";
    private static final String CELENIKE = "npc.fateapocrypha.celenike";
    private static final String SPARTACUS = "npc.fateapocrypha.spartacus";
    private static final String JACK = "npc.fateapocrypha.jack";
    private static final String ATALANTA = "npc.fateapocrypha.atalanta";
    private static final String AVICEBRON = "npc.fateapocrypha.avicebron";
    private static final String ROCHE = "npc.fateapocrypha.roche";
    private static final String DARNIC = "npc.fateapocrypha.darnic";
    private static final String VLAD = "npc.fateapocrypha.vlad";
    private static final String REIKA = "npc.fateapocrypha.reika";
    private static final String FARMER = "npc.fateapocrypha.farmer";
    private static final String DRACULA = "npc.fateapocrypha.dracula";

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
            case INTERLUDE_SPARTACUS -> interludeSpartacus();
            case CELENIKE -> celenike();
            case FARM -> farm();
            case JACK -> jack();
            case INTERLUDE_AVICEBRON -> interludeAvicebron();
            case GOLEM_ADAM -> golemAdam();
            case INTERLUDE_DARNIC -> interludeDarnic();
            case INTERLUDE_GRAIL -> interludeGrail();
            case RESISTANCE -> resistance();
            case TO_BE_CONTINUED -> List.of();
        };
    }

    // 1. Пробуждение в резервуаре
    private static List<Step> labTank() {
        List<Step> s = new ArrayList<>();
        s.add(Steps.fade(0, 40, 60, false));
        s.add(Steps.run(c -> StoryWorld.repairTank(c.trifas())));
        s.add(Steps.teleport(115.5, Y, 115.5, 0.0F));
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
        s.add(Steps.teleport(127.5, Y, 115.5, -90.0F));
        s.add(Steps.wait(20));
        s.add(say(SIEG, "lab_walk.1"));
        s.add(say(SIEG, "lab_walk.2"));
        s.add(Steps.waitInZone(zone(148, 113, 154, 118)));
        s.add(say(SIEG, "lab_walk.3"));
        s.add(Steps.waitInZone(zone(172, 105, 178, 126)));
        s.add(say(SIEG, "lab_walk.4"));
        s.add(Steps.waitInZone(zone(184, 113, 189, 118)));
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

    // 6. Лес: големы, Зигфрид и Гордес, жертва сердца. Приходит Жанна, Зигфрид просит её позаботиться о Зиге.
    //    Другие Мастера и Слуги забирают Астольфо, а Жанна уводит Зига.
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

        // бой с големами: посреди боя приходят Зигфрид и его Мастер
        s.add(Steps.golemFight(new double[][] {
                {FX + 8.5, Y, FZ + 0.5}, {FX - 5.5, Y, FZ - 5.5}, {FX - 5.5, Y, FZ + 6.5}}, 1, 1500));
        s.add(Steps.spawnNpc("siegfried", NpcType.SIEGFRIED, FX + 0.5, Y, FZ - 10.5, 0.0F));
        s.add(Steps.spawnNpc("gordes", NpcType.GORDES, FX + 2.5, Y, FZ - 13.0, 0.0F));
        s.add(say(GORDES, "forest.5"));
        s.add(par(
                Steps.npcWalk("siegfried", FX + 0.5, Y, FZ - 4.0, 1.6D, 300, false),
                Steps.npcWalk("gordes", FX + 2.0, Y, FZ - 6.5, 1.1D, 300, false)));
        s.add(Steps.discardMobs());
        s.add(Steps.sound(SoundEvents.ANVIL_LAND));
        s.add(say(ASTOLFO, "forest.6"));
        s.add(say(GORDES, "forest.7"));

        // Гордес наносит смертельные раны
        s.add(Steps.lightningAtPlayer());
        s.add(Steps.hurtPlayerNearDeath());
        s.add(Steps.wait(30));
        s.add(say(SIEG, "forest.8"));
        s.add(say(ASTOLFO, "forest.9"));

        // Зигфрид отдаёт сердце: он хочет в последний раз кому-то помочь
        s.add(say(SIEGFRIED, "forest.10"));
        s.add(say(GORDES, "forest.11"));
        s.add(say(SIEGFRIED, "forest.12"));
        s.add(Steps.healPlayer());
        s.add(Steps.sound(SoundEvents.BEACON_ACTIVATE));

        // приходит Жанна; Зигфрид просит её позаботиться о Зиге
        s.add(Steps.spawnNpc("jeanne", NpcType.JEANNE, FX + 11.5, Y, FZ + 5.5, 90.0F));
        s.add(Steps.npcWalk("jeanne", FX + 2.0, Y, FZ + 2.5, 2.0D, 160, false));
        s.add(say(JEANNE, "forest.13"));
        s.add(say(SIEGFRIED, "forest.14"));
        s.add(say(JEANNE, "forest.15"));
        s.add(Steps.npcDie("siegfried"));
        s.add(Steps.wait(40));
        s.add(say(GORDES, "forest.16"));

        // другие Мастера и Слуги Чёрной фракции требуют вернуть Астольфо
        s.add(Steps.spawnNpc("darnic", NpcType.DARNIC, FX - 8.5, Y, FZ - 14.0, 0.0F));
        s.add(Steps.spawnNpc("vlad", NpcType.VLAD, FX - 11.0, Y, FZ - 14.0, 0.0F));
        s.add(Steps.spawnNpc("fiore", NpcType.FIORE, FX - 14.0, Y, FZ - 12.0, 0.0F));
        s.add(Steps.spawnNpc("chiron", NpcType.CHIRON, FX - 16.0, Y, FZ - 10.0, 0.0F));
        s.add(par(
                Steps.npcWalk("darnic", FX - 2.5, Y, FZ - 6.0, 1.1D, 300, false),
                Steps.npcWalk("vlad", FX - 5.0, Y, FZ - 5.0, 1.2D, 300, false),
                Steps.npcWalk("fiore", FX - 7.0, Y, FZ - 3.0, 1.0D, 300, false),
                Steps.npcWalk("chiron", FX - 8.0, Y, FZ - 1.0, 1.1D, 300, false)));
        s.add(say(DARNIC, "forest.17"));
        s.add(say(ASTOLFO, "forest.18"));
        s.add(say(VLAD, "forest.19"));
        s.add(say(JEANNE, "forest.20"));
        s.add(say(CHIRON, "forest.21"));
        s.add(say(ASTOLFO, "forest.22"));

        // Астольфо уводят
        s.add(par(
                Steps.npcWalk("astolfo", FX - 12.0, Y, FZ - 12.0, 0.9D, 300, false),
                Steps.npcWalk("darnic", FX - 14.0, Y, FZ - 15.0, 1.0D, 300, false),
                Steps.npcWalk("vlad", FX - 14.0, Y, FZ - 13.0, 1.0D, 300, false),
                Steps.npcWalk("fiore", FX - 15.0, Y, FZ - 11.0, 1.0D, 300, false),
                Steps.npcWalk("chiron", FX - 16.0, Y, FZ - 9.0, 1.0D, 300, false),
                Steps.npcWalk("gordes", FX - 9.0, Y, FZ - 14.0, 0.8D, 300, false)));
        s.add(Steps.npcLeave("astolfo"));
        s.add(Steps.npcLeave("darnic"));
        s.add(Steps.npcLeave("vlad"));
        s.add(Steps.npcLeave("fiore"));
        s.add(Steps.npcLeave("chiron"));
        s.add(Steps.npcLeave("gordes"));
        s.add(Steps.wait(40));

        // Жанна забирает Зига и даёт ему имя
        s.add(say(SIEG, "forest.23"));
        s.add(say(JEANNE, "forest.24"));
        s.add(say(SIEG, "forest.25"));
        s.add(say(JEANNE, "forest.26"));
        s.add(say(SIEG, "forest.27"));
        s.add(say(JEANNE, "forest.28"));
        s.add(Steps.wait(30));
        s.add(Steps.fade(30, 10, 5, true));
        s.add(Steps.despawnAllNpcs());
        s.add(Steps.advance(StoryChapter.FARM));
        return s;
    }

    // ================= Глава Зига: ферма =================
    // Жанна выполняет просьбу Зигфрида: приводит Зига на ферму к фермеру, где ему будет хорошо.
    private static List<Step> farm() {
        final double fx = 1189.5;
        List<Step> s = new ArrayList<>();
        s.add(Steps.fade(0, 20, 40, false));
        s.add(Steps.run(c -> StoryWorld.ensureFarm(c.trifas())));
        s.add(Steps.teleport(fx, Y, 1217.5, 180.0F));
        s.add(Steps.spawnNpc("jeanne", NpcType.JEANNE, fx + 1.5, Y, 1217.5, 180.0F));
        s.add(Steps.spawnNpc("farmer", NpcType.FARMER, fx, Y, 1190.5, 0.0F));
        s.add(Steps.spawnAnimals(new double[][] {
                {1177.5, Y, 1200.5}, {1180.5, Y, 1205.5}, {1199.5, Y, 1203.5},
                {1204.5, Y, 1200.5}, {1202.5, Y, 1197.5}, {1176.5, Y, 1197.5}}));
        s.add(Steps.title("story.fateapocrypha.farm.title", "story.fateapocrypha.farm.subtitle"));
        s.add(Steps.wait(80));
        s.add(Steps.npcWalk("jeanne", fx, Y, 1205.5, 0.8D, 300, false));
        s.add(say(JEANNE, "farm.1"));
        s.add(Steps.npcWalk("farmer", fx, Y, 1198.5, 0.8D, 300, false));
        s.add(Steps.waitInZone(zone(1186, 1196, 1193, 1208)));
        s.add(say(FARMER, "farm.2"));
        s.add(say(JEANNE, "farm.3"));
        s.add(say(FARMER, "farm.4"));
        s.add(say(SIEG, "farm.5"));
        s.add(say(FARMER, "farm.6"));

        // несколько дней спустя
        s.add(Steps.title("story.fateapocrypha.time_skip.title", "story.fateapocrypha.time_skip.subtitle"));
        s.add(Steps.fade(30, 40, 40, true));
        s.add(Steps.npcTeleport("jeanne", fx, Y, 1194.0, 180.0F));
        s.add(Steps.npcTeleport("farmer", 1181.5, Y, 1205.5, 90.0F));
        s.add(Steps.teleport(fx, Y, 1205.5, 180.0F));
        s.add(Steps.waitInZone(zone(1186, 1191, 1193, 1197)));
        s.add(say(JEANNE, "farm.7"));
        s.add(say(SIEG, "farm.8"));
        s.add(say(JEANNE, "farm.9"));
        s.add(say(SIEG, "farm.10"));
        s.add(say(JEANNE, "farm.11"));
        s.add(say(SIEG, "farm.12"));
        s.add(say(JEANNE, "farm.13"));
        s.add(say(FARMER, "farm.14"));
        s.add(Steps.wait(30));
        s.add(Steps.fade(30, 10, 5, true));
        s.add(Steps.despawnAllNpcs());
        s.add(Steps.discardMobs());
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
        s.add(Steps.run(c -> StoryWorld.ensureStagesB(c.trifas())));
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
                seq(say(MORDRED, "red.1"), say(KAIRI, "red.2"), say(MORDRED, "red.3"),
                        say(KAIRI, "red.4"), say(MORDRED, "red.5"), say(KAIRI, "red.6"),
                        say(MORDRED, "red.7"), say(KAIRI, "red.8"), say(MORDRED, "red.9"),
                        say(KAIRI, "red.10"), say(MORDRED, "red.11"))));
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
        interludeEnd(s, StoryChapter.INTERLUDE_SPARTACUS);
        return s;
    }

    // ================= Глава Зига: Селеника =================
    // На поле боя Селеника нападает на Райдера и Зига. Разъярённая заботой Астольфо о Зиге, она двумя
    // Командными заклинаниями заставляет его напасть на Зига. Прежде чем она использует последнее, чтобы
    // приказать убить Зига, Мордред убивает её из презрения. Зиг становится Мастером Астольфо.
    private static List<Step> celenike() {
        List<Step> s = new ArrayList<>();
        s.add(Steps.fade(0, 20, 40, false));
        s.add(Steps.run(c -> StoryWorld.ensureBuilt(c.trifas())));
        s.add(Steps.run(c -> StoryWorld.openGate(c.trifas())));
        s.add(Steps.teleport(196.5, Y, 407.5, -90.0F));
        s.add(Steps.spawnNpc("astolfo", NpcType.ASTOLFO, 199.5, Y, 405.5, 90.0F));
        s.add(Steps.wait(60));
        s.add(say(ASTOLFO, "celenike.a1"));
        s.add(say(SIEG, "celenike.a2"));
        s.add(Steps.spawnNpc("celenike", NpcType.CELENIKE, 207.5, Y, 409.5, 90.0F));

        // Селеника появляется на поле боя и вступает в схватку
        s.add(Steps.npcWalk("celenike", 203.0, Y, 409.0, 1.1D, 300, false));
        s.add(say(CELENIKE, "celenike.1"));
        s.add(say(ASTOLFO, "celenike.2"));
        s.add(say(CELENIKE, "celenike.3"));
        s.add(Steps.lightning(201.0, Y, 406.0));
        s.add(Steps.npcHurt("astolfo"));
        s.add(Steps.wait(10));
        s.add(Steps.lightning(198.0, Y, 409.0));
        s.add(Steps.sound(SoundEvents.ANVIL_LAND));
        s.add(say(SIEG, "celenike.4"));

        // Первое Командное заклинание: Райдер должен напасть на Зига
        s.add(say(CELENIKE, "celenike.5"));
        s.add(Steps.particlesAtNpc("astolfo", ParticleTypes.FLAME, 40));
        s.add(Steps.sound(SoundEvents.BEACON_POWER_SELECT));
        s.add(Steps.npcWalk("astolfo", 198.0, Y, 407.0, 0.6D, 200, false));
        s.add(Steps.npcSwing("astolfo"));
        s.add(say(ASTOLFO, "celenike.6"));

        // Второе заклинание: Астольфо едва сдерживается
        s.add(say(CELENIKE, "celenike.7"));
        s.add(Steps.particlesAtNpc("astolfo", ParticleTypes.FLAME, 60));
        s.add(Steps.npcSwing("astolfo"));
        s.add(Steps.npcHurt("astolfo"));
        s.add(say(ASTOLFO, "celenike.8"));

        // Последнее заклинание не звучит до конца: появляется Мордред
        s.add(say(CELENIKE, "celenike.9"));
        s.add(Steps.spawnNpc("mordred", NpcType.MORDRED, 209.5, Y, 405.5, 90.0F));
        s.add(Steps.npcWalk("mordred", 204.5, Y, 408.5, 2.2D, 100, false));
        s.add(say(MORDRED, "celenike.10"));
        s.add(Steps.npcSwing("mordred"));
        s.add(Steps.sound(SoundEvents.PLAYER_ATTACK_SWEEP));
        s.add(Steps.particlesAtNpc("celenike", ParticleTypes.SWEEP_ATTACK, 3));
        s.add(Steps.npcDie("celenike"));
        s.add(Steps.wait(30));
        s.add(say(MORDRED, "celenike.11"));
        s.add(say(MORDRED, "celenike.12"));
        s.add(Steps.npcLeave("mordred"));

        // Астольфо освобождён от приказов, но остался без Мастера
        s.add(Steps.wait(30));
        s.add(say(ASTOLFO, "celenike.13"));
        s.add(say(SIEG, "celenike.14"));
        s.add(say(ASTOLFO, "celenike.15"));
        s.add(say(SIEG, "celenike.16"));

        // Зиг становится Мастером Астольфо: Командное заклинание и Слуга рядом
        s.add(Steps.despawnAllNpcs());
        s.add(Steps.bindServant(ServantType.ASTOLFO));
        s.add(Steps.give(() -> new ItemStack(ModItems.COMMAND_SEAL.get())));
        s.add(Steps.sound(SoundEvents.BEACON_ACTIVATE));
        s.add(say(ASTOLFO, "celenike.17"));
        s.add(Steps.wait(60));
        s.add(Steps.fade(30, 10, 5, true));
        s.add(Steps.dismissServant());
        s.add(Steps.advance(StoryChapter.JACK));
        return s;
    }

    // ================= Интерлюдия: гнев Спартака (третье лицо) =================
    // Широ направляет Спартака на замок рода Иггдмилления. Спартак не терпит приказов и выходит из-под контроля.
    // Мордред останавливает его, пока он не разрушил всё вокруг.
    private static List<Step> interludeSpartacus() {
        final double cx = StoryWorld.STAGE_SIEGE_X + 0.5;
        List<Step> s = new ArrayList<>();
        interludeStart(s, "interlude_spartacus", cx, 884.5, v(cx - 10, Y + 6, 880), v(cx - 4, Y + 2, 890));
        s.add(Steps.run(c -> StoryWorld.repairSiegeWall(c.trifas())));
        s.add(Steps.spawnNpc("spartacus", NpcType.SPARTACUS, cx - 4, Y, 890.5, 0.0F));
        s.add(Steps.spawnNpc("shiro", NpcType.SHIRO, cx + 6, Y, 886.5, 90.0F));
        s.add(Steps.spawnNpc("mordred", NpcType.MORDRED, cx + 16, Y, 893.5, 90.0F));

        // 1. Широ направляет Спартака; тот отказывается подчиняться
        s.add(par(
                seq(cam(v(cx - 10, Y + 6, 880), v(cx - 4, Y + 2, 890), v(cx - 8, Y + 3.5, 884), v(cx - 4, Y + 2, 890), 200),
                        cam(v(cx + 2, Y + 2.5, 884), v(cx + 6, Y + 1.8, 886.5), v(cx + 3, Y + 2.2, 883.5), v(cx + 6, Y + 1.8, 886.5), 260),
                        cam(v(cx - 8, Y + 2.3, 886), v(cx - 4, Y + 2.2, 890), v(cx - 7, Y + 2.2, 887), v(cx - 4, Y + 2.2, 890), 260)),
                seq(say(SHIRO, "spartacus.1"), say(SPARTACUS, "spartacus.2"),
                        say(SHIRO, "spartacus.3"), say(SPARTACUS, "spartacus.4"))));

        // 2. Спартак бросается на стену замка
        s.add(par(
                cam(v(cx - 12, Y + 3, 888), v(cx - 4, Y + 2.5, 893), v(cx - 6, Y + 6, 888), v(cx, Y + 3, 899), 120),
                Steps.npcWalk("spartacus", cx, Y, 897.5, 1.7D, 150, false)));
        s.add(Steps.run(c -> StoryWorld.smashSiegeWall(c.trifas())));
        s.add(Steps.particles(ParticleTypes.EXPLOSION_EMITTER, cx, Y + 3, 900.5, 3, 1.0D));
        s.add(Steps.sound(SoundEvents.GENERIC_EXPLODE));
        s.add(Steps.wait(30));

        // 3. Появляется Мордред
        s.add(par(
                cam(v(cx + 10, Y + 3, 890), v(cx + 16, Y + 1.8, 893.5), v(cx + 8, Y + 2.8, 893), v(cx + 2, Y + 1.8, 897), 160),
                Steps.npcWalk("mordred", cx + 5, Y, 896.0, 1.8D, 150, false)));
        s.add(par(
                seq(cam(v(cx + 7, Y + 2.2, 891), v(cx + 4, Y + 1.8, 896.5), v(cx + 6, Y + 2.2, 892), v(cx + 4, Y + 1.8, 896.5), 200),
                        cam(v(cx - 6, Y + 2.8, 896), v(cx, Y + 2.2, 897.5), v(cx - 5, Y + 2.8, 894), v(cx, Y + 2.2, 897.5), 200)),
                seq(say(MORDRED, "spartacus.5"), say(MORDRED, "spartacus.6"), say(SPARTACUS, "spartacus.7"))));

        // 4. Бой и гибель Спартака
        s.add(par(
                cam(v(cx + 3, Y + 4.5, 889), v(cx + 1.5, Y + 2, 897), v(cx + 2, Y + 3.5, 891), v(cx + 1.5, Y + 2, 897), 90),
                Steps.npcWalk("mordred", cx + 1.8, Y, 897.2, 2.4D, 100, false)));
        s.add(Steps.npcSwing("mordred"));
        s.add(Steps.npcHurt("spartacus"));
        s.add(Steps.lightning(cx, Y, 897.5));
        s.add(Steps.wait(10));
        s.add(Steps.npcSwing("mordred"));
        s.add(Steps.npcHurt("spartacus"));
        s.add(Steps.lightning(cx - 1, Y, 898.5));
        s.add(Steps.particles(ParticleTypes.ELECTRIC_SPARK, cx, Y + 1.2, 897.5, 80, 1.5D));
        s.add(Steps.wait(20));
        s.add(par(
                cam(v(cx + 3, Y + 2.0, 893), v(cx + 0.8, Y + 1.7, 897.4), v(cx + 2.5, Y + 1.9, 894), v(cx + 0.8, Y + 1.7, 897.4), 200),
                say(MORDRED, "spartacus.8")));
        s.add(Steps.npcDie("spartacus"));
        s.add(par(
                cam(v(cx + 4, Y + 2.2, 895), v(cx + 4.5, Y + 1.8, 897), v(cx + 8, Y + 5.5, 888), v(cx, Y + 3, 899), 260),
                seq(Steps.wait(30), say(MORDRED, "spartacus.9"))));
        interludeEnd(s, StoryChapter.CELENIKE);
        return s;
    }

    // ================= Глава Зига: иллюзия Джека и Атланта =================
    // Зиг и Жанна попадают в иллюзию. Джек-потрошитель это слитые души детей. Атланта защищает Джека
    // и возненавидит Жанну, когда та освободит эти души.
    private static List<Step> jack() {
        final double zc = StoryWorld.STAGE_STREET_Z + 0.5;
        final double lz = StoryWorld.STAGE_LONDON_Z + 0.5;
        List<Step> s = new ArrayList<>();
        s.add(Steps.fade(0, 20, 40, false));
        s.add(Steps.dismissServant());
        s.add(Steps.run(c -> StoryWorld.ensureStagesB(c.trifas())));
        s.add(Steps.teleport(1185.5, Y, zc, -90.0F));
        s.add(Steps.spawnNpc("jeanne", NpcType.JEANNE, 1187.5, Y, zc + 1.5, -90.0F));
        s.add(Steps.title("story.fateapocrypha.jack.title", "story.fateapocrypha.jack.subtitle"));
        s.add(Steps.wait(80));
        s.add(say(JEANNE, "jack.a1"));
        s.add(say(SIEG, "jack.a2"));
        s.add(say(JEANNE, "jack.1"));
        s.add(Steps.particles(ParticleTypes.CLOUD, 1190.5, Y + 0.5, zc, 200, 4.0D));
        s.add(Steps.fade(40, 20, 40, true));

        // иллюзорный Лондон
        s.add(Steps.teleport(1185.5, Y, lz, -90.0F));
        s.add(Steps.npcTeleport("jeanne", 1187.5, Y, lz + 1.5, -90.0F));
        s.add(Steps.particles(ParticleTypes.CLOUD, 1195.5, Y + 0.5, lz, 250, 6.0D));
        s.add(Steps.wait(40));
        s.add(say(JEANNE, "jack.2"));
        s.add(say(SIEG, "jack.3"));
        s.add(Steps.spawnNpc("jack", NpcType.JACK, 1205.5, Y, lz, 90.0F));
        s.add(Steps.npcWalk("jack", 1202.5, Y, lz, 0.8D, 300, false));
        s.add(Steps.spawnNpc("reika", NpcType.REIKA, 1210.5, Y, lz + 1.5, 90.0F));
        s.add(say(REIKA, "jack.r1"));
        s.add(say(JACK, "jack.r2"));
        s.add(Steps.npcWalk("jack", 1208.0, Y, lz + 1.0, 1.6D, 120, false));
        s.add(Steps.npcSwing("jack"));
        s.add(Steps.npcHurt("reika"));
        s.add(Steps.lightning(1210.5, Y, lz + 1.5));
        s.add(say(REIKA, "jack.r3"));
        s.add(Steps.npcLeave("reika"));
        s.add(Steps.npcWalk("jack", 1197.5, Y, lz, 1.0D, 200, false));
        s.add(say(JACK, "jack.4"));

        // бой с тенями
        s.add(Steps.shadeFight(new double[][] {
                {1195.5, Y + 1.5, lz - 3}, {1195.5, Y + 1.5, lz + 3}, {1199.5, Y + 2.0, lz}, {1191.5, Y + 1.5, lz}}));
        s.add(say(JEANNE, "jack.5"));
        s.add(say(SIEG, "jack.6"));
        s.add(say(JACK, "jack.7"));

        // Атланта защищает Джека
        s.add(Steps.spawnNpc("atalanta", NpcType.ATALANTA, 1212.5, Y, lz, 90.0F));
        s.add(Steps.npcWalk("atalanta", 1199.5, Y, lz + 2.0, 2.0D, 120, false));
        s.add(say(ATALANTA, "jack.8"));
        s.add(say(JEANNE, "jack.9"));
        s.add(say(ATALANTA, "jack.10"));
        s.add(say(JEANNE, "jack.11"));

        // Жанна освобождает души детей
        s.add(Steps.particlesAtNpc("jack", ParticleTypes.END_ROD, 150));
        s.add(Steps.particlesAtNpc("jeanne", ParticleTypes.END_ROD, 100));
        s.add(Steps.sound(SoundEvents.BEACON_ACTIVATE));
        s.add(Steps.wait(40));
        s.add(Steps.npcLeave("jack"));
        s.add(say(ATALANTA, "jack.12"));
        s.add(say(ATALANTA, "jack.13"));
        s.add(Steps.npcLeave("atalanta"));
        s.add(Steps.wait(30));
        s.add(say(SIEG, "jack.14"));
        s.add(say(JEANNE, "jack.15"));

        // иллюзия рассеивается
        s.add(Steps.fade(40, 20, 40, true));
        s.add(Steps.teleport(1185.5, Y, zc, -90.0F));
        s.add(Steps.npcTeleport("jeanne", 1187.5, Y, zc + 1.5, -90.0F));
        s.add(Steps.title("story.fateapocrypha.jack_end.title", "story.fateapocrypha.jack_end.subtitle"));
        s.add(Steps.wait(80));
        s.add(Steps.fade(30, 10, 5, true));
        s.add(Steps.despawnAllNpcs());
        s.add(Steps.discardMobs());
        s.add(Steps.advance(StoryChapter.INTERLUDE_AVICEBRON));
        return s;
    }

    // ================= Интерлюдия: мечта Авицеброна (третье лицо) =================
    // Авицеброн предаёт Чёрных ради воплощения идеала, жертвует своим Мастером Роше и пробуждает Голема Адама.
    private static List<Step> interludeAvicebron() {
        final double px = StoryWorld.STAGE_PLAIN_X + 0.5;
        final double pz = StoryWorld.STAGE_PLAIN_Z + 0.5;
        List<Step> s = new ArrayList<>();
        interludeStart(s, "interlude_avicebron", px, pz + 20, v(px, Y + 6, pz + 18), v(px, Y + 2, pz));
        s.add(Steps.spawnNpc("avicebron", NpcType.AVICEBRON, px, Y, pz, 0.0F));
        s.add(Steps.spawnNpc("shiro", NpcType.SHIRO, px + 7, Y, pz + 4, 135.0F));
        s.add(Steps.spawnNpc("roche", NpcType.ROCHE, px - 5.5, Y, pz + 4.5, 45.0F));

        // 1. Авицеброн и Широ: общая мечта
        s.add(par(
                seq(cam(v(px, Y + 6, pz + 18), v(px, Y + 2, pz), v(px + 2, Y + 3.5, pz + 11), v(px, Y + 2, pz), 200),
                        cam(v(px - 4, Y + 2.4, pz + 6), v(px, Y + 1.8, pz), v(px - 3, Y + 2.2, pz + 4), v(px, Y + 1.8, pz), 260)),
                seq(say(AVICEBRON, "avi.1"), say(SHIRO, "avi.2"))));

        // 2. Предательство Роше
        s.add(par(
                seq(cam(v(px - 10, Y + 2.5, pz + 10), v(px - 5.5, Y + 1.7, pz + 4.5), v(px - 8, Y + 2.3, pz + 8), v(px - 5.5, Y + 1.7, pz + 4.5), 260),
                        cam(v(px + 3, Y + 2.2, pz + 6), v(px, Y + 1.8, pz), v(px + 2, Y + 2.0, pz + 4.5), v(px, Y + 1.8, pz), 260),
                        cam(v(px - 8, Y + 2.3, pz + 8), v(px - 5.5, Y + 1.7, pz + 4.5), v(px - 7, Y + 2.0, pz + 7), v(px - 5.5, Y + 1.7, pz + 4.5), 160)),
                seq(say(ROCHE, "avi.3"), say(AVICEBRON, "avi.4"), say(ROCHE, "avi.5"))));
        s.add(Steps.npcWalk("avicebron", px - 3.5, Y, pz + 3.5, 1.3D, 120, false));
        s.add(Steps.npcSwing("avicebron"));
        s.add(Steps.lightning(px - 5.5, Y, pz + 4.5));
        s.add(Steps.npcHurt("roche"));
        s.add(Steps.wait(15));
        s.add(Steps.npcSwing("avicebron"));
        s.add(Steps.lightning(px - 5.5, Y, pz + 4.5));
        s.add(Steps.npcDie("roche"));
        s.add(Steps.npcWalk("avicebron", px, Y, pz, 1.0D, 120, false));
        s.add(Steps.wait(40));

        // 3. Пробуждение Голема Адама
        s.add(par(
                seq(cam(v(px, Y + 3, pz + 8), v(px, Y + 2, pz), v(px, Y + 7, pz + 15), v(px, Y + 3, pz), 320)),
                seq(say(AVICEBRON, "avi.6"), say(SHIRO, "avi.7"))));
        s.add(Steps.lightning(px, Y, pz));
        s.add(Steps.wait(10));
        s.add(Steps.lightning(px + 3, Y, pz + 2));
        s.add(Steps.particles(ParticleTypes.EXPLOSION_EMITTER, px, Y + 2, pz, 3, 1.5D));
        s.add(Steps.sound(SoundEvents.GENERIC_EXPLODE));
        s.add(Steps.wait(40));
        interludeEnd(s, StoryChapter.GOLEM_ADAM);
        return s;
    }

    // ================= Глава Зига: Голем Адам =================
    // Зиг, Жанна и Астольфо останавливают Голема Адама. Идеал Авицеброна рушится вместе с ним.
    private static List<Step> golemAdam() {
        final double px = StoryWorld.STAGE_PLAIN_X + 0.5;
        final double pz = StoryWorld.STAGE_PLAIN_Z + 0.5;
        List<Step> s = new ArrayList<>();
        s.add(Steps.fade(0, 20, 40, false));
        s.add(Steps.dismissServant());
        s.add(Steps.run(c -> StoryWorld.ensureStagesB(c.trifas())));
        s.add(Steps.teleport(px, Y, pz + 20, 180.0F));
        s.add(Steps.restoreServant(ServantType.ASTOLFO));
        s.add(Steps.spawnNpc("jeanne", NpcType.JEANNE, px - 3, Y, pz + 21.5, 180.0F));
        s.add(Steps.spawnNpc("avicebron", NpcType.AVICEBRON, px, Y, pz, 0.0F));
        s.add(Steps.title("story.fateapocrypha.golem_adam.title", "story.fateapocrypha.golem_adam.subtitle"));
        s.add(Steps.wait(80));
        s.add(say(JEANNE, "gol.1"));
        s.add(say(SIEG, "gol.2"));
        s.add(say(ASTOLFO, "gol.3"));
        s.add(say(AVICEBRON, "gol.4"));
        s.add(say(JEANNE, "gol.5"));
        s.add(Steps.particles(ParticleTypes.HAPPY_VILLAGER, px, Y + 1, pz + 8, 200, 6.0D));
        s.add(Steps.sound(SoundEvents.ANVIL_LAND));
        s.add(Steps.bossFight(new double[][] {{px, Y, pz + 6}}, 300.0D, 12.0D, "npc.fateapocrypha.golem_adam"));
        s.add(Steps.sound(SoundEvents.GENERIC_EXPLODE));
        s.add(Steps.wait(30));
        s.add(say(AVICEBRON, "gol.6"));
        s.add(say(JEANNE, "gol.7"));
        s.add(say(AVICEBRON, "gol.8"));
        s.add(Steps.npcDie("avicebron"));
        s.add(Steps.wait(30));
        s.add(say(SIEG, "gol.9"));
        s.add(say(ASTOLFO, "gol.10"));
        s.add(Steps.fade(30, 10, 5, true));
        s.add(Steps.despawnAllNpcs());
        s.add(Steps.discardMobs());
        s.add(Steps.dismissServant());
        s.add(Steps.advance(StoryChapter.INTERLUDE_DARNIC));
        return s;
    }

    // ================= Интерлюдия: падение Чёрной фракции (третье лицо) =================
    // Дэрник проигрывает войну и сливает свою душу с Владом III. Тот превращается в Дракулу и теряет
    // контроль. Жанна и Мордред останавливают его. Влад гибнет, Дэрник исчезает вместе с ним.
    private static List<Step> interludeDarnic() {
        final double cx = StoryWorld.STAGE_RUINS_X + 0.5;
        final double cz = StoryWorld.STAGE_RUINS_Z + 0.5;
        List<Step> s = new ArrayList<>();
        interludeStart(s, "interlude_darnic", cx, cz + 12, v(cx - 3, Y + 5, cz + 10), v(cx - 7, Y + 1.8, cz));
        s.add(Steps.spawnNpc("darnic", NpcType.DARNIC, cx - 9.0, Y, cz, -90.0F));
        s.add(Steps.spawnNpc("vlad", NpcType.VLAD, cx - 5.5, Y, cz, 90.0F));

        // 1. Дэрник предлагает слить души
        s.add(par(
                seq(cam(v(cx - 3, Y + 5, cz + 10), v(cx - 7, Y + 1.8, cz), v(cx - 5, Y + 3.2, cz + 7), v(cx - 7, Y + 1.8, cz), 200),
                        cam(v(cx - 4, Y + 2.2, cz + 3), v(cx - 5.5, Y + 1.8, cz), v(cx - 4.5, Y + 2.0, cz + 2.6), v(cx - 5.5, Y + 1.8, cz), 280),
                        cam(v(cx - 11, Y + 2.2, cz + 3), v(cx - 9, Y + 1.8, cz), v(cx - 10.5, Y + 2.0, cz + 2.6), v(cx - 9, Y + 1.8, cz), 280),
                        cam(v(cx - 7, Y + 2.6, cz + 5), v(cx - 7, Y + 1.8, cz), v(cx - 7, Y + 3.0, cz + 6.5), v(cx - 7, Y + 1.8, cz), 200)),
                seq(say(VLAD, "darnic.1"), say(DARNIC, "darnic.2"), say(VLAD, "darnic.3"),
                        say(DARNIC, "darnic.4"), say(VLAD, "darnic.5"), say(DARNIC, "darnic.6"))));

        // 2. слияние душ: Влад становится Дракулой
        s.add(Steps.lightning(cx - 7.0, Y, cz));
        s.add(Steps.particles(ParticleTypes.SOUL_FIRE_FLAME, cx - 7.0, Y + 1.0, cz, 180, 2.0D));
        s.add(Steps.sound(SoundEvents.GENERIC_EXPLODE));
        s.add(Steps.wait(15));
        s.add(Steps.lightning(cx - 5.0, Y, cz + 1.0));
        s.add(Steps.npcLeave("darnic"));
        s.add(Steps.npcRename("vlad", "npc.fateapocrypha.dracula"));
        s.add(par(
                cam(v(cx - 2, Y + 2.0, cz + 4), v(cx - 5.5, Y + 1.8, cz), v(cx - 3.5, Y + 3.0, cz + 6), v(cx - 5.5, Y + 2.2, cz), 200),
                seq(Steps.wait(20), say(DRACULA, "darnic.7"))));
        s.add(Steps.lightning(cx - 3.0, Y, cz - 3.0));
        s.add(Steps.lightning(cx - 8.0, Y, cz + 4.0));

        // 3. Жанна и Мордред вмешиваются
        s.add(Steps.spawnNpc("jeanne", NpcType.JEANNE, cx + 15.0, Y, cz + 3.0, 90.0F));
        s.add(Steps.spawnNpc("mordred", NpcType.MORDRED, cx + 15.0, Y, cz - 3.0, 90.0F));
        s.add(par(
                cam(v(cx + 8, Y + 3.0, cz + 8), v(cx + 12, Y + 1.8, cz), v(cx + 2, Y + 2.8, cz + 6), v(cx - 2, Y + 1.8, cz), 180),
                Steps.npcWalk("jeanne", cx + 1.5, Y, cz + 2.5, 1.8D, 150, false),
                Steps.npcWalk("mordred", cx + 1.5, Y, cz - 2.5, 1.8D, 150, false)));
        s.add(par(
                cam(v(cx + 4, Y + 2.2, cz + 4), v(cx + 0.5, Y + 1.8, cz + 1.5), v(cx + 3, Y + 2.2, cz + 3), v(cx + 0.5, Y + 1.8, cz + 1.5), 240),
                seq(say(JEANNE, "darnic.8"), say(MORDRED, "darnic.9"))));

        // 4. бой с Дракулой
        s.add(par(
                cam(v(cx - 1, Y + 5.5, cz + 8), v(cx - 4, Y + 1.8, cz), v(cx - 4, Y + 3.5, cz + 5), v(cx - 5, Y + 1.8, cz), 90),
                Steps.npcWalk("mordred", cx - 3.0, Y, cz - 1.5, 2.2D, 100, false),
                Steps.npcWalk("jeanne", cx - 2.5, Y, cz + 2.0, 2.0D, 100, false)));
        s.add(Steps.npcSwing("mordred"));
        s.add(Steps.lightning(cx - 5.0, Y, cz));
        s.add(Steps.particlesAtNpc("jeanne", ParticleTypes.FLAME, 80));
        s.add(Steps.npcSwing("jeanne"));
        s.add(Steps.npcHurt("vlad"));
        s.add(Steps.wait(10));
        s.add(Steps.particles(ParticleTypes.FLAME, cx - 5.0, Y + 1.0, cz, 120, 1.8D));
        s.add(Steps.npcSwing("mordred"));
        s.add(Steps.lightning(cx - 5.5, Y, cz + 0.5));
        s.add(Steps.npcHurt("vlad"));
        s.add(Steps.wait(20));
        s.add(par(
                cam(v(cx - 1.5, Y + 2.0, cz + 3), v(cx - 5, Y + 1.8, cz), v(cx - 2.5, Y + 2.0, cz + 2.5), v(cx - 5, Y + 1.8, cz), 220),
                say(VLAD, "darnic.10")));
        s.add(Steps.npcDie("vlad"));
        s.add(par(
                cam(v(cx + 1, Y + 2.2, cz + 3), v(cx + 0.5, Y + 1.8, cz), v(cx + 4, Y + 4.5, cz + 8), v(cx, Y + 2.5, cz), 300),
                seq(Steps.wait(30), say(JEANNE, "darnic.11"), say(MORDRED, "darnic.12"))));
        interludeEnd(s, StoryChapter.INTERLUDE_GRAIL);
        return s;
    }

    // ================= Интерлюдия: Висячие сады (третье лицо) =================
    // Чёрная фракция разрушена. Широ получает Грааль, Семирамис поднимает Висячие сады Вавилона.
    private static List<Step> interludeGrail() {
        final double gx = StoryWorld.GARDEN_X + 0.5;
        final double gz = StoryWorld.GARDEN_Z + 0.5;
        final double top = Y + 60;   // уровень, на котором стоят герои в садах
        List<Step> s = new ArrayList<>();
        interludeStart(s, "interlude_grail", gx, gz - 18, v(gx, Y + 1.5, gz - 24), v(gx, Y + 52, gz));
        s.add(Steps.run(c -> StoryWorld.ensureStages(c.trifas())));
        s.add(Steps.spawnNpc("shiro", NpcType.SHIRO, gx, top, gz + 2.5, 180.0F));
        s.add(Steps.spawnNpc("semiramis", NpcType.SEMIRAMIS, gx + 2.0, top, gz + 0.5, 150.0F));

        // 1. пустое небо; голос Широ и приказ Семирамис
        s.add(par(
                cam(v(gx, Y + 1.5, gz - 24), v(gx, Y + 52, gz), v(gx, Y + 2.0, gz - 22), v(gx, Y + 45, gz), 330),
                seq(say(SHIRO, "grail.1"), say(SEMIRAMIS, "grail.2"))));
        s.add(Steps.fade(15, 10, 20, true));
        s.add(Steps.run(c -> StoryWorld.ensureGardens(c.trifas())));
        s.add(Steps.lightning(gx, Y, gz - 10));
        s.add(Steps.sound(SoundEvents.GENERIC_EXPLODE));

        // 2. Висячие сады парят в небе
        s.add(par(
                cam(v(gx, Y + 2.0, gz - 26), v(gx, Y + 40, gz), v(gx, Y + 4.0, gz - 20), v(gx, Y + 66, gz), 280),
                Steps.particles(ParticleTypes.CLOUD, gx, Y + 40, gz, 200, 12.0D)));

        // 3. на вершине: Широ и Семирамис
        s.add(par(
                seq(cam(v(gx - 3, top + 2.4, gz - 4), v(gx, top + 1.8, gz + 2.5), v(gx - 2, top + 2.2, gz - 1.5), v(gx, top + 1.8, gz + 2.5), 360),
                        cam(v(gx + 5, top + 2.4, gz - 2), v(gx + 2, top + 1.8, gz + 0.5), v(gx + 4, top + 2.2, gz - 1.5), v(gx + 2, top + 1.8, gz + 0.5), 360),
                        cam(v(gx, top + 3.0, gz - 5), v(gx + 1, top + 1.9, gz + 1.5), v(gx, top + 6.5, gz - 4.8), v(gx + 1, top + 2.2, gz + 1.5), 360)),
                seq(say(SHIRO, "grail.3"), say(SEMIRAMIS, "grail.4"), say(SHIRO, "grail.5"),
                        say(SEMIRAMIS, "grail.6"), say(SHIRO, "grail.7"))));
        interludeEnd(s, StoryChapter.RESISTANCE);
        return s;
    }

    // ================= Глава Зига: последнее сопротивление =================
    // Союзники собираются у разрушенной стены замка: Широ нужно остановить.
    private static List<Step> resistance() {
        final double cx = StoryWorld.STAGE_SIEGE_X + 0.5;
        List<Step> s = new ArrayList<>();
        s.add(Steps.fade(0, 20, 40, false));
        s.add(Steps.dismissServant());
        s.add(Steps.run(c -> StoryWorld.ensureStagesB(c.trifas())));
        s.add(Steps.teleport(cx, Y, 882.5, 0.0F));
        s.add(Steps.spawnNpc("jeanne", NpcType.JEANNE, cx - 2.5, Y, 892.5, 0.0F));
        s.add(Steps.spawnNpc("astolfo", NpcType.ASTOLFO, cx + 2.5, Y, 892.5, 0.0F));
        s.add(Steps.spawnNpc("mordred", NpcType.MORDRED, cx - 6.5, Y, 890.5, 45.0F));
        s.add(Steps.spawnNpc("kairi", NpcType.KAIRI, cx - 8.5, Y, 891.5, 45.0F));
        s.add(Steps.spawnNpc("caules", NpcType.CAULES, cx + 6.5, Y, 890.5, -45.0F));
        s.add(Steps.title("story.fateapocrypha.resistance.title", "story.fateapocrypha.resistance.subtitle"));
        s.add(Steps.wait(80));
        s.add(Steps.waitInZone(zone(cx - 4, 884, cx + 4, 892)));
        s.add(say(JEANNE, "res.1"));
        s.add(say(ASTOLFO, "res.2"));
        s.add(say(KAIRI, "res.3"));
        s.add(say(MORDRED, "res.4"));
        s.add(say(CAULES, "res.5"));
        s.add(say(JEANNE, "res.6"));
        s.add(say(SIEG, "res.7"));
        s.add(say(ASTOLFO, "res.8"));
        s.add(say(JEANNE, "res.9"));
        s.add(Steps.wait(30));
        s.add(Steps.title("story.fateapocrypha.to_be_continued.title", "story.fateapocrypha.to_be_continued.subtitle"));
        s.add(Steps.wait(100));
        s.add(Steps.fade(30, 10, 5, true));
        s.add(Steps.despawnAllNpcs());
        s.add(Steps.teleport(StoryWorld.FOREST_CX + 0.5, Y, StoryWorld.FOREST_CZ + 0.5, -90.0F));
        s.add(Steps.restoreServant(ServantType.ASTOLFO));
        s.add(Steps.advance(StoryChapter.TO_BE_CONTINUED));
        return s;
    }
}
