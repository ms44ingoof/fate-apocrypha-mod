package com.example.fateapocrypha.story;

import com.example.fateapocrypha.ModDimensions;
import com.example.fateapocrypha.scene.SceneManager;
import com.example.fateapocrypha.scene.Step;
import com.example.fateapocrypha.scene.StoryDetails;
import com.example.fateapocrypha.scene.StoryScenes;
import com.example.fateapocrypha.scene.StoryWorld;
import java.util.List;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;

/** Запускает сцену текущей главы игрока. */
public class StoryDirector {
    public static void startScene(ServerPlayer player) {
        startScene(player, false);
    }

    /**
     * force=false: если глава начинается в локации, а игрок ещё не в ней, сцена не стартует:
     * игрок идёт к локации, а вход в неё (LocationGuard) запускает сцену.
     * force=true: сцена запускается сразу (возрождение, вход в игру, команды).
     */
    public static void startScene(ServerPlayer player, boolean force) {
        StoryChapter chapter = StoryManager.getChapter(player);
        List<Step> steps = StoryScenes.build(chapter);
        if (steps.isEmpty()) {
            SceneManager.stop(player.getUUID());
            return;
        }
        StoryLocation location = StoryLocation.forChapter(chapter);
        if (!force && location != null && !location.contains(player.getX(), player.getZ())) {
            SceneManager.stop(player.getUUID());
            StoryManager.setStarted(player, false);
            return;
        }
        ServerLevel trifas = player.getServer() == null ? null : player.getServer().getLevel(ModDimensions.TRIFAS);
        if (trifas == null) {
            player.sendSystemMessage(Component.translatable("command.fateapocrypha.no_dimension"));
            return;
        }
        StoryWorld.ensureBuilt(trifas);
        StoryDetails.ensure(trifas);
        StoryManager.setStarted(player, true);
        SceneManager.start(player, steps);
    }

    /** Вход в мир или возрождение: продолжаем сюжет. */
    public static void resume(ServerPlayer player) {
        // старые сохранения: печати проявляются у всех, кто уже прошёл сцену с Селеникой
        if (!StoryManager.hasSeals(player)
                && StoryManager.getChapter(player).ordinal() > StoryChapter.CELENIKE.ordinal()) {
            StoryManager.setSeals(player, true);
        }
        QuestSync.send(player);
        StoryChapter chapter = StoryManager.getChapter(player);
        if (chapter.ordinal() >= StoryChapter.TO_BE_CONTINUED.ordinal()) {
            StoryManager.showObjective(player);
            return;
        }
        StoryLocation location = StoryLocation.forChapter(chapter);
        if (location == null || StoryManager.isStarted(player)) {
            startScene(player, true);   // сцена была прервана (смерть, выход из игры): начинаем главу сначала
            return;
        }
        // игрок идёт к локации: возвращаем его в мир Трифаса и напоминаем цель
        ServerLevel trifas = player.getServer() == null ? null : player.getServer().getLevel(ModDimensions.TRIFAS);
        if (trifas != null && !player.level().dimension().equals(ModDimensions.TRIFAS)) {
            location.teleportNear(player, trifas);
        }
        StoryManager.showObjective(player);
    }
}
