package bz.fxcore.fxbuild;

import com.mojang.brigadier.CommandDispatcher;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

public class FXBuildCommands {

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(
            Commands.literal("fxbuild")
                .requires(source -> source.hasPermission(2)) // Exclusivo para Staff / Op
                .then(Commands.literal("place")
                    .executes(context -> {
                        ServerPlayer player = context.getSource().getPlayerOrException();
                        boolean enabled = FXBuildState.toggleFreePlace(player.getUUID());

                        if (enabled) {
                            player.sendSystemMessage(Component.literal("§a[FXBuild] Colocação livre ativada! Agora você pode colocar blocos em posições 'ilegais'."));
                        } else {
                            player.sendSystemMessage(Component.literal("§c[FXBuild] Colocação livre desativada. Comportamento padrão restaurado."));
                        }
                        return 1;
                    })
                )
        );
    }
}