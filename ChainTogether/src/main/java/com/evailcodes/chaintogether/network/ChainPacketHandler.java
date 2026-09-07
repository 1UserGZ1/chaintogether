package com.evailcodes.chaintogether.network;

import com.evailcodes.chaintogether.ChainTogether;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.simple.SimpleChannel;

public class ChainPacketHandler {
    private static final String PROTOCOL_VERSION = "1";
    private static final SimpleChannel INSTANCE = NetworkRegistry.newSimpleChannel(
            new ResourceLocation(ChainTogether.MODID, "main"),
            () -> PROTOCOL_VERSION,
            PROTOCOL_VERSION::equals,
            PROTOCOL_VERSION::equals
    );
    private static int packetId = 0;

    public static void register() {
        INSTANCE.registerMessage(packetId++, UpdateChainLengthPacket.class,
                UpdateChainLengthPacket::encode,
                UpdateChainLengthPacket::decode,
                UpdateChainLengthPacket::handle);

        INSTANCE.registerMessage(packetId++, SyncBoundStatusPacket.class,
                SyncBoundStatusPacket::encode,
                SyncBoundStatusPacket::decode,
                SyncBoundStatusPacket::handle);

        INSTANCE.registerMessage(packetId++, PullChainedPlayerPacket.class,
                PullChainedPlayerPacket::encode,
                PullChainedPlayerPacket::decode,
                PullChainedPlayerPacket::handle);
    }

    public static <MSG> void sendToServer(MSG message) {
        INSTANCE.sendToServer(message);
    }

    public static <MSG> void sendToPlayer(MSG message, ServerPlayer player) {
        INSTANCE.send(PacketDistributor.PLAYER.with(() -> player), message);
    }

    public static <MSG> void sendToAll(MSG message) {
        INSTANCE.send(PacketDistributor.ALL.noArg(), message);
    }
}
