package com.example.fateapocrypha.anim;

import com.example.fateapocrypha.entity.ServantEntity;
import com.example.fateapocrypha.network.ModNetwork;
import com.example.fateapocrypha.network.PlayerAnimPacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.network.PacketDistributor;

/**
 * Единая шина действий, которая связывает обе библиотеки анимаций. Одно и то же действие (взмах, каст, благословение)
 * запускается сразу на двух сторонах: у Мастера проигрывается клип Player Animation Library, а у его Слуги имя действия
 * уходит в синхронизируемое поле ServantEntity.playAnim, по которому GeckoLib-рендерер выбирает позу героя. Так
 * анимации Мастера и Слуги всегда согласованы по времени и смыслу.
 */
public final class FateAnimBus {
    private FateAnimBus() {
    }

    /** Клип PAL для игрока (виден всем, кто его видит, и самому игроку). */
    public static void player(Player player, String clip) {
        if (player instanceof ServerPlayer sp) {
            ModNetwork.CHANNEL.send(PacketDistributor.TRACKING_ENTITY_AND_SELF.with(() -> sp),
                    new PlayerAnimPacket(sp.getUUID(), clip));
        }
    }

    /** Действие Слуги (имя жеста FateModel: cast, bow, wave, pray...). */
    public static void servant(ServantEntity servant, String action, int ticks) {
        if (servant != null) {
            servant.playAnim(action, ticks);
        }
    }

    /** Согласованное действие: Мастер играет клип PAL, Слуга своё действие. */
    public static void together(Player master, String masterClip, ServantEntity servant, String servantAction, int ticks) {
        player(master, masterClip);
        servant(servant, servantAction, ticks);
    }
}
