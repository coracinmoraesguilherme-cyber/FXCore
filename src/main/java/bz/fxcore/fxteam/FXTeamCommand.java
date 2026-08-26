package bz.fxcore.fxteam;

import bz.fxcore.fxteam.TeamConfig;
import bz.fxcore.fxteam.TeamManager;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.scores.PlayerTeam;
import net.minecraft.world.scores.Scoreboard;

import java.util.Collection;
import java.util.UUID;

public class FXTeamCommand {

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(
            Commands.literal("fxteam")

                // --- CRIAR TIME ---
                .then(Commands.literal("create")
                    .then(Commands.argument("nome", StringArgumentType.word())
                        .then(Commands.argument("tag", StringArgumentType.word())
                            .executes(context -> {
                                ServerPlayer player = context.getSource().getPlayerOrException();

                                String tag = StringArgumentType.getString(context, "tag");
                                if (tag.length() > 3) {
                                    player.sendSystemMessage(Component.literal(TeamConfig.MESSAGES.tagLengthExceeded.replace("{MAX_LENGTH}", "3")));
                                    return 1;
                                }

                                if (TeamManager.estaEmAlgumTime(player)) {
                                    player.sendSystemMessage(Component.literal(TeamConfig.MESSAGES.alreadyInTeam));
                                    return 1;
                                }

                                if (TeamManager.ehLiderDeQualquerTime(player.getUUID())) {
                                    player.sendSystemMessage(Component.literal(TeamConfig.MESSAGES.alreadyLeader));
                                    return 1;
                                }

                                String nome = StringArgumentType.getString(context, "nome");

                                if (TeamManager.criarTime(player, nome, tag)) {
                                    String msg = TeamConfig.MESSAGES.teamCreatedSuccess
                                            .replace("{NAME}", nome)
                                            .replace("{TAG}", tag)
                                            .replace("{TAG_COLOR}", "§f");
                                    player.sendSystemMessage(Component.literal(msg));
                                } else {
                                    player.sendSystemMessage(Component.literal(TeamConfig.MESSAGES.teamCreateError));
                                }
                                return 1;
                            }))))

                // --- CONVIDAR JOGADOR ---
                .then(Commands.literal("invite")
                    .then(Commands.argument("jogador", EntityArgument.player())
                        .executes(context -> {
                            ServerPlayer lider = context.getSource().getPlayerOrException();
                            PlayerTeam team = lider.getTeam();

                            if (team == null || !TeamManager.ehLiderDeQualquerTime(lider.getUUID())) {
                                lider.sendSystemMessage(Component.literal(TeamConfig.MESSAGES.inviteOnlyLeader));
                                return 1;
                            }

                            ServerPlayer convidado = EntityArgument.getPlayer(context, "jogador");

                            if (TeamManager.estaEmAlgumTime(convidado)) {
                                lider.sendSystemMessage(Component.literal(TeamConfig.MESSAGES.targetAlreadyInTeam.replace("{PLAYER}", convidado.getScoreboardName())));
                                return 1;
                            }

                            TeamManager.CONVITES_PENDENTES.put(convidado.getUUID(), team.getName());

                            lider.sendSystemMessage(Component.literal(TeamConfig.MESSAGES.inviteSent.replace("{PLAYER}", convidado.getScoreboardName())));

                            String msgTexto = TeamConfig.MESSAGES.inviteReceived.replace("{NAME}", team.getDisplayName().getString());
                            Component msgConvite = Component.literal(msgTexto)
                                    .withStyle(style -> style
                                            .withClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/fxteam accept"))
                                            .withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, Component.literal(TeamConfig.MESSAGES.inviteHoverText))));

                            convidado.sendSystemMessage(msgConvite);
                            return 1;
                        })))

                // --- ACEITAR CONVITE ---
                .then(Commands.literal("accept")
                    .executes(context -> {
                        ServerPlayer player = context.getSource().getPlayerOrException();
                        String idTeam = TeamManager.CONVITES_PENDENTES.get(player.getUUID());

                        if (idTeam == null) {
                            player.sendSystemMessage(Component.literal(TeamConfig.MESSAGES.noPendingInvites));
                            return 1;
                        }

                        if (TeamManager.estaEmAlgumTime(player)) {
                            player.sendSystemMessage(Component.literal(TeamConfig.MESSAGES.alreadyInTeam));
                            TeamManager.CONVITES_PENDENTES.remove(player.getUUID());
                            return 1;
                        }

                        Scoreboard sb = player.getServer().getScoreboard();
                        PlayerTeam team = sb.getPlayerTeam(idTeam);

                        if (team != null) {
                            sb.addPlayerToTeam(player.getScoreboardName(), team);
                            player.sendSystemMessage(Component.literal(TeamConfig.MESSAGES.joinedTeamSuccess.replace("{NAME}", team.getDisplayName().getString())));
                            TeamManager.CONVITES_PENDENTES.remove(player.getUUID());
                        } else {
                            player.sendSystemMessage(Component.literal(TeamConfig.MESSAGES.teamNoLongerExists));
                        }
                        return 1;
                    }))

                // --- REMOVER JOGADOR (KICK) ---
                .then(Commands.literal("remove")
                    .then(Commands.argument("jogador", EntityArgument.player())
                        .executes(context -> {
                            ServerPlayer lider = context.getSource().getPlayerOrException();
                            PlayerTeam team = lider.getTeam();

                            if (team == null || !TeamManager.ehLiderDeQualquerTime(lider.getUUID())) {
                                lider.sendSystemMessage(Component.literal(TeamConfig.MESSAGES.removeOnlyLeader));
                                return 1;
                            }

                            ServerPlayer alvo = EntityArgument.getPlayer(context, "jogador");
                            if (alvo.getUUID().equals(lider.getUUID())) {
                                lider.sendSystemMessage(Component.literal(TeamConfig.MESSAGES.cannotRemoveSelf));
                                return 1;
                            }

                            Scoreboard sb = lider.getServer().getScoreboard();
                            sb.removePlayerFromTeam(alvo.getScoreboardName(), team);

                            lider.sendSystemMessage(Component.literal(TeamConfig.MESSAGES.playerRemovedLeader.replace("{PLAYER}", alvo.getScoreboardName())));
                            alvo.sendSystemMessage(Component.literal(TeamConfig.MESSAGES.playerRemovedTarget.replace("{NAME}", team.getDisplayName().getString())));
                            return 1;
                        })))

                // --- SAIR DO TIME ---
                .then(Commands.literal("leave")
                    .executes(context -> {
                        ServerPlayer player = context.getSource().getPlayerOrException();
                        PlayerTeam team = player.getTeam();

                        if (team == null) {
                            player.sendSystemMessage(Component.literal(TeamConfig.MESSAGES.notInTeam));
                            return 1;
                        }

                        if (TeamManager.ehLiderDeQualquerTime(player.getUUID())) {
                            player.sendSystemMessage(Component.literal(TeamConfig.MESSAGES.leaderCannotLeave));
                            return 1;
                        }

                        Scoreboard sb = player.getServer().getScoreboard();
                        sb.removePlayerFromTeam(player.getScoreboardName(), team);
                        player.sendSystemMessage(Component.literal(TeamConfig.MESSAGES.playerLeft.replace("{NAME}", team.getDisplayName().getString())));
                        return 1;
                    }))

                // --- LISTAR JOGADORES DO PRÓPRIO TIME ---
                .then(Commands.literal("list")
                    .executes(context -> {
                        ServerPlayer player = context.getSource().getPlayerOrException();
                        PlayerTeam team = player.getTeam();

                        if (team == null) {
                            player.sendSystemMessage(Component.literal(TeamConfig.MESSAGES.notInTeam));
                            return 1;
                        }

                        Collection<String> membros = team.getPlayers();
                        player.sendSystemMessage(Component.literal("§e=== Jogadores do Time (" + membros.size() + ") ==="));
                        if (membros.isEmpty()) {
                            player.sendSystemMessage(Component.literal("§7Nenhum jogador encontrado neste time."));
                        } else {
                            String listaMembros = String.join("§f, §a", membros);
                            player.sendSystemMessage(Component.literal("§a" + listaMembros));
                        }
                        return 1;
                    }))

                // --- MODIFICAR TIME ---
                .then(Commands.literal("modify")
                    .then(Commands.literal("color")
                        .then(Commands.argument("cor", StringArgumentType.word())
                            .executes(context -> {
                                ServerPlayer lider = context.getSource().getPlayerOrException();
                                PlayerTeam team = lider.getTeam();

                                if (team == null || !TeamManager.ehLiderDeQualquerTime(lider.getUUID())) {
                                    lider.sendSystemMessage(Component.literal(TeamConfig.MESSAGES.modifyOnlyLeader));
                                    return 1;
                                }

                                String corNome = StringArgumentType.getString(context, "cor").toUpperCase();
                                ChatFormatting cor = ChatFormatting.getByName(corNome);

                                if (cor != null && cor.isColor()) {
                                    team.setColor(cor);
                                    lider.sendSystemMessage(Component.literal(TeamConfig.MESSAGES.colorSuccess.replace("{COLOR_NAME}", cor + cor.getName())));
                                } else {
                                    lider.sendSystemMessage(Component.literal(TeamConfig.MESSAGES.invalidColor));
                                }
                                return 1;
                            })))

                    .then(Commands.literal("tag")
                        .then(Commands.argument("nova_tag", StringArgumentType.word())
                            .executes(context -> {
                                ServerPlayer lider = context.getSource().getPlayerOrException();
                                PlayerTeam team = lider.getTeam();

                                if (team == null || !TeamManager.ehLiderDeQualquerTime(lider.getUUID())) {
                                    lider.sendSystemMessage(Component.literal(TeamConfig.MESSAGES.modifyOnlyLeader));
                                    return 1;
                                }

                                String novaTag = StringArgumentType.getString(context, "nova_tag");
                                if (novaTag.length() > 3) {
                                    lider.sendSystemMessage(Component.literal(TeamConfig.MESSAGES.tagLengthExceeded.replace("{MAX_LENGTH}", "3")));
                                    return 1;
                                }

                                String prefixoAtual = team.getPlayerPrefix().getString();
                                String codigoCorAtual = "§f";

                                if (prefixoAtual.contains("§") && prefixoAtual.length() >= 5) {
                                    int idx = prefixoAtual.indexOf("§", 3);
                                    if (idx != -1 && idx + 1 < prefixoAtual.length()) {
                                        codigoCorAtual = prefixoAtual.substring(idx, idx + 2);
                                    }
                                }

                                TeamManager.atualizarTagTime(team, novaTag, codigoCorAtual);
                                lider.sendSystemMessage(Component.literal(TeamConfig.MESSAGES.tagUpdateSuccess.replace("{TAG_FORMATTED}", team.getPlayerPrefix().getString())));
                                return 1;
                            })))

                    .then(Commands.literal("tagcolor")
                        .then(Commands.argument("cor", StringArgumentType.word())
                            .executes(context -> {
                                ServerPlayer lider = context.getSource().getPlayerOrException();
                                PlayerTeam team = lider.getTeam();

                                if (team == null || !TeamManager.ehLiderDeQualquerTime(lider.getUUID())) {
                                    lider.sendSystemMessage(Component.literal(TeamConfig.MESSAGES.modifyOnlyLeader));
                                    return 1;
                                }

                                String corNome = StringArgumentType.getString(context, "cor").toUpperCase();
                                ChatFormatting cor = ChatFormatting.getByName(corNome);

                                if (cor != null && cor.isColor()) {
                                    String prefixoLimpo = team.getPlayerPrefix().getString()
                                            .replaceAll("§[0-9a-fk-orA-FK-OR]", "")
                                            .replace("[", "")
                                            .replace("]", "")
                                            .trim();

                                    TeamManager.atualizarTagTime(team, prefixoLimpo, cor.toString());
                                    lider.sendSystemMessage(Component.literal(TeamConfig.MESSAGES.tagColorSuccess));
                                } else {
                                    lider.sendSystemMessage(Component.literal(TeamConfig.MESSAGES.invalidColor));
                                }
                                return 1;
                            }))))

                // --- DELETAR TIME ---
                .then(Commands.literal("delete")
                    .executes(context -> {
                        ServerPlayer lider = context.getSource().getPlayerOrException();
                        PlayerTeam team = lider.getTeam();

                        if (team == null || !TeamManager.ehLiderDeQualquerTime(lider.getUUID())) {
                            lider.sendSystemMessage(Component.literal(TeamConfig.MESSAGES.deleteOnlyLeader));
                            return 1;
                        }

                        String idTime = team.getName();
                        TeamManager.removerLider(lider.getUUID());
                        TeamManager.deletarTime(lider.getServer(), idTime);

                        lider.sendSystemMessage(Component.literal(TeamConfig.MESSAGES.teamDeletedSuccess));
                        return 1;
                    }))

                // --- INFORMAÇÕES DO TIME ---
                .then(Commands.literal("info")
                    .executes(context -> {
                        ServerPlayer player = context.getSource().getPlayerOrException();
                        PlayerTeam team = player.getTeam();

                        if (team == null) {
                            player.sendSystemMessage(Component.literal(TeamConfig.MESSAGES.notInTeam));
                            return 1;
                        }

                        player.sendSystemMessage(Component.literal(TeamConfig.MESSAGES.teamInfoHeader));
                        player.sendSystemMessage(Component.literal(TeamConfig.MESSAGES.teamInfoName.replace("{NAME}", team.getDisplayName().getString())));
                        player.sendSystemMessage(Component.literal(TeamConfig.MESSAGES.teamInfoTag.replace("{TAG_FORMATTED}", team.getPlayerPrefix().getString())));
                        player.sendSystemMessage(Component.literal(TeamConfig.MESSAGES.teamInfoMembers.replace("{MEMBERS_COUNT}", String.valueOf(team.getPlayers().size()))));
                        return 1;
                    }))

                // --- ADM ---
                .then(Commands.literal("adm")
                    .requires(source -> source.hasPermission(2))
                    .then(Commands.literal("list")
                        .executes(context -> {
                            var source = context.getSource();
                            Scoreboard sb = source.getServer().getScoreboard();

                            source.sendSuccess(() -> Component.literal(TeamConfig.MESSAGES.admListHeader), false);
                            for (PlayerTeam team : sb.getPlayerTeams()) {
                                if (team.getName().startsWith("fx_")) {
                                    String item = TeamConfig.MESSAGES.admListItem
                                            .replace("{ID}", team.getName())
                                            .replace("{NAME}", team.getDisplayName().getString())
                                            .replace("{MEMBERS_COUNT}", String.valueOf(team.getPlayers().size()));
                                    source.sendSuccess(() -> Component.literal(item), false);
                                }
                            }
                            return 1;
                        }))

                    .then(Commands.literal("players")
                        .then(Commands.argument("id_time", StringArgumentType.word())
                            .executes(context -> {
                                var source = context.getSource();
                                String idTime = StringArgumentType.getString(context, "id_time");
                                Scoreboard sb = source.getServer().getScoreboard();
                                PlayerTeam team = sb.getPlayerTeam(idTime);

                                if (team == null) {
                                    source.sendFailure(Component.literal("§cTime não encontrado com a ID: " + idTime));
                                    return 1;
                                }

                                Collection<String> membros = team.getPlayers();
                                source.sendSuccess(() -> Component.literal("§e=== Jogadores do Time " + team.getDisplayName().getString() + " (" + membros.size() + ") ==="), false);
                                if (membros.isEmpty()) {
                                    source.sendSuccess(() -> Component.literal("§7Nenhum jogador encontrado neste time."), false);
                                } else {
                                    String listaMembros = String.join("§f, §a", membros);
                                    source.sendSuccess(() -> Component.literal("§a" + listaMembros), false);
                                }
                                return 1;
                            })))

                    .then(Commands.literal("forcedelete")
                        .then(Commands.argument("id_time", StringArgumentType.word())
                            .executes(context -> {
                                var source = context.getSource();
                                String idTime = StringArgumentType.getString(context, "id_time");

                                if (TeamManager.deletarTime(source.getServer(), idTime)) {
                                    source.sendSuccess(() -> Component.literal(TeamConfig.MESSAGES.admForceDeleteSuccess.replace("{ID}", idTime)), true);
                                } else {
                                    source.sendFailure(Component.literal(TeamConfig.MESSAGES.admForceDeleteNotFound));
                                }
                                return 1;
                            })))

                    .then(Commands.literal("spy")
                        .executes(context -> {
                            ServerPlayer admin = context.getSource().getPlayerOrException();
                            UUID adminUuid = admin.getUUID();

                            if (TeamManager.TEAM_SPY_ATIVOS.contains(adminUuid)) {
                                TeamManager.TEAM_SPY_ATIVOS.remove(adminUuid);
                                admin.sendSystemMessage(Component.literal(TeamConfig.MESSAGES.spyDisabled));
                            } else {
                                TeamManager.TEAM_SPY_ATIVOS.add(adminUuid);
                                admin.sendSystemMessage(Component.literal(TeamConfig.MESSAGES.spyEnabled));
                            }
                            return 1;
                        })))
        );
    }
}