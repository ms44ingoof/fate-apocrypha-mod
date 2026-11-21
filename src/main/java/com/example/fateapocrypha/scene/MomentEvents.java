package com.example.fateapocrypha.scene;

import com.example.fateapocrypha.FateApocrypha;
import com.example.fateapocrypha.ModDimensions;
import com.example.fateapocrypha.story.StoryChapter;
import com.example.fateapocrypha.story.StoryLocation;
import com.example.fateapocrypha.story.StoryManager;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * «Моменты»: после эпилога у костра Красных, в часовне и на ферме можно сесть (Shift + ПКМ с пустой рукой) и
 * посмотреть небольшую сцену с героями. У каждого момента перерыв, чтобы не надоедало.
 */
@Mod.EventBusSubscriber(modid = FateApocrypha.MODID)
public final class MomentEvents {
    private static final int COOLDOWN_TICKS = 20 * 60 * 4;
    private static final String PERSISTED = "PlayerPersisted";

    private enum Moment {
        CAMPFIRE("campfire", StoryLocation.RED_CAMP, 7.0D),
        CHAPEL("chapel", StoryLocation.CHAPEL, 8.0D),
        FARM("farm", StoryLocation.FARM, 11.0D);

        final String id;
        final StoryLocation location;
        final double radius;

        Moment(String id, StoryLocation location, double radius) {
            this.id = id;
            this.location = location;
            this.radius = radius;
        }

        boolean near(ServerPlayer p) {
            return Math.hypot(p.getX() - location.cx(), p.getZ() - location.cz()) <= radius;
        }
    }

    private MomentEvents() {
    }

    private static boolean available(ServerPlayer p) {
        return p.level().dimension().equals(ModDimensions.TRIFAS)
                && StoryManager.getChapter(p).ordinal() >= StoryChapter.TO_BE_CONTINUED.ordinal()
                && !SceneManager.isRunning(p.getUUID());
    }

    private static CompoundTag store(ServerPlayer p) {
        CompoundTag data = p.getPersistentData();
        if (!data.contains(PERSISTED, 10)) {
            data.put(PERSISTED, new CompoundTag());
        }
        return data.getCompound(PERSISTED);
    }

    @SubscribeEvent
    public static void onRightClickBlock(PlayerInteractEvent.RightClickBlock event) {
        if (event.getLevel().isClientSide || event.getHand() != InteractionHand.MAIN_HAND
                || !(event.getEntity() instanceof ServerPlayer player)
                || !player.isShiftKeyDown() || !player.getMainHandItem().isEmpty() || !available(player)) {
            return;
        }
        for (Moment m : Moment.values()) {
            if (!m.near(player)) {
                continue;
            }
            event.setCanceled(true);
            long now = player.level().getGameTime();
            long next = store(player).getLong("moment_" + m.id);
            if (now < next) {
                player.displayClientMessage(Component.translatable("moment.fateapocrypha.cooldown"), true);
                return;
            }
            store(player).putLong("moment_" + m.id, now + COOLDOWN_TICKS);
            SceneManager.start(player, StoryScenes.moment(m.id));
            return;
        }
    }

    /** Подсказка рядом с местом момента. */
    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END || !(event.player instanceof ServerPlayer player)
                || player.tickCount % 60 != 0 || !available(player)) {
            return;
        }
        for (Moment m : Moment.values()) {
            if (m.near(player) && player.level().getGameTime() >= store(player).getLong("moment_" + m.id)) {
                player.displayClientMessage(Component.translatable("moment.fateapocrypha.hint." + m.id), true);
                return;
            }
        }
    }
}
