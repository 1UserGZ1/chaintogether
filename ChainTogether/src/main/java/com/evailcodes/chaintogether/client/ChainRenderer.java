package com.evailcodes.chaintogether.client;

import com.evailcodes.chaintogether.config.ChainConfig;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.block.BlockRenderDispatcher;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.ChainBlock;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderLevelStageEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.joml.Quaternionf;
import org.joml.Vector3f;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Mod.EventBusSubscriber(value = Dist.CLIENT, modid = "chaintogether")
public class ChainRenderer {
    private static final Map<UUID, UUID> CLIENT_BOUND_PLAYERS = new HashMap<>();
    private static final net.minecraft.world.level.block.state.BlockState CHAIN_STATE =
            Blocks.CHAIN.defaultBlockState().setValue(ChainBlock.AXIS, Direction.Axis.Y);

    @SubscribeEvent
    public static void onRenderLevel(RenderLevelStageEvent event) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_PARTICLES) {
            return;
        }

        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.level == null) {
            return;
        }

        renderChains(event.getPoseStack(), event.getPartialTick());
    }

    public static void syncBoundStatus(UUID player1, UUID player2, boolean bound) {
        if (bound) {
            CLIENT_BOUND_PLAYERS.put(player1, player2);
            CLIENT_BOUND_PLAYERS.put(player2, player1);
        } else {
            CLIENT_BOUND_PLAYERS.remove(player1);
            CLIENT_BOUND_PLAYERS.remove(player2);
        }
    }

    public static Player getTargetedBoundPartner(Minecraft mc) {
        LocalPlayer localPlayer = mc.player;
        if (localPlayer == null || mc.level == null) {
            return null;
        }

        UUID partnerId = CLIENT_BOUND_PLAYERS.get(localPlayer.getUUID());
        if (partnerId == null) {
            return null;
        }

        Player partner = mc.level.getPlayerByUUID(partnerId);
        if (partner == null) {
            return null;
        }

        return isLookingAtChain(mc, localPlayer, partner, mc.getFrameTime()) ? partner : null;
    }

    private static void renderChains(PoseStack poseStack, float partialTicks) {
        Minecraft mc = Minecraft.getInstance();
        Player localPlayer = mc.player;
        if (localPlayer == null || mc.level == null) {
            return;
        }

        Vec3 cameraPos = mc.gameRenderer.getMainCamera().getPosition();
        MultiBufferSource.BufferSource bufferSource = mc.renderBuffers().bufferSource();
        BlockRenderDispatcher blockRenderer = mc.getBlockRenderer();
        float alpha = ChainConfig.getTransparencyAsFloat();

        RenderSystem.setShaderColor(1.0f, 1.0f, 1.0f, alpha);
        try {
            for (Player otherPlayer : mc.level.players()) {
                if (otherPlayer == localPlayer) {
                    continue;
                }
                if (!isPlayersBound(localPlayer, otherPlayer)) {
                    continue;
                }

                renderChainBetweenPlayers(
                        poseStack,
                        bufferSource,
                        blockRenderer,
                        cameraPos,
                        localPlayer,
                        otherPlayer,
                        partialTicks
                );
            }
            bufferSource.endBatch();
        } finally {
            RenderSystem.setShaderColor(1.0f, 1.0f, 1.0f, 1.0f);
        }
    }

    private static boolean isPlayersBound(Player player1, Player player2) {
        return CLIENT_BOUND_PLAYERS.containsKey(player1.getUUID())
                && CLIENT_BOUND_PLAYERS.get(player1.getUUID()).equals(player2.getUUID());
    }

    private static boolean isLookingAtChain(Minecraft mc, Player player1, Player player2, float partialTicks) {
        Vec3 cameraPos = mc.gameRenderer.getMainCamera().getPosition();
        Vector3f lookVector = mc.gameRenderer.getMainCamera().getLookVector();
        Vec3 rayDir = new Vec3(lookVector.x, lookVector.y, lookVector.z).normalize();

        Vec3 start = chainAnchor(player1, partialTicks);
        Vec3 end = chainAnchor(player2, partialTicks);
        return isRayCloseToSegment(cameraPos, rayDir, start, end, 0.45D);
    }

    private static void renderChainBetweenPlayers(
            PoseStack poseStack,
            MultiBufferSource bufferSource,
            BlockRenderDispatcher blockRenderer,
            Vec3 cameraPos,
            Player player1,
            Player player2,
            float partialTicks
    ) {
        Vec3 start = player1.getPosition(partialTicks).add(0.0, 1.0, 0.0);
        Vec3 end = player2.getPosition(partialTicks).add(0.0, 1.0, 0.0);
        Vec3 delta = end.subtract(start);
        double distance = delta.length();

        double maxDistance = ChainConfig.CHAIN_LENGTH.get() * 10.0;
        if (distance <= 0.01 || distance > maxDistance * 2.0) {
            return;
        }

        Vec3 direction = delta.normalize();
        int segmentCount = Math.max(1, (int) Math.ceil(distance / 0.42));

        for (int i = 0; i < segmentCount; i++) {
            float progress = (i + 0.5f) / segmentCount;
            Vec3 segmentPos = start.lerp(end, progress);
            renderChainSegment(poseStack, blockRenderer, bufferSource, cameraPos, segmentPos, direction, i);
        }
    }

    private static void renderChainSegment(
            PoseStack poseStack,
            BlockRenderDispatcher blockRenderer,
            MultiBufferSource bufferSource,
            Vec3 cameraPos,
            Vec3 worldPos,
            Vec3 direction,
            int segmentIndex
    ) {
        poseStack.pushPose();
        try {
            poseStack.translate(
                    worldPos.x - cameraPos.x,
                    worldPos.y - cameraPos.y,
                    worldPos.z - cameraPos.z
            );
            poseStack.translate(-0.5, -0.5, -0.5);

            Quaternionf rotation = new Quaternionf().rotationTo(
                    new Vector3f(0.0f, 1.0f, 0.0f),
                    new Vector3f((float) direction.x, (float) direction.y, (float) direction.z)
            );
            if ((segmentIndex & 1) == 1) {
                rotation.rotateY((float) (Math.PI / 2.0));
            }
            poseStack.mulPose(rotation);
            poseStack.scale(0.48f, 0.48f, 0.48f);

            int light = LevelRenderer.getLightColor(Minecraft.getInstance().level, BlockPos.containing(worldPos));
            blockRenderer.renderSingleBlock(CHAIN_STATE, poseStack, bufferSource, light, OverlayTexture.NO_OVERLAY);
        } finally {
            poseStack.popPose();
        }
    }

    private static Vec3 chainAnchor(Player player, float partialTicks) {
        return player.getPosition(partialTicks).add(0.0, 1.0, 0.0);
    }

    private static boolean isRayCloseToSegment(Vec3 rayOrigin, Vec3 rayDir, Vec3 segmentStart, Vec3 segmentEnd, double threshold) {
        Vec3 segment = segmentEnd.subtract(segmentStart);
        Vec3 w0 = rayOrigin.subtract(segmentStart);

        double b = rayDir.dot(segment);
        double c = segment.lengthSqr();
        double d = rayDir.dot(w0);
        double e = segment.dot(w0);
        double denom = c - b * b;
        double thresholdSq = threshold * threshold;

        if (denom > 1.0E-6D) {
            double rayT = (b * e - c * d) / denom;
            if (rayT < 0.0D) {
                return false;
            }

            double segT = (e - b * d) / denom;
            segT = clamp(segT, 0.0D, 1.0D);

            Vec3 segmentPoint = segmentStart.add(segment.scale(segT));
            double projectedRayT = Math.max(0.0D, segmentPoint.subtract(rayOrigin).dot(rayDir));
            Vec3 rayPoint = rayOrigin.add(rayDir.scale(projectedRayT));
            return rayPoint.distanceToSqr(segmentPoint) <= thresholdSq;
        }

        return distanceToRaySquared(rayOrigin, rayDir, segmentStart) <= thresholdSq
                || distanceToRaySquared(rayOrigin, rayDir, segmentEnd) <= thresholdSq
                || distanceToRaySquared(rayOrigin, rayDir, segmentStart.add(segment.scale(0.5D))) <= thresholdSq;
    }

    private static double distanceToRaySquared(Vec3 rayOrigin, Vec3 rayDir, Vec3 point) {
        Vec3 delta = point.subtract(rayOrigin);
        double t = delta.dot(rayDir);
        if (t <= 0.0D) {
            return delta.lengthSqr();
        }
        Vec3 closest = rayOrigin.add(rayDir.scale(t));
        return closest.distanceToSqr(point);
    }

    private static double clamp(double value, double min, double max) {
        return Math.max(min, Math.min(max, value));
    }
}
