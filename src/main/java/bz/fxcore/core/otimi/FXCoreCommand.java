package bz.fxcore.core.otimi;

import bz.fxcore.modules.clear.FXClearConfig;
import bz.fxcore.modules.build.FXBuildConfig;
import bz.fxcore.core.database.PlayerDataManager;
import bz.fxcore.core.otimi.chunk.FXChunkAnalyzer;
import bz.fxcore.core.otimi.chunk.FXChunkBan;
import bz.fxcore.core.otimi.chunk.FXLagView;
import bz.fxcore.core.otimi.chunk.FXTestChunkBan;
import bz.fxcore.core.otimi.server.FXServerConfig;
import bz.fxcore.core.otimi.server.FXServerManager;
import bz.fxcore.core.staff.FXJustStaff;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.BoolArgumentType;
import com.mojang.brigadier.arguments.DoubleArgumentType;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.DimensionArgument;
import net.minecraft.commands.arguments.coordinates.Vec2Argument;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.phys.Vec2;

public class FXCoreCommand {

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("fxcore")
            .requires(source -> source.hasPermission(2))

            // Subcomando /fxcore server
            .then(Commands.literal("server")
                // /fxcore server status
                .then(Commands.literal("status")
                    .executes(context -> {
                        MinecraftServer server = context.getSource().getServer();
                        var msg = FXServerConfig.DATA.messages;

                        double mspt = server.getAverageTickTimeNanos() / 1_000_000.0;
                        double tps = Math.min(20.0, 1000.0 / Math.max(mspt, 50.0));
                        String formattedTps = String.format("%.1f", tps);
                        String formattedMspt = String.format("%.1f", mspt);

                        int loadedChunks = FXServerManager.getTotalLoadedChunks(server);
                        int loadedEntities = FXServerManager.getTotalLoadedEntities(server);
                        int onlinePlayers = server.getPlayerCount();
                        
                        int recentWarns = FXCoreWarnings.getRecentWarningsCount();
                        String warnStatus = FXServerConfig.DATA.warningsEnabled ? "§aON" : "§cOFF";

                        boolean isLagging = FXServerManager.isOptimized(server);
                        String coreStatus = isLagging ? "§cOTIMIZADO" : "§aNORMAL";

                        context.getSource().sendSuccess(() -> Component.literal(msg.statusHeader), false);
                        context.getSource().sendSuccess(() -> Component.literal(msg.statusTps
                                .replace("{tps}", formattedTps)
                                .replace("{mspt}", formattedMspt)), false);
                        context.getSource().sendSuccess(() -> Component.literal(msg.statusWorld
                                .replace("{chunks}", String.valueOf(loadedChunks))
                                .replace("{entities}", String.valueOf(loadedEntities))), false);
                        context.getSource().sendSuccess(() -> Component.literal(msg.statusPlayers
                                .replace("{players}", String.valueOf(onlinePlayers))), false);
                        context.getSource().sendSuccess(() -> Component.literal(msg.statusCore
                                .replace("{enabled}", FXServerConfig.DATA.autoOptimizationEnabled ? "§aON" : "§cOFF")
                                .replace("{coreStatus}", coreStatus)), false);
                        context.getSource().sendSuccess(() -> Component.literal(msg.statusView
                                .replace("{view}", String.valueOf(FXServerManager.getCurrentViewDistance()))
                                .replace("{defView}", String.valueOf(FXServerConfig.DATA.defaultViewDistance))), false);
                        context.getSource().sendSuccess(() -> Component.literal(msg.statusTick
                                .replace("{tick}", String.valueOf(FXServerManager.getCurrentSimulationDistance()))
                                .replace("{defTick}", String.valueOf(FXServerConfig.DATA.defaultSimulationDistance))), false);
                        context.getSource().sendSuccess(() -> Component.literal(msg.statusMobCap
                                .replace("{mobcap}", String.valueOf((int)(FXServerManager.getCurrentMobCapMultiplier() * 100)))), false);
                        
                        context.getSource().sendSuccess(() -> Component.literal("§7> §fAvisos Staff: " + warnStatus + " §7| §fAlertas (Última 1h): §e" + recentWarns), false);
                        
                        return 1;
                    })
                )
                // /fxcore server warn <on/off>
                .then(Commands.literal("warn")
                    .then(Commands.argument("state", BoolArgumentType.bool())
                        .executes(context -> {
                            boolean enable = BoolArgumentType.getBool(context, "state");
                            FXServerConfig.DATA.warningsEnabled = enable;
                            FXServerConfig.salvar();
                            
                            String status = enable ? "§aATIVADOS" : "§cDESATIVADOS";
                            context.getSource().sendSuccess(() -> 
                                Component.literal("§a[FXCore] §fAvisos de performance agora estão: " + status), true);
                            return 1;
                        })
                    )
                )
                // /fxcore server change ...
                .then(Commands.literal("change")
                    // /fxcore server change viewdist <valor>
                    .then(Commands.literal("viewdist")
                        .then(Commands.argument("valor", IntegerArgumentType.integer(2, 32))
                            .executes(context -> {
                                int dist = IntegerArgumentType.getInteger(context, "valor");
                                FXServerManager.setViewDistance(context.getSource().getServer(), dist);
                                context.getSource().sendSuccess(() -> Component.literal(
                                        FXServerConfig.DATA.messages.forceViewSuccess.replace("{view}", String.valueOf(dist))
                                ), true);
                                return 1;
                            })
                        )
                    )
                    // /fxcore server change viewtick <valor>
                    .then(Commands.literal("viewtick")
                        .then(Commands.argument("valor", IntegerArgumentType.integer(2, 32))
                            .executes(context -> {
                                int dist = IntegerArgumentType.getInteger(context, "valor");
                                FXServerManager.setSimulationDistance(context.getSource().getServer(), dist);
                                context.getSource().sendSuccess(() -> Component.literal(
                                        FXServerConfig.DATA.messages.forceTickSuccess.replace("{tick}", String.valueOf(dist))
                                ), true);
                                return 1;
                            })
                        )
                    )
                    // /fxcore server change mobcap <valor%>
                    .then(Commands.literal("mobcap")
                        .then(Commands.argument("valor%", DoubleArgumentType.doubleArg(0.0, 200.0))
                            .executes(context -> {
                                double pct = DoubleArgumentType.getDouble(context, "valor%");
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
            )

            // /fxcore reload
            .then(Commands.literal("reload")
                .executes(context -> {
                    CommandSourceStack source = context.getSource();

                    try {
                        // 1. Recarrega as configurações unificadas
                        FXServerConfig.carregar();
                        FXBuildConfig.load();
                        FXClearConfig.load();

                        // 2. Persiste os dados dos jogadores online que estão em cache
                        for (ServerPlayer player : source.getServer().getPlayerList().getPlayers()) {
                            PlayerDataManager.save(PlayerDataManager.get(player.getUUID()));
                        }

                        source.sendSuccess(() -> Component.literal("§a[FXCore] Configurações e dados dos jogadores recarregados com sucesso!"), true);
                        return 1;
                    } catch (Exception e) {
                        source.sendFailure(Component.literal("§c[FXCore] Ocorreu um erro ao recarregar as configurações. Verifique o console."));
                        e.printStackTrace();
                        return 0;
                    }
                })
            )

            // /fxcore lagview <dimension>
            .then(Commands.literal("lagview")
                .then(Commands.argument("dimension", DimensionArgument.dimension())
                    .executes(context -> {
                        ServerLevel level = DimensionArgument.getDimension(context, "dimension");
                        FXLagView.generateReport(level, msg -> 
                            context.getSource().sendSuccess(() -> Component.literal(msg), false)
                        );
                        return 1;
                    })
                )
            )

            // /fxcore chunklag [chunkpos]
            .then(Commands.literal("chunklag")
                .executes(context -> {
                    if (context.getSource().getEntity() instanceof ServerPlayer player) {
                        ChunkPos cPos = player.chunkPosition();
                        ServerLevel level = player.serverLevel();
                        FXChunkAnalyzer.analyzeChunk(level, cPos, msg -> 
                            context.getSource().sendSuccess(() -> Component.literal(msg), false)
                        );
                    } else {
                        context.getSource().sendFailure(Component.literal("§cConsole deve especificar as coordenadas: /fxcore chunklag <x z>"));
                    }
                    return 1;
                })
                .then(Commands.argument("chunkpos", Vec2Argument.vec2(true))
                    .executes(context -> {
                        Vec2 pos = Vec2Argument.getVec2(context, "chunkpos");
                        ChunkPos cPos = new ChunkPos((int) pos.x, (int) pos.y);
                        ServerLevel level = context.getSource().getLevel();

                        FXChunkAnalyzer.analyzeChunk(level, cPos, msg -> 
                            context.getSource().sendSuccess(() -> Component.literal(msg), false)
                        );
                        return 1;
                    })
                )
            )

            // /fxcore chunkban
            .then(Commands.literal("chunkban")
                // delete <chunkpos>
                .then(Commands.literal("delete")
                    .then(Commands.argument("chunkpos", Vec2Argument.vec2(true))
                        .executes(context -> {
                            Vec2 pos = Vec2Argument.getVec2(context, "chunkpos");
                            ChunkPos cPos = new ChunkPos((int) pos.x, (int) pos.y);
                            ServerLevel level = context.getSource().getLevel();

                            FXChunkBan.deleteChunk(level, cPos, msg -> 
                                context.getSource().sendSuccess(() -> Component.literal(msg), true)
                            );
                            return 1;
                        })
                    )
                )
                // rollback <chunkpos>
                .then(Commands.literal("rollback")
                    .then(Commands.argument("chunkpos", Vec2Argument.vec2(true))
                        .executes(context -> {
                            Vec2 pos = Vec2Argument.getVec2(context, "chunkpos");
                            ChunkPos cPos = new ChunkPos((int) pos.x, (int) pos.y);
                            ServerLevel level = context.getSource().getLevel();

                            FXChunkBan.restoreChunk(level, cPos, msg -> 
                                context.getSource().sendSuccess(() -> Component.literal(msg), true)
                            );
                            return 1;
                        })
                    )
                )
            )
            // /fxcore chunkban spawn_test
            .then(Commands.literal("spawn_test")
                .executes(context -> {
                    if (context.getSource().getEntity() instanceof Player player) {
                        ServerLevel level = (ServerLevel) player.level();
                        ChunkPos cPos = player.chunkPosition();

                        FXTestChunkBan.spawnTestChunkBan(level, cPos, msg -> 
                            context.getSource().sendSuccess(() -> Component.literal(msg), true)
                        );
                    } else {
                        context.getSource().sendFailure(Component.literal("Este comando só pode ser executado por jogadores!"));
                    }
                    return 1;
                })
            )

            // /fxcore chunkpos
            .then(Commands.literal("chunkpos")
                .executes(context -> {
                    if (context.getSource().getEntity() instanceof ServerPlayer player) {
                        ChunkPos cPos = player.chunkPosition();
                        BlockPos bPos = player.blockPosition();
                        context.getSource().sendSuccess(() -> Component.literal(
                            "§a[FXCore] §fSua Chunk Atual: §eX: " + cPos.x + " | Z: " + cPos.z + 
                            " §7(Bloco X: " + bPos.getX() + " Y: " + bPos.getY() + " Z: " + bPos.getZ() + ")"
                        ), false);
                    } else {
                        context.getSource().sendFailure(Component.literal("§cApenas jogadores podem executar este comando sem argumentos!"));
                    }
                    return 1;
                })
            )

            // /fxcore server juststaff <on/off>
            .then(Commands.literal("juststaff")
                .then(Commands.literal("on")
                    .executes(context -> {
                        if (context.getSource().getEntity() instanceof ServerPlayer player) {
                            FXJustStaff.toggleJustStaff(player, true, msg -> 
                                context.getSource().sendSuccess(() -> Component.literal(msg), true)
                            );
                        }
                        return 1;
                    })
                )
                .then(Commands.literal("off")
                    .executes(context -> {
                        if (context.getSource().getEntity() instanceof ServerPlayer player) {
                            FXJustStaff.toggleJustStaff(player, false, msg -> 
                                context.getSource().sendSuccess(() -> Component.literal(msg), true)
                            );
                        }
                        return 1;
                    })
                )
            )

            // /fxcore afk
            .then(Commands.literal("afk")
                // tempo <valorEmS>
                .then(Commands.literal("tempo")
                    .then(Commands.argument("valorEmS", IntegerArgumentType.integer(1))
                        .executes(context -> {
                            int segundos = IntegerArgumentType.getInteger(context, "valorEmS");
                            FXServerConfig.DATA.maxAfkClickSeconds = segundos;
                            FXServerConfig.salvar();
                            
                            context.getSource().sendSuccess(() -> Component.literal("§a[FXCore] Tempo AFK ajustado para: " + segundos + "s"), true);
                            return 1;
                        })
                    )
                )
                // <on/off>
                .then(Commands.argument("state", BoolArgumentType.bool())
                    .executes(context -> {
                        boolean state = BoolArgumentType.getBool(context, "state");
                        FXServerConfig.DATA.enableAfkFarmGuard = state;
                        FXServerConfig.salvar();
                        
                        String statusStr = state ? "§aATIVADO" : "§cDESATIVADO";
                        context.getSource().sendSuccess(() -> Component.literal("§a[FXCore] Sistema AFK alterado para: " + statusStr), true);
                        return 1;
                    })
                )
            )
        );
    }
}