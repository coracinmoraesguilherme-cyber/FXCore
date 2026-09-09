package bz.fxcore.modules.rp;

import com.mojang.brigadier.CommandDispatcher;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.scores.PlayerTeam;
import net.minecraft.world.scores.Scoreboard;

public class RPCommand {
    private static final String TEAM_NAME = "off_rp_team";

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("rp")
            .executes(context -> {
                CommandSourceStack source = context.getSource();
                if (!(source.getEntity() instanceof ServerPlayer player)) {
                    source.sendFailure(Component.literal("§cApenas jogadores podem executar este comando."));
                    return 0;
                }

                RPManager.toggle(player.getUUID());
                boolean isOff = RPManager.isOff(player.getUUID());

                // Aplica ou remove o prefixo no nametag instantaneamente
                updateNameTag(player, isOff);

                if (isOff) {
                    player.sendSystemMessage(Component.literal("§e[RP] Você desativou seu RP. (OFF RP)"));
                } else {
                    player.sendSystemMessage(Component.literal("§e[RP] Você ativou seu RP novamente."));
                }

                return 1;
            })
        );
    }

    private static void updateNameTag(ServerPlayer player, boolean isOff) {
        Scoreboard scoreboard = player.getServer().getScoreboard();
        PlayerTeam team = scoreboard.getPlayersTeam(TEAM_NAME);

        if (team == null) {
            team = scoreboard.addPlayerTeam(TEAM_NAME);
            team.setPlayerPrefix(Component.literal("§c[OFF RP] "));
        }

        String playerName = player.getScoreboardName();
        PlayerTeam currentTeam = scoreboard.getPlayersTeam(playerName);

        if (isOff) {
            if (currentTeam != team) {
                scoreboard.addPlayerToTeam(playerName, team);
            }
        } else {
            if (currentTeam == team) {
                scoreboard.removePlayerFromTeam(playerName, team);
            }
        }
    }
}