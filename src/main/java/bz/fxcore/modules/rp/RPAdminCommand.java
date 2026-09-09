package bz.fxcore.modules.rp;

import com.mojang.brigadier.CommandDispatcher;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;


public class RPAdminCommand {
    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("rpadm")
            .requires(source -> source.hasPermission(2))
            
            .then(Commands.literal("list")
                .executes(context -> {
                    CommandSourceStack source = context.getSource();
                    var offList = RPManager.getOffPlayers();
                    
                    source.sendSuccess(() -> Component.literal("§6--- Jogadores com OFF RP ---"), false);
                    if (offList.isEmpty()) {
                        source.sendSuccess(() -> Component.literal("§7Nenhum jogador com RP desativado no momento."), false);
                    } else {
                        for (java.util.UUID uuid : offList) {
                            ServerPlayer p = source.getServer().getPlayerList().getPlayer(uuid);
                            String name = (p != null) ? p.getName().getString() : uuid.toString();
                            source.sendSuccess(() -> Component.literal("§c- " + name), false);
                        }
                    }
                    return 1;
                })
            )

            .then(Commands.literal("toggle")
                .then(Commands.argument("player", EntityArgument.player())
                    .executes(context -> {
                        CommandSourceStack source = context.getSource();
                        ServerPlayer target = EntityArgument.getPlayer(context, "player");

                        RPManager.toggle(target.getUUID());
                        boolean isOff = RPManager.isOff(target.getUUID());

                        if (isOff) {
                            source.sendSuccess(() -> Component.literal("§a[RP] Você desativou o RP de " + target.getName().getString()), true);
                            target.sendSystemMessage(Component.literal("§cUm administrador desativou o seu RP."));
                        } else {
                            source.sendSuccess(() -> Component.literal("§a[RP] Você ativou o RP de " + target.getName().getString()), true);
                            target.sendSystemMessage(Component.literal("§aUm administrador ativou o seu RP."));
                        }
                        return 1;
                    })
                )
            )
        );
    }
}