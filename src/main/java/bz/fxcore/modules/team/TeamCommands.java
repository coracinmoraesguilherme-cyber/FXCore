package bz.fxcore.modules.team;

import bz.fxcore.core.database.PlayerDataManager;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;

import java.util.UUID;

public class TeamCommands {

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        for (String label : new String[]{"fxteam", "fxteams"}) {
            dispatcher.register(Commands.literal(label)
                
                // create <nome> <tag>
                .then(Commands.literal("create")
                    .then(Commands.argument("nome", StringArgumentType.word())
                        .then(Commands.argument("tag", StringArgumentType.word())
                            .executes(ctx -> {
                                ServerPlayer player = ctx.getSource().getPlayerOrException();
                                if (TeamManager.hasTeam(player.getUUID())) {
                                    ctx.getSource().sendFailure(Component.literal("§c[FXCore] Você já está em um time e não pode criar ou entrar em outro!"));
                                    return 0;
                                }

                                String tag = StringArgumentType.getString(ctx, "tag");
                                if (tag.length() > 5) {
                                    ctx.getSource().sendFailure(Component.literal("§c[FXCore] A tag do time deve ter no máximo 5 caracteres!"));
                                    return 0;
                                }

                                String nome = StringArgumentType.getString(ctx, "nome");
                                TeamData team = TeamManager.createTeam(nome, tag, player.getUUID(), player.getName().getString());
                                ctx.getSource().sendSuccess(() -> Component.literal("§a[FXCore] Time " + team.name + " [" + team.tag + "] criado!"), false);
                                return 1;
                            }))))

                // invite <player>
                .then(Commands.literal("invite")
                    .then(Commands.argument("player", EntityArgument.player())
                        .executes(ctx -> {
                            ServerPlayer sourcePlayer = ctx.getSource().getPlayerOrException();
                            ServerPlayer targetPlayer = EntityArgument.getPlayer(ctx, "player");
                            String targetName = targetPlayer.getName().getString();
                            UUID targetUuid = targetPlayer.getUUID();

                            TeamData team = TeamManager.getPlayerTeam(sourcePlayer.getUUID());
                            if (team == null || !team.owner.equals(sourcePlayer.getUUID())) {
                                ctx.getSource().sendFailure(Component.literal("§c[FXCore] Apenas o dono do time pode convidar novos membros."));
                                return 0;
                            }

                            if (targetUuid.equals(sourcePlayer.getUUID())) {
                                ctx.getSource().sendFailure(Component.literal("§c[FXCore] Você não pode convidar a si mesmo!"));
                                return 0;
                            }

                            if (TeamManager.hasTeam(targetUuid)) {
                                ctx.getSource().sendFailure(Component.literal("§c[FXCore] Este jogador já faz parte de um time!"));
                                return 0;
                            }

                            TeamManager.invitePlayer(targetUuid, team.id);
                            ctx.getSource().sendSuccess(() -> Component.literal("§a[FXCore] Convite enviado para " + targetName), false);

                            Component inviteMsg = Component.literal("§e[FXCore] Você foi convidado para o time §f" + team.name + "§e! ")
                                .append(Component.literal("§a§l[CLIQUE AQUI PARA ACEITAR]")
                                .withStyle(style -> style
                                    .withClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/" + label + " accept"))
                                    .withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, Component.literal("§7Clique para entrar no time")))));

                            targetPlayer.sendSystemMessage(inviteMsg);
                            return 1;
                        })))

                // accept
                .then(Commands.literal("accept")
                    .executes(ctx -> {
                        ServerPlayer player = ctx.getSource().getPlayerOrException();
                        if (TeamManager.hasTeam(player.getUUID())) {
                            ctx.getSource().sendFailure(Component.literal("§c[FXCore] Você já pertence a um time!"));
                            return 0;
                        }

                        String teamId = TeamManager.getPendingInvite(player.getUUID());
                        if (teamId == null) {
                            ctx.getSource().sendFailure(Component.literal("§c[FXCore] Você não possui convites pendentes."));
                            return 0;
                        }

                        TeamData team = TeamManager.getAllTeams().stream().filter(t -> t.id.equals(teamId)).findFirst().orElse(null);
                        if (team != null) {
                            TeamManager.addMember(team, player.getUUID());
                            TeamManager.removeInvite(player.getUUID());
                            ctx.getSource().sendSuccess(() -> Component.literal("§a[FXCore] Você entrou para o time " + team.name + "!"), false);
                        }
                        return 1;
                    }))

                // leave (Sair do time atual)
                .then(Commands.literal("leave")
                    .executes(ctx -> {
                        ServerPlayer player = ctx.getSource().getPlayerOrException();
                        TeamData team = TeamManager.getPlayerTeam(player.getUUID());

                        if (team == null) {
                            ctx.getSource().sendFailure(Component.literal("§c[FXCore] Você não está em nenhum time."));
                            return 0;
                        }

                        if (team.owner.equals(player.getUUID())) {
                            ctx.getSource().sendFailure(Component.literal("§c[FXCore] O dono não pode sair do time. Use /fxteam delete para apagar o time ou repasse a liderança."));
                            return 0;
                        }

                        if (TeamManager.removeMember(team, player.getUUID())) {
                            ctx.getSource().sendSuccess(() -> Component.literal("§a[FXCore] Você saiu do time " + team.name + "."), false);
                            
                            // Avisar o dono caso ele esteja online
                            ServerPlayer ownerPlayer = ctx.getSource().getServer().getPlayerList().getPlayer(team.owner);
                            if (ownerPlayer != null) {
                                ownerPlayer.sendSystemMessage(Component.literal("§e[FXCore] O jogador §f" + player.getName().getString() + " §esaiu do seu time."));
                            }
                            return 1;
                        }

                        ctx.getSource().sendFailure(Component.literal("§c[FXCore] Ocorreu um erro ao tentar sair do time."));
                        return 0;
                    }))

                // delete (Somente o dono)
                .then(Commands.literal("delete")
                    .executes(ctx -> {
                        ServerPlayer player = ctx.getSource().getPlayerOrException();
                        TeamData team = TeamManager.getPlayerTeam(player.getUUID());

                        if (team == null || !team.owner.equals(player.getUUID())) {
                            ctx.getSource().sendFailure(Component.literal("§c[FXCore] Apenas o dono pode deletar o time."));
                            return 0;
                        }

                        TeamManager.deleteTeam(team.id);
                        ctx.getSource().sendSuccess(() -> Component.literal("§a[FXCore] Time removido com sucesso."), false);
                        return 1;
                    }))

                // change color / tag / tagcolor (Somente o dono)
                .then(Commands.literal("change")
                    .then(Commands.literal("color")
                        .then(Commands.argument("cor", StringArgumentType.word())
                            .executes(ctx -> {
                                ServerPlayer player = ctx.getSource().getPlayerOrException();
                                TeamData team = TeamManager.getPlayerTeam(player.getUUID());
                                if (team != null && team.owner.equals(player.getUUID())) {
                                    team.color = parseColor(StringArgumentType.getString(ctx, "cor"));
                                    TeamManager.save();
                                    ctx.getSource().sendSuccess(() -> Component.literal("§a[FXCore] Cor do time alterada."), false);
                                    return 1;
                                }
                                return 0;
                            })))
                    .then(Commands.literal("tag")
                        .then(Commands.argument("novaTag", StringArgumentType.word())
                            .executes(ctx -> {
                                ServerPlayer player = ctx.getSource().getPlayerOrException();
                                TeamData team = TeamManager.getPlayerTeam(player.getUUID());

                                if (team != null && team.owner.equals(player.getUUID())) {
                                    String novaTag = StringArgumentType.getString(ctx, "novaTag");
                                    if (novaTag.length() > 5) {
                                        ctx.getSource().sendFailure(Component.literal("§c[FXCore] A tag do time deve ter no máximo 5 caracteres!"));
                                        return 0;
                                    }

                                    team.tag = novaTag;
                                    TeamManager.save();
                                    ctx.getSource().sendSuccess(() -> Component.literal("§a[FXCore] Tag atualizada."), false);
                                    return 1;
                                }
                                return 0;
                            })))
                    .then(Commands.literal("tagcolor")
                        .then(Commands.argument("corTag", StringArgumentType.word())
                            .executes(ctx -> {
                                ServerPlayer player = ctx.getSource().getPlayerOrException();
                                TeamData team = TeamManager.getPlayerTeam(player.getUUID());
                                if (team != null && team.owner.equals(player.getUUID())) {
                                    team.tagColor = parseColor(StringArgumentType.getString(ctx, "corTag"));
                                    TeamManager.save();
                                    ctx.getSource().sendSuccess(() -> Component.literal("§a[FXCore] Cor da tag alterada."), false);
                                    return 1;
                                }
                                return 0;
                            }))))

                // remove <player> ["motivo"]
                .then(Commands.literal("remove")
                    .then(Commands.argument("player", StringArgumentType.word())
                        .executes(ctx -> executeRemove(ctx.getSource(), StringArgumentType.getString(ctx, "player"), "Sem motivo especificado"))
                        .then(Commands.argument("motivo", StringArgumentType.greedyString())
                            .executes(ctx -> executeRemove(ctx.getSource(), StringArgumentType.getString(ctx, "player"), StringArgumentType.getString(ctx, "motivo"))))))

                // info
                .then(Commands.literal("info")
                    .executes(ctx -> {
                        ServerPlayer player = ctx.getSource().getPlayerOrException();
                        TeamData team = TeamManager.getPlayerTeam(player.getUUID());
                        if (team == null) {
                            ctx.getSource().sendFailure(Component.literal("§c[FXCore] Você não está em um time."));
                            return 0;
                        }
                        showTeamInfo(ctx.getSource(), team);
                        return 1;
                    }))

                // adm list / info <idtime> / delete <idtime> (Staff only)
                .then(Commands.literal("adm")
                    .requires(source -> source.hasPermission(2))
                    .then(Commands.literal("list")
                        .executes(ctx -> {
                            ctx.getSource().sendSuccess(() -> Component.literal("§e========== LISTA DE TIMES =========="), false);
                            for (TeamData t : TeamManager.getAllTeams()) {
                                ctx.getSource().sendSuccess(() -> Component.literal("§8- §f" + t.name + " §7[" + t.tag + "] §8| ID: §f" + t.id), false);
                            }
                            return 1;
                        }))
                    .then(Commands.literal("info")
                        .then(Commands.argument("idtime", StringArgumentType.word())
                            .executes(ctx -> {
                                String id = StringArgumentType.getString(ctx, "idtime");
                                TeamData team = TeamManager.getAllTeams().stream().filter(t -> t.id.equals(id)).findFirst().orElse(null);
                                if (team != null) {
                                    showTeamInfo(ctx.getSource(), team);
                                } else {
                                    ctx.getSource().sendFailure(Component.literal("§c[FXCore] Time com ID '" + id + "' não encontrado."));
                                }
                                return 1;
                            })))
                    .then(Commands.literal("delete")
                        .then(Commands.argument("idtime", StringArgumentType.word())
                            .executes(ctx -> {
                                String id = StringArgumentType.getString(ctx, "idtime");
                                TeamManager.deleteTeam(id);
                                ctx.getSource().sendSuccess(() -> Component.literal("§a[FXCore] Time " + id + " deletado com sucesso."), false);
                                return 1;
                            }))))
            );
        }
    }

    private static int executeRemove(CommandSourceStack source, String targetName, String reason) {
        try {
            ServerPlayer player = source.getPlayerOrException();
            TeamData team = TeamManager.getPlayerTeam(player.getUUID());

            if (team == null || !team.owner.equals(player.getUUID())) {
                source.sendFailure(Component.literal("§c[FXCore] Apenas o dono pode remover membros."));
                return 0;
            }

            MinecraftServer server = source.getServer();
            UUID targetUuid = PlayerDataManager.getUUIDByName(server, targetName);

            if (targetUuid == null) {
                source.sendFailure(Component.literal("§c[FXCore] O jogador '" + targetName + "' nunca entrou no servidor."));
                return 0;
            }

            if (team.owner.equals(targetUuid)) {
                source.sendFailure(Component.literal("§c[FXCore] Você não pode remover a si mesmo sendo o dono. Use /fxteam delete."));
                return 0;
            }

            if (TeamManager.removeMember(team, targetUuid)) {
                team.offlineKicks.put(targetUuid, reason);
                TeamManager.save();
                source.sendSuccess(() -> Component.literal("§a[FXCore] " + targetName + " foi removido do time."), false);
                
                ServerPlayer targetPlayer = server.getPlayerList().getPlayerByName(targetName);
                if (targetPlayer != null) {
                    targetPlayer.sendSystemMessage(Component.literal("§c[FXCore] Você foi removido do time " + team.name + ". Motivo: §f" + reason));
                }
                return 1;
            } else {
                source.sendFailure(Component.literal("§c[FXCore] O jogador não está no seu time."));
                return 0;
            }
        } catch (CommandSyntaxException e) {
            source.sendFailure(Component.literal("§c[FXCore] Este comando só pode ser executado por um jogador."));
            return 0;
        }
    }

    private static void showTeamInfo(CommandSourceStack source, TeamData team) {
        source.sendSuccess(() -> Component.literal("§e========== INFO DO TIME =========="), false);
        source.sendSuccess(() -> Component.literal("§7Nome: " + parseColor(team.color) + team.name), false);
        source.sendSuccess(() -> Component.literal("§7Tag: " + parseColor(team.tagColor) + "[" + team.tag + "]"), false);
        source.sendSuccess(() -> Component.literal("§7ID do Time: §f" + team.id), false);
        
        String ownerName = getPlayerNameByUUID(source, team.owner);
        source.sendSuccess(() -> Component.literal("§7Dono: §f" + ownerName), false);
        
        StringBuilder membersList = new StringBuilder();
        for (int i = 0; i < team.members.size(); i++) {
            UUID memberUuid = team.members.get(i);
            ServerPlayer onlineMember = source.getServer().getPlayerList().getPlayer(memberUuid);
            
            if (onlineMember != null) {
                membersList.append("§a").append(onlineMember.getName().getString()).append(" §7(Online)");
            } else {
                membersList.append("§c").append(getPlayerNameByUUID(source, memberUuid)).append(" §7(Offline)");
            }
            
            if (i < team.members.size() - 1) {
                membersList.append("§7, ");
            }
        }
        
        int totalMembers = team.members.size();
        source.sendSuccess(() -> Component.literal("§7Membros (" + totalMembers + "): " + membersList.toString()), false);
    }

    public static String parseColor(String input) {
        if (input == null) return "§f";
        
        String formatted = input.replace("&", "§");
        
        return switch (formatted.toLowerCase()) {
            case "aqua" -> "§b";
            case "black" -> "§0";
            case "blue" -> "§9";
            case "dark_aqua", "darkaqua" -> "§3";
            case "dark_blue", "darkblue" -> "§1";
            case "dark_gray", "darkgray" -> "§8";
            case "dark_green", "darkgreen" -> "§2";
            case "dark_purple", "darkpurple" -> "§5";
            case "dark_red", "darkred" -> "§4";
            case "gold" -> "§6";
            case "gray" -> "§7";
            case "green" -> "§a";
            case "light_purple", "lightpurple" -> "§d";
            case "red" -> "§c";
            case "white" -> "§f";
            case "yellow" -> "§e";
            default -> formatted.startsWith("§") ? formatted : "§" + formatted;
        };
    }

    private static String getPlayerNameByUUID(CommandSourceStack source, UUID uuid) {
        ServerPlayer player = source.getServer().getPlayerList().getPlayer(uuid);
        if (player != null) {
            return player.getName().getString();
        }
        
        var profile = source.getServer().getProfileCache().get(uuid);
        return profile.isPresent() ? profile.get().getName() : uuid.toString().substring(0, 8) + "...";
    }
}