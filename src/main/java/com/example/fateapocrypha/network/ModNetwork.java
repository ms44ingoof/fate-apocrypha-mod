package com.example.fateapocrypha.network;

import com.example.fateapocrypha.FateApocrypha;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.simple.SimpleChannel;

public class ModNetwork {
    private static final String PROTOCOL = "1";

    public static final SimpleChannel CHANNEL = NetworkRegistry.newSimpleChannel(
            new ResourceLocation(FateApocrypha.MODID, "main"),
            () -> PROTOCOL, PROTOCOL::equals, PROTOCOL::equals);

    public static void register() {
        int id = 0;
        CHANNEL.registerMessage(id++, SealActionPacket.class,
                SealActionPacket::encode, SealActionPacket::decode, SealActionPacket::handle);
        CHANNEL.registerMessage(id++, SummonPacket.class,
                SummonPacket::encode, SummonPacket::decode, SummonPacket::handle);
        CHANNEL.registerMessage(id++, DialoguePacket.class,
                DialoguePacket::encode, DialoguePacket::decode, DialoguePacket::handle);
        CHANNEL.registerMessage(id++, FadePacket.class,
                FadePacket::encode, FadePacket::decode, FadePacket::handle);
        CHANNEL.registerMessage(id++, CameraPacket.class,
                CameraPacket::encode, CameraPacket::decode, CameraPacket::handle);
        CHANNEL.registerMessage(id++, SkipDialoguePacket.class,
                SkipDialoguePacket::encode, SkipDialoguePacket::decode, SkipDialoguePacket::handle);
    }
}
