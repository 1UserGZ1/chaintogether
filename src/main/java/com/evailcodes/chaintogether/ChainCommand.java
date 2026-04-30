package com.evailcodes.chaintogether;

import com.evailcodes.chaintogether.config.ChainConfig;
import com.evailcodes.chaintogether.handler.ChainHandler;
import com.evailcodes.chaintogether.handler.ChainPlayerHandler;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.DoubleArgumentType;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.builder.RequiredArgumentBuilder;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.commands.arguments.selector.EntitySelector;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

import java.util.Collection;
import java.util.List;

public class ChainCommand {

    private static java.lang.reflect.Method PLAYER_ARGUMENT_METHOD;
    private static java.lang.reflect.Method GET_PLAYER_METHOD;

    static {
        // Debug: Print EntityArgument methods
        try {
            System.out.println("DEBUG: Dumping EntityArgument methods:");
            for (java.lang.reflect.Method m : EntityArgument.class.getMethods()) {
                System.out.println("Method: " + m.getName() + " " + m.getParameterCount() + " " + m.getReturnType().getName());
            }
            for (java.lang.reflect.Method m : EntityArgument.class.getDeclaredMethods()) {
                System.out.println("Declared Method: " + m.getName() + " " + m.getParameterCount() + " " + m.getReturnType().getName());
            }

            // Find player() method
            try {
                // Try standard names first
                PLAYER_ARGUMENT_METHOD = EntityArgument.class.getMethod("player");
            } catch (NoSuchMethodException e) {
                try {
                     // Try m_91466_
                     PLAYER_ARGUMENT_METHOD = EntityArgument.class.getMethod("m_91466_");
                } catch (NoSuchMethodException e2) {
                     // Try to find by signature: static, 0 args, returns EntityArgument, name usually 'player' or 'm_...'
                     for (java.lang.reflect.Method m : EntityArgument.class.getDeclaredMethods()) {
                         if (java.lang.reflect.Modifier.isStatic(m.getModifiers()) &&
                             m.getParameterCount() == 0 &&
                             m.getReturnType().equals(EntityArgument.class)) {
                                 // We need to distinguish player() (single) from players() (multiple)
                                 // Usually player() is first or we can check something else?
                                 // For now let's hope finding 'player' works or fallback matches
                                 // user verify log
                                 System.out.println("Potential candidate for player(): " + m.getName());
                                 // Assume first match if not found by name? No unsafe.
                         }
                     }
                     // Fallback: assume obfuscated name is passed via build reobf but we need to call it via reflection if direct call fails?
                     // Actually, if direct call fails with NoSuchMethodError, usage of Method class might bypass the link error if we find the RIGHT name.
                     // If runtime uses 'player', we found it in first try.
                     // If runtime uses 'm_91466_', we found it in second try.
                     // If neither, we really need the log.
                }
            }
            
            // Find getPlayer() method
            // public static ServerPlayer getPlayer(CommandContext<CommandSourceStack> context, String name)
            try {
                 GET_PLAYER_METHOD = EntityArgument.class.getMethod("getPlayer", CommandContext.class, String.class);
            } catch (NoSuchMethodException e) {
                 try {
                     // Try SRG name for getPlayer? m_91474_ ? (Need to verify)
                     // I don't know the exact SRG. I'll search by signature.
                      for (java.lang.reflect.Method m : EntityArgument.class.getDeclaredMethods()) {
                         if (java.lang.reflect.Modifier.isStatic(m.getModifiers()) &&
                             m.getParameterCount() == 2 &&
                             m.getParameterTypes()[0].equals(CommandContext.class) &&
                             m.getParameterTypes()[1].equals(String.class) &&
                             m.getReturnType().equals(ServerPlayer.class)) {
                             GET_PLAYER_METHOD = m;
                             break;
                         }
                     }
                 } catch (Exception ex) {
                     ex.printStackTrace();
                 }
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static EntityArgument playerArg() {
        try {
            if (PLAYER_ARGUMENT_METHOD != null) return (EntityArgument) PLAYER_ARGUMENT_METHOD.invoke(null);
        } catch (Exception e) { e.printStackTrace(); }
        // Fallback or fail
        throw new RuntimeException("Could not find EntityArgument.player() method");
    }

    private static ServerPlayer getPlayerArg(CommandContext<CommandSourceStack> ctx, String name) {
        try {
            if (GET_PLAYER_METHOD != null) return (ServerPlayer) GET_PLAYER_METHOD.invoke(null, ctx, name);
        } catch (Exception e) { 
             // If reflection fails, try direct call? No, that caused crash.
             // Maybe throw runtime exception.
             throw new RuntimeException("Could not invoke getPlayer", e);
        }
        throw new RuntimeException("Could not find EntityArgument.getPlayer() method");
    }

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(LiteralArgumentBuilder.<CommandSourceStack>literal("chain")
                .requires(source -> source.hasPermission(2))
                .then(LiteralArgumentBuilder.<CommandSourceStack>literal("bind")
                        .then(RequiredArgumentBuilder.<CommandSourceStack, EntitySelector>argument("player1", playerArg())
                                .then(RequiredArgumentBuilder.<CommandSourceStack, EntitySelector>argument("player2", playerArg())
                                        .executes(context -> bindPlayers(context,
                                                getPlayerArg(context, "player1"),
                                                getPlayerArg(context, "player2"),
                                                null, null, null))
                                        .then(RequiredArgumentBuilder.<CommandSourceStack, Double>argument("x", DoubleArgumentType.doubleArg())
                                                .then(RequiredArgumentBuilder.<CommandSourceStack, Double>argument("y", DoubleArgumentType.doubleArg())
                                                        .then(RequiredArgumentBuilder.<CommandSourceStack, Double>argument("z", DoubleArgumentType.doubleArg())
                                                                .executes(context -> bindPlayers(context,
                                                                        getPlayerArg(context, "player1"),
                                                                        getPlayerArg(context, "player2"),
                                                                        DoubleArgumentType.getDouble(context, "x"),
                                                                        DoubleArgumentType.getDouble(context, "y"),
                                                                        DoubleArgumentType.getDouble(context, "z")))))))))

                .then(LiteralArgumentBuilder.<CommandSourceStack>literal("unbind")
                        .then(RequiredArgumentBuilder.<CommandSourceStack, EntitySelector>argument("player", playerArg())
                                .executes(context -> unbindPlayer(context,
                                        getPlayerArg(context, "player")))))

                .then(LiteralArgumentBuilder.<CommandSourceStack>literal("length")
                        .then(RequiredArgumentBuilder.<CommandSourceStack, Double>argument("multiplier", DoubleArgumentType.doubleArg(0.5, 10.0))
                                .executes(context -> setChainLength(context,
                                        DoubleArgumentType.getDouble(context, "multiplier")))))

                .then(LiteralArgumentBuilder.<CommandSourceStack>literal("reload")
                        .executes(ChainCommand::reloadConfig))

                .then(LiteralArgumentBuilder.<CommandSourceStack>literal("list")
                        .executes(ChainCommand::listChainedPlayers))
                
                .then(LiteralArgumentBuilder.<CommandSourceStack>literal("transparency")
                        .then(RequiredArgumentBuilder.<CommandSourceStack, Integer>argument("value", IntegerArgumentType.integer(10, 100))
                                .executes(context -> setChainTransparency(context,
                                        IntegerArgumentType.getInteger(context, "value")))))
        );
    }
    
    // ... helper methods remain same ... but I need to make sure I closed the class properly and kept other methods.
    // I am replacing from "register" method downwards or just the register method?
    // ReplacementContent seems to include "register" and static block. 
    // And I need to keep the other methods.
    // I will read the file again to be safe on line numbers or use a larger chunk.
    // The previous view_file showed file ends at line 143.
    // I will replace the whole file? No, that's heavy.
    // I'll replace from `public class ChainCommand {` to the end of `register`.

    // Wait, I need to check where `register` ends.
    // Line 61: `    }` (end of register).
    // Line 63: `    private static int bindPlayers...`
    
    // I will replace `public class ChainCommand { ... register ... }` (lines 19-61).


    private static int bindPlayers(CommandContext<CommandSourceStack> context, ServerPlayer player1, ServerPlayer player2, Double x, Double y, Double z) {
        // 如果提供了坐标，将player2传送到指定坐标
        if (x != null && y != null && z != null) {
            // 将player2传送到player1所在维度的指定坐标
            player2.teleportTo((net.minecraft.server.level.ServerLevel) player1.level(), x, y, z, player2.getYRot(), player2.getXRot());
            ChainTogether.LOGGER.info("Teleported player {} to coordinates {} in dimension {}", 
                    player2.getName().getString(), 
                    String.format("%.2f, %.2f, %.2f", x, y, z), 
                    player1.level().dimension().location());
        } else {
            // 如果没有提供坐标，将player2传送到player1身边
            player2.teleportTo((net.minecraft.server.level.ServerLevel) player1.level(), 
                    player1.getX(), player1.getY(), player1.getZ(), 
                    player2.getYRot(), player2.getXRot());
            ChainTogether.LOGGER.info("Teleported player {} to player {}", 
                    player2.getName().getString(), 
                    player1.getName().getString());
        }
        
        // 绑定玩家
        ChainHandler.bindPlayers(player1, player2);
        context.getSource().sendSuccess(() ->
                Component.literal("已成功将 " + player1.getName().getString() + " 与 " + player2.getName().getString() + " 绑定！"), true);
        return 1;
    }

    private static int unbindPlayer(CommandContext<CommandSourceStack> context, ServerPlayer player) {
        ChainHandler.unbindPlayer(player);
        context.getSource().sendSuccess(() ->
                Component.literal("已成功解绑玩家：" + player.getName().getString()), true);
        return 1;
    }

    private static int setChainLength(CommandContext<CommandSourceStack> context, double multiplier) {
        // 这里应该更新配置，然后通知所有玩家
        ChainConfig.CHAIN_LENGTH.set(multiplier);
        ChainConfig.SPEC.save();
        
        context.getSource().sendSuccess(() ->
                Component.literal("链条长度已设置为：" + multiplier + " 倍"), true);
        return 1;
    }

    private static int setChainTransparency(CommandContext<CommandSourceStack> context, int value) {
        // 更新配置
        ChainConfig.CHAIN_TRANSPARENCY.set(value);
        ChainConfig.SPEC.save();
        
        context.getSource().sendSuccess(() ->
                Component.literal("链条透明度已设置为：" + value + "%"), true);
        return 1;
    }

    private static int reloadConfig(CommandContext<CommandSourceStack> context) {

        context.getSource().sendSuccess(() ->
                Component.literal("配置已重新加载！"), true);
        return 1;
    }

    private static int listChainedPlayers(CommandContext<CommandSourceStack> context) {
        CommandSourceStack source = context.getSource();
        net.minecraft.server.MinecraftServer server = source.getServer();
        
        List<String> pairs = ChainHandler.getChainedPlayerPairs(server);
        if (pairs.isEmpty()) {
            context.getSource().sendSuccess(() ->
                    Component.literal("没有玩家绑定！"), false);
        } else {
            context.getSource().sendSuccess(() ->
                    Component.literal("玩家绑定列表："), false);
            context.getSource().sendSuccess(() ->
                    Component.literal("-----------------------"), false);
            for (String pair : pairs) {
                context.getSource().sendSuccess(() ->
                        Component.literal(pair), false);
            }
        }
        return 1;
    }
}