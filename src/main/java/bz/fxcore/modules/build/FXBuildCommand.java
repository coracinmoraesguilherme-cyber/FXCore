package bz.fxcore.modules.build;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.BoolArgumentType;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

public class FXBuildCommand {

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
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
        );
    }
}