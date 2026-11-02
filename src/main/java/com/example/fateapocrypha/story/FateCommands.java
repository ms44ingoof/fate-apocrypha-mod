package com.example.fateapocrypha.story;

import com.example.fateapocrypha.ModDimensions;
import com.example.fateapocrypha.ModItems;
import com.example.fateapocrypha.servant.ServantActions;
import net.minecraft.world.item.ItemStack;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.levelgen.Heightmap;

public class FateCommands {
    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("fate")
                .then(Commands.literal("story")
                        .executes(ctx -> showStory(ctx.getSource())))
                .then(Commands.literal("kit")
                        .executes(ctx -> kit(ctx.getSource())))
                .then(Commands.literal("trifas")
                        .executes(ctx -> travel(ctx.getSource())))
                .then(Commands.literal("chapter")
                        .requires(source -> source.hasPermission(2))
                        .then(Commands.argument("id", IntegerArgumentType.integer(0, StoryChapter.values().length - 1))
                                .executes(ctx -> setChapter(ctx.getSource(),
                                        IntegerArgumentType.getInteger(ctx, "id"))))));
    }

    /** Возвращает потерянное Командное заклинание; операторам ещё и катализатор. */
    private static int kit(CommandSourceStack source) throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        ServerPlayer player = source.getPlayerOrException();
        if (!ServantActions.hasItem(player, ModItems.COMMAND_SEAL.get())) {
            ServantActions.give(player, new ItemStack(ModItems.COMMAND_SEAL.get()));
        }
        if (source.hasPermission(2) && !ServantActions.hasItem(player, ModItems.SUMMON_CATALYST.get())) {
            ServantActions.give(player, new ItemStack(ModItems.SUMMON_CATALYST.get()));
        }
        return 1;
    }

    private static int showStory(CommandSourceStack source) throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        ServerPlayer player = source.getPlayerOrException();
        StoryManager.showObjective(player);
        return 1;
    }

    private static int setChapter(CommandSourceStack source, int id) throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        ServerPlayer player = source.getPlayerOrException();
        StoryManager.setChapter(player, StoryChapter.byId(id));
        StoryManager.showObjective(player);
        return 1;
    }

    /** Телепорт Overworld <-> Трифас. Доступен с главы "Дорога в Румынию" (или операторам). */
    private static int travel(CommandSourceStack source) throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        ServerPlayer player = source.getPlayerOrException();
        boolean allowed = source.hasPermission(2)
                || StoryManager.getChapter(player).ordinal() >= StoryChapter.ROMANIA.ordinal();
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
            // Поверхность плоского мира: y = -59
            player.teleportTo(trifas, 0.5D, -58.0D, 0.5D, player.getYRot(), player.getXRot());
        }
        return 1;
    }
}
