package bz.fxcore.fxserver;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.DoubleArgumentType;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;

public class FXServerCommand {

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("fxserver")
                .requires(source -> source.hasPermission(2))
                .then(Commands.literal("status")
                        .executes(context -> {
                            MinecraftServer server = context.getSource().getServer();
                            var msg = FXServerConfig.DATA.messages;

                            double mspt = server.getAverageTickTimeNanos() / 1_000_000.0;
                            double tps = Math.min(20.0, 1000.0 / Math.max(mspt, 50.0));
                            String formattedTps = String.format("%.1f", tps);
                            String formattedMspt = String.format("%.1f", mspt);

                            context.getSource().sendSuccess(() -> Component.literal(msg.statusHeader), false);
                            context.getSource().sendSuccess(() -> Component.literal(msg.statusTps
                                    .replace("{tps}", formattedTps)
                                    .replace("{mspt}", formattedMspt)), false);
                            context.getSource().sendSuccess(() -> Component.literal(msg.statusAuto
                                    .replace("{enabled}", FXServerConfig.DATA.autoOptimizationEnabled ? "§aATIVADO" : "§cDESATIVADO")), false);
                            context.getSource().sendSuccess(() -> Component.literal(msg.statusView
                                    .replace("{view}", String.valueOf(FXServerManager.getCurrentViewDistance()))
                                    .replace("{defView}", String.valueOf(FXServerConfig.DATA.defaultViewDistance))), false);
                            context.getSource().sendSuccess(() -> Component.literal(msg.statusTick
                                    .replace("{tick}", String.valueOf(FXServerManager.getCurrentSimulationDistance()))
                                    .replace("{defTick}", String.valueOf(FXServerConfig.DATA.defaultSimulationDistance))), false);
                            context.getSource().sendSuccess(() -> Component.literal(msg.statusMobCap
                                    .replace("{mobcap}", String.valueOf((int)(FXServerManager.getCurrentMobCapMultiplier() * 100)))), false);
                            return 1;
                        })
                )
                .then(Commands.literal("reload")
                        .executes(context -> {
                            FXServerConfig.carregar();
                            FXServerManager.initDefaults();
                            context.getSource().sendSuccess(() -> Component.literal(FXServerConfig.DATA.messages.reloadSuccess), true);
                            return 1;
                        })
                )
                .then(Commands.literal("forcechange")
                        .then(Commands.literal("view")
                                .then(Commands.argument("distance", IntegerArgumentType.integer(2, 32))
                                        .executes(context -> {
                                            int dist = IntegerArgumentType.getInteger(context, "distance");
                                            FXServerManager.setViewDistance(context.getSource().getServer(), dist);
                                            context.getSource().sendSuccess(() -> Component.literal(
                                                    FXServerConfig.DATA.messages.forceViewSuccess.replace("{view}", String.valueOf(dist))
                                            ), true);
                                            return 1;
                                        })
                                )
                        )
                        .then(Commands.literal("tick")
                                .then(Commands.argument("distance", IntegerArgumentType.integer(2, 32))
                                        .executes(context -> {
                                            int dist = IntegerArgumentType.getInteger(context, "distance");
                                            FXServerManager.setSimulationDistance(context.getSource().getServer(), dist);
                                            context.getSource().sendSuccess(() -> Component.literal(
                                                    FXServerConfig.DATA.messages.forceTickSuccess.replace("{tick}", String.valueOf(dist))
                                            ), true);
                                            return 1;
                                        })
                                )
                        )
                        .then(Commands.literal("mobcap")
                                .then(Commands.argument("porcentagem", DoubleArgumentType.doubleArg(0.0, 200.0))
                                        .executes(context -> {
                                            double pct = DoubleArgumentType.getDouble(context, "porcentagem");
                                            double multiplier = pct / 100.0;
                                            FXServerManager.setMobCapMultiplier(multiplier);
                                            context.getSource().sendSuccess(() -> Component.literal(
                                                    FXServerConfig.DATA.messages.forceMobCapSuccess.replace("{mobcap}", String.valueOf((int)pct))
                                            ), true);
                                            return 1;
                                        })
                                )
                        )
                )
        );
    }
}