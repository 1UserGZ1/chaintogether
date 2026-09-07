package com.evailcodes.chaintogether.network;

import com.evailcodes.chaintogether.handler.ChainHandler;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.network.NetworkEvent;

import java.util.UUID;
import java.util.function.Supplier;

public class PullChainedPlayerPacket {
    private final UUID targetPlayer;

    public PullChainedPlayerPacket(UUID targetPlayer) {
        this.targetPlayer = targetPlayer;
    }

    public static void encode(PullChainedPlayerPacket packet, FriendlyByteBuf buf) {
        buf.writeUUID(packet.targetPlayer);
    }

    public static PullChainedPlayerPacket decode(FriendlyByteBuf buf) {
        return new PullChainedPlayerPacket(buf.readUUID());
    }

    public static void handle(PullChainedPlayerPacket packet, Supplier<NetworkEvent.Context> context) {
        NetworkEvent.Context ctx = context.get();
        ServerPlayer sender = ctx.getSender();
        ctx.enqueueWork(() -> {
            if (sender == null) {
                return;
            }

            ServerPlayer partner = ChainHandler.getPartner(sender);
            if (partner == null || !partner.getUUID().equals(packet.targetPlayer)) {
                return;
            }
            if (sender.level() != partner.level()) {
                return;
            }

            Vec3 senderAnchor = sender.position().add(0.0, 1.0, 0.0);
            Vec3 partnerAnchor = partner.position().add(0.0, 1.0, 0.0);
            Vec3 pull = senderAnchor.subtract(partnerAnchor);
            double distance = pull.length();
            if (distance < 0.75D) {
                return;
            }

            double strength = Math.min(0.9D, distance * 0.22D);
            Vec3 velocity = pull.normalize().scale(strength);
            partner.setDeltaMovement(partner.getDeltaMovement().add(velocity));
            partner.hurtMarked = true;
        });
        ctx.setPacketHandled(true);
    }

    public UUID getTargetPlayer() {
        return targetPlayer;
    }
}
