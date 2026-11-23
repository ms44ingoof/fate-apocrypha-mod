package com.example.fateapocrypha.client.pal;

import com.example.fateapocrypha.FateApocrypha;
import dev.kosmx.playerAnim.api.layered.IAnimation;
import dev.kosmx.playerAnim.api.layered.KeyframeAnimationPlayer;
import dev.kosmx.playerAnim.api.layered.ModifierLayer;
import dev.kosmx.playerAnim.core.data.KeyframeAnimation;
import dev.kosmx.playerAnim.minecraftApi.PlayerAnimationAccess;
import dev.kosmx.playerAnim.minecraftApi.PlayerAnimationFactory;
import dev.kosmx.playerAnim.minecraftApi.PlayerAnimationRegistry;
import java.util.UUID;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;

/**
 * Связь с Player Animation Library: свой слой анимаций игрока и воспроизведение клипов из
 * assets/fateapocrypha/player_animation/*.json (взмах, выпад, луч, благословение, печать).
 */
public final class PalBridge {
    private static final ResourceLocation LAYER = new ResourceLocation(FateApocrypha.MODID, "fate");

    private PalBridge() {
    }

    /** Регистрирует слой на каждом игроке (вызывается при настройке клиента). */
    public static void register() {
        PlayerAnimationFactory.ANIMATION_DATA_FACTORY.registerFactory(LAYER, 42, player -> new ModifierLayer<>());
    }

    /** Проигрывает клип игроку; если клип не найден или игрока нет, ничего не происходит. */
    @SuppressWarnings("unchecked")
    public static void play(UUID id, String name) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null) {
            return;
        }
        Player p = mc.level.getPlayerByUUID(id);
        if (!(p instanceof AbstractClientPlayer clientPlayer)) {
            return;
        }
        ModifierLayer<IAnimation> layer = (ModifierLayer<IAnimation>) PlayerAnimationAccess.getPlayerAssociatedData(clientPlayer).get(LAYER);
        KeyframeAnimation anim = PlayerAnimationRegistry.getAnimation(new ResourceLocation(FateApocrypha.MODID, name));
        if (layer != null && anim != null) {
            layer.setAnimation(new KeyframeAnimationPlayer(anim));
        }
    }
}
