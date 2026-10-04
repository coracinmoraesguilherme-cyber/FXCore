package bz.fxcore.core.otimi;

import bz.fxcore.core.config.FXCoreConfig;
import bz.fxcore.core.database.PlayerDataManager;
import bz.fxcore.core.otimi.chunk.FXChunkAnalyzer;
import bz.fxcore.core.otimi.chunk.FXChunkBan;
import bz.fxcore.core.otimi.chunk.FXLagView;
import bz.fxcore.core.otimi.chunk.FXTestChunkBan;
import bz.fxcore.core.staff.FXJustStaff;
import bz.fxcore.modules.build.FXBuildConfig;
import bz.fxcore.modules.build.particles.ParticleShapeJson;
import bz.fxcore.modules.chat.ChannelManager;
import bz.fxcore.modules.clear.FXClearConfig;
import bz.fxcore.modules.giveback.GiveBConfig;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.BoolArgumentType;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.DimensionArgument;
import net.minecraft.commands.arguments.coordinates.Vec2Argument;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.phys.Vec2;

public class FXCoreCommand {

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("fxcore")
            .requires(source -> source.hasPermission(2))

            // /fxcore reload
            .then(Commands.literal("reload")
                .executes(context -> {
                    CommandSourceStack source = context.getSource();

                    try {
                        // 1. Recarrega as configurações unificadas
                        FXCoreConfig.load();
                        FXBuildConfig.load();
                        FXClearConfig.load();
                        ParticleShapeJson.loadShapes(net.neoforged.fml.loading.FMLPaths.CONFIGDIR.get());
                        ChannelManager.load();
                        GiveBConfig.load();
                        
                        // 2. Persiste os dados dos jogadores online que estão em cache de forma assíncrona
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
                // delete <chunkX> <chunkZ>
                .then(Commands.literal("delete")
                    .then(Commands.argument("chunkX", IntegerArgumentType.integer())
                        .then(Commands.argument("chunkZ", IntegerArgumentType.integer())
                            .executes(context -> {
                                int x = IntegerArgumentType.getInteger(context, "chunkX");
                                int z = IntegerArgumentType.getInteger(context, "chunkZ");
                                ChunkPos cPos = new ChunkPos(x, z);
                                ServerLevel level = context.getSource().getLevel();

                                FXChunkBan.deleteChunk(level, cPos, msg -> 
                                    context.getSource().sendSuccess(() -> Component.literal(msg), true)
                                );
                                return 1;
                            })
                        )
                    )
                )
                // rollback <chunkX> <chunkZ>
                .then(Commands.literal("rollback")
                    .then(Commands.argument("chunkX", IntegerArgumentType.integer())
                        .then(Commands.argument("chunkZ", IntegerArgumentType.integer())
                            .executes(context -> {
                                int x = IntegerArgumentType.getInteger(context, "chunkX");
                                int z = IntegerArgumentType.getInteger(context, "chunkZ");
                                ChunkPos cPos = new ChunkPos(x, z);
                                ServerLevel level = context.getSource().getLevel();

                                FXChunkBan.restoreChunk(level, cPos, msg -> 
                                    context.getSource().sendSuccess(() -> Component.literal(msg), true)
                                );
                                return 1;
                            })
                        )
                    )
                )
                // regen <chunkX> <chunkZ>
                .then(Commands.literal("regen")
                    .then(Commands.argument("chunkX", IntegerArgumentType.integer())
                        .then(Commands.argument("chunkZ", IntegerArgumentType.integer())
                            .executes(context -> {
                                int x = IntegerArgumentType.getInteger(context, "chunkX");
                                int z = IntegerArgumentType.getInteger(context, "chunkZ");
                                ChunkPos cPos = new ChunkPos(x, z);
                                ServerLevel level = context.getSource().getLevel();

                                FXChunkBan.regenerateChunk(level, cPos, msg -> 
                                    context.getSource().sendSuccess(() -> Component.literal(msg), true)
                                );
                                return 1;
                            })
                        )
                    )
                )
                // spawn_test
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
            )

            // /fxcore chunkpos [x] [z]
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
                        context.getSource().sendFailure(Component.literal("§c[FXCore] O console deve especificar as coordenadas: /fxcore chunkpos <x> <z>"));
                    }
                    return 1;
                })
                .then(Commands.argument("x", IntegerArgumentType.integer())
                    .then(Commands.argument("z", IntegerArgumentType.integer())
                        .executes(context -> {
                            int x = IntegerArgumentType.getInteger(context, "x");
                            int z = IntegerArgumentType.getInteger(context, "z");
                            
                            // Converte as coordenadas do bloco para a ChunkPos correspondente
                            ChunkPos cPos = new ChunkPos(new BlockPos(x, 0, z));
                            
                            context.getSource().sendSuccess(() -> Component.literal(
                                "§a[FXCore] §fCoordenadas §eX: " + x + " | Z: " + z + 
                                " §fpertencem à Chunk: §eX: " + cPos.x + " | Z: " + cPos.z
                            ), false);
                            return 1;
                        })
                    )
                )
            )

            // /fxcore juststaff <on/off>
            .then(Commands.literal("juststaff")
                .then(Commands.literal("on")
                    .executes(context -> {
                        FXJustStaff.toggleJustStaff(context.getSource().getServer(), true, msg -> 
                            context.getSource().sendSuccess(() -> Component.literal(msg), true)
                        );
                        return 1;
                    })
                )
                .then(Commands.literal("off")
                    .executes(context -> {
                        FXJustStaff.toggleJustStaff(context.getSource().getServer(), false, msg -> 
                            context.getSource().sendSuccess(() -> Component.literal(msg), true)
                        );
                        return 1;
                    })
                )
            )

            // /fxcore afk
            .then(Commands.literal("afk")
                // tempo <valorEmS>
                .then(Commands.literal("tempo")
                    .then(Commands.argument("valorEmS", IntegerArgumentType.integer(10))
                        .executes(context -> {
                            int segundos = IntegerArgumentType.getInteger(context, "valorEmS");
                            FXCoreConfig.DATA.afk.maxAfkSeconds = segundos;
                            FXCoreConfig.save();
                            
                            context.getSource().sendSuccess(() -> Component.literal("§a[FXCore] Tempo AFK ajustado para: " + segundos + "s"), true);
                            return 1;
                        })
                    )
                )
                // <on/off>
                .then(Commands.argument("state", BoolArgumentType.bool())
                    .executes(context -> {
                        boolean state = BoolArgumentType.getBool(context, "state");
                        FXCoreConfig.DATA.afk.enabled = state;
                        FXCoreConfig.save();
                        
                        String statusStr = state ? "§aATIVADO" : "§cDESATIVADO";
                        context.getSource().sendSuccess(() -> Component.literal("§a[FXCore] Sistema AFK alterado para: " + statusStr), true);
                        return 1;
                    })
                )
            )
        );
    }
}