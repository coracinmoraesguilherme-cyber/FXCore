package bz.fxcore.modules.build;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.BoolArgumentType;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import net.minecraft.commands.CommandBuildContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.ParticleArgument;
import net.minecraft.commands.arguments.coordinates.Vec3Argument;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.Vec3;
import bz.fxcore.modules.build.particles.FXParticleManager;
import com.mojang.brigadier.arguments.DoubleArgumentType;

public class FXBuildCommand {

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher, CommandBuildContext contextBuild) {
        dispatcher.register(Commands.literal("fxbuild")
            .requires(source -> source.hasPermission(2))
            
            .then(Commands.literal("place")
                .then(Commands.argument("state", BoolArgumentType.bool())
                    .executes(context -> {
                        if (context.getSource().getEntity() instanceof ServerPlayer player) {
                            boolean enable = BoolArgumentType.getBool(context, "state");
                            FXBuildManager.toggleBuildMode(player, enable);
                        } else {
                            context.getSource().sendFailure(Component.literal("§cApenas jogadores podem executar este comando."));
                        }
                        return 1;
                    })
                )
            )
            .then(Commands.literal("particle")
                .then(Commands.argument("preset", StringArgumentType.word())
                    .then(Commands.argument("particle", ParticleArgument.particle(contextBuild))
                        .then(Commands.argument("pos", Vec3Argument.vec3())
                            .then(Commands.argument("deltaX", DoubleArgumentType.doubleArg(0.0, 10.0))
                                .then(Commands.argument("deltaY", DoubleArgumentType.doubleArg(0.0, 10.0))
                                    .then(Commands.argument("deltaZ", DoubleArgumentType.doubleArg(0.0, 10.0))
                                        .then(Commands.argument("duration", IntegerArgumentType.integer(1, 300))
                                            .then(Commands.argument("anim", StringArgumentType.word())
                                                .executes(context -> {
                                                    CommandSourceStack source = context.getSource();
                                                    if (!(source.getEntity() instanceof ServerPlayer player)) {
                                                        source.sendFailure(Component.literal("§cApenas jogadores podem executar este comando."));
                                                        return 0;
                                                    }

                                                    String preset = StringArgumentType.getString(context, "preset");
                                                    ParticleOptions particleOpt = ParticleArgument.getParticle(context, "particle");
                                                    Vec3 pos = Vec3Argument.getVec3(context, "pos");
                                                    
                                                    // Pega os 3 valores de delta separadamente
                                                    float dx = (float) DoubleArgumentType.getDouble(context, "deltaX");
                                                    float dy = (float) DoubleArgumentType.getDouble(context, "deltaY");
                                                    float dz = (float) DoubleArgumentType.getDouble(context, "deltaZ");
                                                    float[] delta = new float[]{dx, dy, dz};

                                                    int durationSec = IntegerArgumentType.getInteger(context, "duration");
                                                    String anim = StringArgumentType.getString(context, "anim");

                                                    FXParticleManager.startTaskAt(player, preset, particleOpt, pos, delta, durationSec, anim);
                                                    
                                                    source.sendSuccess(() -> Component.literal("§a[FXCore] Efeito de partícula '" + preset + "' iniciado!"), false);
                                                    return 1;
                                                })
                                            )
                                        )
                                    )
                                )
                            )
                        )
                    )
                )
            )

            .then(Commands.literal("ptime")
                .then(Commands.argument("time", StringArgumentType.word())
                    .executes(context -> {
                        CommandSourceStack source = context.getSource();
                        if (!(source.getEntity() instanceof ServerPlayer player)) {
                            source.sendFailure(Component.literal("§cApenas jogadores podem executar este comando."));
                            return 0;
                        }

                        String timeArg = StringArgumentType.getString(context, "time").toLowerCase();
                        long targetTime;

                        if (timeArg.equals("day") || timeArg.equals("dia")) {
                            targetTime = 1000L;
                        } else if (timeArg.equals("night") || timeArg.equals("noite")) {
                            targetTime = 13000L;
                        } else if (timeArg.equals("noon") || timeArg.equals("meiodia")) {
                            targetTime = 6000L;
                        } else if (timeArg.equals("reset") || timeArg.equals("normal")) {
                            FXBuildManager.setCustomTime(player, null);
                            source.sendSuccess(() -> Component.literal("§a[FXCore] Seu horário pessoal foi redefinido para o do servidor."), false);
                            return 1;
                        } else {
                            try {
                                targetTime = Long.parseLong(timeArg);
                            } catch (NumberFormatException e) {
                                source.sendFailure(Component.literal("§c[FXCore] Use: /fxbuild ptime <day/night/noon/reset/número>"));
                                return 0;
                            }
                        }

                        FXBuildManager.setCustomTime(player, targetTime);
                        source.sendSuccess(() -> Component.literal("§a[FXCore] Horário pessoal travado com sucesso!"), false);
                        return 1;
                    })
                )
            )

            .then(Commands.literal("pweather")
                .then(Commands.argument("weather", StringArgumentType.word())
                    .executes(context -> {
                        CommandSourceStack source = context.getSource();
                        if (!(source.getEntity() instanceof ServerPlayer player)) {
                            source.sendFailure(Component.literal("§cApenas jogadores podem executar este comando."));
                            return 0;
                        }

                        String weatherArg = StringArgumentType.getString(context, "weather").toLowerCase();

                        if (weatherArg.equals("rain") || weatherArg.equals("chuva")) {
                            FXBuildManager.setCustomWeather(player, true);
                            source.sendSuccess(() -> Component.literal("§a[FXCore] Seu clima pessoal foi travado como chovendo."), false);
                        } else if (weatherArg.equals("sun") || weatherArg.equals("clear") || weatherArg.equals("limpo")) {
                            FXBuildManager.setCustomWeather(player, false);
                            source.sendSuccess(() -> Component.literal("§a[FXCore] Seu clima pessoal foi travado como ensolarado."), false);
                        } else if (weatherArg.equals("reset") || weatherArg.equals("normal")) {
                            FXBuildManager.setCustomWeather(player, null);
                            source.sendSuccess(() -> Component.literal("§a[FXCore] Clima pessoal sincronizado novamente com o servidor."), false);
                        } else {
                            source.sendFailure(Component.literal("§c[FXCore] Use: /fxbuild pweather <sun/rain/reset>"));
                            return 0;
                        }
                        return 1;
                    })
                )
            )
        );
    }

    private static float[] parseDelta(String deltaStr) {
        try {
            // Divide por espaços ou vírgulas para aceitar "0.1, 0.5, 0.1" ou "0.1 0.5 0.1"
            String[] parts = deltaStr.split("[,\\s]+");
            if (parts.length >= 3) {
                return new float[]{Float.parseFloat(parts[0]), Float.parseFloat(parts[1]), Float.parseFloat(parts[2])};
            } else if (parts.length == 1) {
                float val = Float.parseFloat(parts[0]);
                return new float[]{val, val, val};
            }
        } catch (Exception ignored) {}
        return new float[]{0.1f, 0.1f, 0.1f};
    }
}