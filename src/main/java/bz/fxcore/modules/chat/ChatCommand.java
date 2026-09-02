package bz.fxcore.modules.chat;

import bz.fxcore.modules.team.TeamCommands;
import bz.fxcore.modules.team.TeamData;
import bz.fxcore.modules.team.TeamManager;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.DoubleArgumentType;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public class ChatCommand {

    public static final Map<UUID, String> CANAL_ATUAL_JOGADOR = new HashMap<>();
    public static final Map<UUID, UUID> ULTIMA_CONVERSA = new HashMap<>();
    public static final Set<UUID> SPY_ATIVOS = new HashSet<>();

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {

        dispatcher.getRoot().getChildren().removeIf(node -> 
            node.getName().equalsIgnoreCase("msg") || 
            node.getName().equalsIgnoreCase("tell") || 
            node.getName().equalsIgnoreCase("w")
        );

        String[] comandosTell = {"msg", "tell", "w"};
        for (String cmd : comandosTell) {
            dispatcher.register(
                Commands.literal(cmd)
                    .then(Commands.argument("jogador", EntityArgument.player())
                        .then(Commands.argument("mensagem", StringArgumentType.greedyString())
                            .executes(context -> {
                                ServerPlayer remetente = context.getSource().getPlayerOrException();
                                ServerPlayer destinatario = EntityArgument.getPlayer(context, "jogador");
                                String texto = StringArgumentType.getString(context, "mensagem");

                                enviarMensagemPrivada(remetente, destinatario, texto);
                                return 1;
                            })))
            );
        }

        for (ChatChannel canal : ChannelManager.CHANNELS.values()) {
            registrarCanalUnico(dispatcher, canal);
        }

        dispatcher.register(
            Commands.literal("r")
                .then(Commands.argument("mensagem", StringArgumentType.greedyString())
                    .executes(context -> {
                        ServerPlayer remetente = context.getSource().getPlayerOrException();
                        UUID ultimoUuid = ULTIMA_CONVERSA.get(remetente.getUUID());

                        if (ultimoUuid == null) {
                            remetente.sendSystemMessage(Component.literal("§cVocê não tem ninguém para responder."));
                            return 1;
                        }

                        ServerPlayer destinatario = remetente.server.getPlayerList().getPlayer(ultimoUuid);
                        if (destinatario == null) {
                            remetente.sendSystemMessage(Component.literal("§cO jogador está offline."));
                            return 1;
                        }

                        String texto = StringArgumentType.getString(context, "mensagem");
                        enviarMensagemPrivada(remetente, destinatario, texto);
                        return 1;
                    }))
        );

        dispatcher.register(
            Commands.literal("fxchat")
                .requires(source -> source.hasPermission(2))

                .then(Commands.literal("slow")
                    .then(Commands.argument("comando", StringArgumentType.word())
                        .then(Commands.argument("segundos", DoubleArgumentType.doubleArg(0.0))
                            .executes(context -> {
                                String cmd = StringArgumentType.getString(context, "comando").toLowerCase();
                                double segundos = DoubleArgumentType.getDouble(context, "segundos");

                                ChatChannel canal = ChannelManager.CHANNELS.get(cmd);
                                if (canal != null) {
                                    canal.setSlow(segundos);
                                    ChannelManager.salvarCanal(canal);
                                    context.getSource().sendSuccess(() -> Component.literal("§a[FXChat] Slow do canal /" + cmd + " definido para " + segundos + "s."), true);
                                } else {
                                    context.getSource().sendFailure(Component.literal("§cCanal não encontrado."));
                                }
                                return 1;
                            }))))

                .then(Commands.literal("channel")
                    .then(Commands.literal("create")
                        .then(Commands.argument("nome", StringArgumentType.word())
                            .then(Commands.argument("comando", StringArgumentType.word())
                                .then(Commands.argument("raio", DoubleArgumentType.doubleArg(-1.0))
                                    .then(Commands.argument("permissao", IntegerArgumentType.integer(0, 4))
                                        .executes(context -> {
                                            String nome = StringArgumentType.getString(context, "nome");
                                            String cmd = StringArgumentType.getString(context, "comando").toLowerCase();
                                            double raio = DoubleArgumentType.getDouble(context, "raio");
                                            int perm = IntegerArgumentType.getInteger(context, "permissao");

                                            String formatoPadrao = "§7[" + nome + "] {prefix}§f{player}§8: §f{msg}";
                                            ChatChannel novoCanal = new ChatChannel(nome, cmd, formatoPadrao, raio, perm, 0.0);

                                            ChannelManager.salvarCanal(novoCanal);

                                            CommandDispatcher<CommandSourceStack> serverDispatcher = context.getSource().getServer().getCommands().getDispatcher();
                                            registrarCanalUnico(serverDispatcher, novoCanal);

                                            context.getSource().sendSuccess(() -> Component.literal("§a[FXChat] Canal /" + cmd + " criado com sucesso!"), true);
                                            return 1;
                                        }))))))
                    .then(Commands.literal("remove")
                        .then(Commands.argument("comando", StringArgumentType.word())
                            .executes(context -> {
                                String cmd = StringArgumentType.getString(context, "comando").toLowerCase();

                                if (ChannelManager.deletarCanal(cmd)) {
                                    ChannelManager.loadChannels();

                                    CommandDispatcher<CommandSourceStack> disp = context.getSource().getServer().getCommands().getDispatcher();
                                    if (disp.getRoot().getChild(cmd) != null) {
                                        disp.getRoot().getChildren().removeIf(node -> node.getName().equalsIgnoreCase(cmd));
                                    }

                                    context.getSource().sendSuccess(() -> Component.literal("§a[FXChat] Canal /" + cmd + " removido!"), true);
                                } else {
                                    context.getSource().sendFailure(Component.literal("§cCanal não encontrado."));
                                }
                                return 1;
                            }))))
        );
    }

    public static void registrarCanalUnico(CommandDispatcher<CommandSourceStack> dispatcher, ChatChannel canal) {
        String cmdName = canal.getCommand().startsWith("/") ? canal.getCommand().substring(1) : canal.getCommand();

        var cmdNode = Commands.literal(cmdName)
            .requires(source -> source.hasPermission(canal.getPermissionLevel()))
            .executes(context -> {
                ServerPlayer player = context.getSource().getPlayerOrException();
                CANAL_ATUAL_JOGADOR.put(player.getUUID(), cmdName);
                player.sendSystemMessage(Component.literal("§aVocê entrou no canal: " + canal.getName()));
                return 1;
            })
            .then(Commands.argument("mensagem", StringArgumentType.greedyString())
                .executes(context -> {
                    ServerPlayer player = context.getSource().getPlayerOrException();
                    String texto = StringArgumentType.getString(context, "mensagem");
                    enviarMensagemCanal(player, cmdName, texto);
                    return 1;
                }));

        dispatcher.register(cmdNode);
    }

    public static void enviarMensagemPrivada(ServerPlayer remetente, ServerPlayer destinatario, String texto) {
        String msgColorida = formatarCores(texto);

        String txtRemetente = "§7[§sEu §8-> §f" + destinatario.getScoreboardName() + "§7] §f" + msgColorida;
        String txtDestinatario = "§7[§f" + remetente.getScoreboardName() + " §8-> §sEu§7] §f" + msgColorida;

        remetente.sendSystemMessage(Component.literal(txtRemetente));
        destinatario.sendSystemMessage(Component.literal(txtDestinatario));

        ULTIMA_CONVERSA.put(destinatario.getUUID(), remetente.getUUID());
        ULTIMA_CONVERSA.put(remetente.getUUID(), destinatario.getUUID());

        enviarParaSpies(remetente, destinatario, msgColorida);
    }

    public static void enviarParaSpies(ServerPlayer remetente, ServerPlayer destinatario, String texto) {
    String txtSpy = "§e[SPY] §7" + remetente.getScoreboardName() + " -> " + destinatario.getScoreboardName() + ": §f" + texto;
    Component msgSpy = Component.literal(txtSpy);

    for (ServerPlayer admin : remetente.server.getPlayerList().getPlayers()) {
        // Ignora os próprios envolvidos na conversa
        if (admin.getUUID().equals(remetente.getUUID()) || admin.getUUID().equals(destinatario.getUUID())) {
            continue;
        }

        // Obtém os dados do admin para checar o spy
        bz.fxcore.core.database.FXPlayerData adminData = bz.fxcore.core.database.PlayerDataManager.get(admin.getUUID());
        if (adminData.spyEnabled) {
            admin.sendSystemMessage(msgSpy);
        }
    }
    }

    public static void enviarMensagemCanal(ServerPlayer player, String comandoCanal, String texto) {
        String cmdLower = comandoCanal.toLowerCase();
        ChatChannel canal = ChannelManager.CHANNELS.get(cmdLower);

        if (canal == null) {
            canal = ChannelManager.CHANNELS.get("l");
        }

        String tagTime = "";
        TeamData team = TeamManager.getPlayerTeam(player.getUUID());
        if (team != null) {
            tagTime = TeamCommands.parseColor(team.tagColor) + "[" + team.tag + "] ";
        }

        String mensagemColorida = formatarCores(texto);

        String formatoFinal = canal.getFormat()
                .replace("{prefix}", tagTime)
                .replace("{player}", player.getScoreboardName())
                .replace("{msg}", mensagemColorida);

        Component componente = Component.literal(formatoFinal);

        if (canal.getRadius() < 0) {
            for (ServerPlayer destinatario : player.getServer().getPlayerList().getPlayers()) {
                if (destinatario.hasPermissions(canal.getPermissionLevel())) {
                    destinatario.sendSystemMessage(componente);
                }
            }
        } else {
            double raioQuadrado = canal.getRadius() * canal.getRadius();
            int ouvintesProximos = 0;

            for (ServerPlayer proximo : player.serverLevel().players()) {
                if (proximo.distanceToSqr(player) <= raioQuadrado) {
                    if (proximo.hasPermissions(canal.getPermissionLevel())) {
                        proximo.sendSystemMessage(componente);
                        if (!proximo.getUUID().equals(player.getUUID())) {
                            ouvintesProximos++;
                        }
                    }
                }
            }

            if (ouvintesProximos == 0) {
                player.sendSystemMessage(Component.literal("§cNinguém ouviu você."));
            }
        }
    }

    public static String formatarCores(String texto) {
        if (texto == null) return "";
        return texto.replaceAll("&([0-9a-fk-orA-FK-OR])", "§$1");
    }
}