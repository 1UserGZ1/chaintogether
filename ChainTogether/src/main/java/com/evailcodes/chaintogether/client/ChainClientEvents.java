package com.evailcodes.chaintogether.client;

import com.evailcodes.chaintogether.network.ChainPacketHandler;
import com.evailcodes.chaintogether.network.PullChainedPlayerPacket;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(value = Dist.CLIENT, modid = "chaintogether")
public class ChainClientEvents {
    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) {
            return;
        }

        Minecraft mc = Minecraft.getInstance();
        LocalPlayer player = mc.player;
        if (player == null || mc.level == null) {
            return;
        }

        while (ChainClient.PULL_CHAIN_KEY.consumeClick()) {
            Player target = ChainRenderer.getTargetedBoundPartner(mc);
            if (target != null) {
                ChainPacketHandler.sendToServer(new PullChainedPlayerPacket(target.getUUID()));
            }
        }
    }
}
