package com.example.fateapocrypha.story;

import com.example.fateapocrypha.ModDimensions;
import com.example.fateapocrypha.ModItems;
import com.example.fateapocrypha.scene.Reloc;
import com.example.fateapocrypha.scene.SceneManager;
import com.example.fateapocrypha.scene.StoryWorld;
import com.example.fateapocrypha.servant.ServantActions;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.levelgen.Heightmap;

public class FateCommands {
    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("fate")
                .then(Commands.literal("story").executes(ctx -> showStory(ctx.getSource())))
                .then(Commands.literal("kit").executes(ctx -> kit(ctx.getSource())))
                .then(Commands.literal("trifas").executes(ctx -> travel(ctx.getSource())))
                .then(Commands.literal("go").executes(ctx -> go(ctx.getSource())))
                .then(Commands.literal("restart")
                        .requires(source -> source.hasPermission(2))
                        .executes(ctx -> restart(ctx.getSource())))
                .then(Commands.literal("build")
                        .requires(source -> source.hasPermission(2))
                        .executes(ctx -> build(ctx.getSource())))
                .then(Commands.literal("chapter")
                        .requires(source -> source.hasPermission(2))
                        .then(Commands.argument("id", IntegerArgumentType.integer(0, StoryChapter.values().length - 1))
                                .executes(ctx -> setChapter(ctx.getSource(),
                                        IntegerArgumentType.getInteger(ctx, "id"))))));
    }

    private static int showStory(CommandSourceStack source) throws CommandSyntaxException {
        StoryManager.showObjective(source.getPlayerOrException());
        return 1;
    }

    /** Возвращает потерянное Командное заклинание; операторам ещё и катализатор. */
    private static int kit(CommandSourceStack source) throws CommandSyntaxException {
        ServerPlayer player = source.getPlayerOrException();
        if (StoryManager.getChapter(player).ordinal() < StoryChapter.TO_BE_CONTINUED.ordinal() && !source.hasPermission(2)) {
            source.sendFailure(Component.translatable("command.fateapocrypha.locked").withStyle(ChatFormatting.RED));
            return 0;
        }
        StoryManager.setSeals(player, true);
        if (source.hasPermission(2) && !ServantActions.hasItem(player, ModItems.SUMMON_CATALYST.get())) {
            ServantActions.give(player, new ItemStack(ModItems.SUMMON_CATALYST.get()));
        }
        return 1;
    }

    private static int setChapter(CommandSourceStack source, int id) throws CommandSyntaxException {
        ServerPlayer player = source.getPlayerOrException();
        StoryManager.setChapter(player, StoryChapter.byId(id));
        StoryManager.showObjective(player);
        StoryDirector.startScene(player, true);
        return 1;
    }

    private static int restart(CommandSourceStack source) throws CommandSyntaxException {
        StoryDirector.startScene(source.getPlayerOrException(), true);
        return 1;
    }

    /** Быстрое перемещение к границе локации текущей цели сюжета. */
    private static int go(CommandSourceStack source) throws CommandSyntaxException {
        ServerPlayer player = source.getPlayerOrException();
        StoryLocation location = StoryLocation.forChapter(StoryManager.getChapter(player));
        ServerLevel trifas = source.getServer().getLevel(ModDimensions.TRIFAS);
        if (location == null || trifas == null || SceneManager.isRunning(player.getUUID())) {
            source.sendFailure(Component.translatable("command.fateapocrypha.go_none"));
            return 0;
        }
        location.teleportNear(player, trifas);
        return 1;
    }

    private static int build(CommandSourceStack source) {
        ServerLevel trifas = source.getServer().getLevel(ModDimensions.TRIFAS);
        if (trifas == null) {
            source.sendFailure(Component.translatable("command.fateapocrypha.no_dimension"));
            return 0;
        }
        StoryWorld.buildAll(trifas);
        source.sendSuccess(() -> Component.translatable("command.fateapocrypha.built"), true);
        return 1;
    }

    /** Телепорт Overworld <-> Трифас. Доступен операторам и после текущего сюжета. */
    private static int travel(CommandSourceStack source) throws CommandSyntaxException {
        ServerPlayer player = source.getPlayerOrException();
        boolean allowed = source.hasPermission(2)
                || StoryManager.getChapter(player).ordinal() >= StoryChapter.TO_BE_CONTINUED.ordinal();
        if (!allowed) {
            source.sendFailure(Component.translatable("command.fateapocrypha.locked").withStyle(ChatFormatting.RED));
            return 0;
        }

        ServerLevel trifas = source.getServer().getLevel(ModDimensions.TRIFAS);
        if (trifas == null) {
            source.sendFailure(Component.translatable("command.fateapocrypha.no_dimension"));
            return 0;
        }

        if (player.level().dimension().equals(ModDimensions.TRIFAS)) {
            ServerLevel overworld = source.getServer().overworld();
            BlockPos spawn = overworld.getHeightmapPos(Heightmap.Types.MOTION_BLOCKING, overworld.getSharedSpawnPos());
            player.teleportTo(overworld, spawn.getX() + 0.5D, spawn.getY(), spawn.getZ() + 0.5D,
                    player.getYRot(), player.getXRot());
        } else {
            int tx = (int) Reloc.x(StoryWorld.FOREST_CX, StoryWorld.FOREST_CZ);
            int tz = (int) Reloc.z(StoryWorld.FOREST_CX, StoryWorld.FOREST_CZ);
            int ty = trifas.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, tx, tz);
            player.teleportTo(trifas, tx + 0.5D, ty, tz + 0.5D, player.getYRot(), player.getXRot());
        }
        return 1;
    }
}
