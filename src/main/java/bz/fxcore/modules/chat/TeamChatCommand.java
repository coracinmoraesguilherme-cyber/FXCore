package bz.fxcore.modules.chat;

import bz.fxcore.core.database.FXPlayerData;
import bz.fxcore.core.database.PlayerDataManager;
import bz.fxcore.modules.staff.FXStaffData;
import bz.fxcore.modules.staff.TimeUtil;
import bz.fxcore.modules.team.TeamData;
import bz.fxcore.modules.team.TeamManager;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

public class TeamChatCommand {

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("tc")
            .then(Commands.argument("mensagem", StringArgumentType.greedyString())
                .executes(ctx -> {
                    ServerPlayer player = ctx.getSource().getPlayerOrException();

                    // 1. Checagem de Mute
                    if (FXStaffData.isMuted(player.getUUID())) {
                        long remaining = FXStaffData.getMuteRemainingTime(player.getUUID());
                        String timeText = TimeUtil.formatTime(remaining);
                        ctx.getSource().sendFailure(Component.literal("§c[FXCore] Você está mutado! Tempo restante: §f" + timeText));
                        return 0;
                    }

                    // 2. Checagem de Freeze
                    FXPlayerData senderData = PlayerDataManager.get(player.getUUID());
                    if (senderData != null && senderData.isFrozen) {
                        ctx.getSource().sendFailure(Component.literal("§c[FXCore] Você está congelado e não pode usar o chat de time."));
                        return 0;
                    }

                    TeamData team = TeamManager.getPlayerTeam(player.getUUID());
                    if (team == null) {
                        ctx.getSource().sendFailure(Component.literal("§c[FXCore] Você não está em um time para usar o /tc."));
                        return 0;
                    }

                    String msg = StringArgumentType.getString(ctx, "mensagem");
                    Component formattedTc = Component.literal("§3[TC] §f" + player.getName().getString() + "§7: §b" + msg);
                    Component spyTc = Component.literal("§8[SPY-TC] §7(" + team.tag + ") §f" + player.getName().getString() + "§7: §b" + msg);

                    // Transmissão para os membros do time e para admins com /spy ativo
                    for (ServerPlayer onlinePlayer : player.getServer().getPlayerList().getPlayers()) {
                        boolean isTeamMember = team.members.contains(onlinePlayer.getUUID());
                        
                        if (isTeamMember) {
                            onlinePlayer.sendSystemMessage(formattedTc);
                        } else {
                            // Envia para staff fora do time que possui SPY ativado
                            FXPlayerData targetData = PlayerDataManager.get(onlinePlayer.getUUID());
                            if (targetData != null && targetData.spyEnabled) {
                                onlinePlayer.sendSystemMessage(spyTc);
                            }
                        }
                    }
                    return 1;
                }))
        );
    }
}