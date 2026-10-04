package bz.fxcore.modules.build;

import bz.fxcore.modules.build.particles.FXParticleManager;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.BoolArgumentType;
import com.mojang.brigadier.arguments.DoubleArgumentType;
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
        );
    }
}