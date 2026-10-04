package bz.fxcore.fxchat;

<<<<<<< Updated upstream:src/main/java/bz/fxcore/fxchat/ChatCommand.java
import bz.fxcore.fxchat.ChannelManager;
import bz.fxcore.fxchat.ChatChannel;
import bz.fxcore.fxchat.ModConfig;
import bz.fxcore.fxteam.TeamConfig;
import bz.fxcore.fxteam.TeamManager;
=======
import bz.fxcore.core.database.FXPlayerData;
import bz.fxcore.core.database.PlayerDataManager;
import bz.fxcore.modules.team.TeamCommands;
import bz.fxcore.modules.team.TeamData;
import bz.fxcore.modules.team.TeamManager;
>>>>>>> Stashed changes:src/main/java/bz/fxcore/modules/chat/ChatCommand.java
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.DoubleArgumentType;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.scores.PlayerTeam;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class ChatCommand {

<<<<<<< Updated upstream:src/main/java/bz/fxcore/fxchat/ChatCommand.java
    public static final Map<UUID, String> CANAL_ATUAL_JOGADOR = new HashMap<>();
    public static final Map<UUID, UUID> ULTIMA_CONVERSA = new HashMap<>();
    public static final Set<UUID> SPY_ATIVOS = new HashSet<>();
=======
    public static final Map<UUID, String> CANAL_ATUAL_JOGADOR = new ConcurrentHashMap<>();
    public static final Map<UUID, UUID> ULTIMA_CONVERSA = new ConcurrentHashMap<>();
>>>>>>> Stashed changes:src/main/java/bz/fxcore/modules/chat/ChatCommand.java

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {

        // 1. Remove comandos /msg, /tell e /w padrão do Minecraft Vanilla para evitar conflitos
        dispatcher.getRoot().getChildren().removeIf(node -> 
            node.getName().equalsIgnoreCase("msg") || 
            node.getName().equalsIgnoreCase("tell") || 
            node.getName().equalsIgnoreCase("w")
        );

        // 2. Registra /msg, /tell e /w explicitamente com a mesma lógica
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

        // Registra os canais dinâmicos carregados (ex: /g, /l, /tc, /s)
        for (ChatChannel canal : ChannelManager.CHANNELS.values()) {
            registrarCanalUnico(dispatcher, canal);
        }

        // --- COMANDO /R ---
        dispatcher.register(
            Commands.literal("r")
                .then(Commands.argument("mensagem", StringArgumentType.greedyString())
                    .executes(context -> {
                        ServerPlayer remetente = context.getSource().getPlayerOrException();
                        UUID ultimoUuid = ULTIMA_CONVERSA.get(remetente.getUUID());

                        if (ultimoUuid == null) {
                            remetente.sendSystemMessage(Component.literal(ModConfig.MESSAGES.semQuemResponder));
                            return 1;
                        }

                        ServerPlayer destinatario = remetente.server.getPlayerList().getPlayer(ultimoUuid);
                        if (destinatario == null) {
                            remetente.sendSystemMessage(Component.literal(ModConfig.MESSAGES.destinatarioOffline));
                            return 1;
                        }

                        String texto = StringArgumentType.getString(context, "mensagem");
                        enviarMensagemPrivada(remetente, destinatario, texto);
                        return 1;
                    }))
        );

        // --- COMANDO /FXCHAT ---
        dispatcher.register(
            Commands.literal("fxchat")
                .requires(source -> source.hasPermission(2))

                .then(Commands.literal("reload")
                    .executes(context -> {
                        ChannelManager.loadChannels();
                        context.getSource().sendSuccess(() -> Component.literal(ModConfig.MESSAGES.reloadSucesso), true);
                        return 1;
                    }))

                .then(Commands.literal("spy")
                    .executes(context -> {
                        ServerPlayer player = context.getSource().getPlayerOrException();
                        if (SPY_ATIVOS.contains(player.getUUID())) {
                            SPY_ATIVOS.remove(player.getUUID());
                            player.sendSystemMessage(Component.literal(ModConfig.MESSAGES.spyDesativado));
                        } else {
                            SPY_ATIVOS.add(player.getUUID());
                            player.sendSystemMessage(Component.literal(ModConfig.MESSAGES.spyAtivado));
                        }
                        return 1;
                    }))

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
                                            ChatChannel novoCanal = new ChatChannel(nome, cmd, formatoPadrao, raio, perm);

                                            ChannelManager.salvarCanal(novoCanal);

                                            CommandDispatcher<CommandSourceStack> serverDispatcher = context.getSource().getServer().getCommands().getDispatcher();
                                            registrarCanalUnico(serverDispatcher, novoCanal);

                                            String msgCriado = ModConfig.MESSAGES.canalCriado
                                                    .replace("{nome}", nome)
                                                    .replace("{comando}", cmd);
                                            context.getSource().sendSuccess(() -> Component.literal(msgCriado), true);
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

                                    String msgRemovido = ModConfig.MESSAGES.canalRemovido.replace("{comando}", cmd);
                                    context.getSource().sendSuccess(() -> Component.literal(msgRemovido), true);

                                    if (context.getSource().getServer() != null) {
                                        context.getSource().getServer().getPlayerList().getPlayers().forEach(p ->
                                            context.getSource().getServer().getCommands().sendCommands(p)
                                        );
                                    }
                                } else {
                                    context.getSource().sendFailure(Component.literal(ModConfig.MESSAGES.canalNaoEncontrado));
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

                String msgEntrou = ModConfig.MESSAGES.canalEntrou.replace("{canal}", canal.getName());
                player.sendSystemMessage(Component.literal(msgEntrou));
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

<<<<<<< Updated upstream:src/main/java/bz/fxcore/fxchat/ChatCommand.java
        String txtRemetente = ModConfig.MESSAGES.formatoTellRemetente
                .replace("{destinatario}", destinatario.getScoreboardName())
                .replace("{msg}", msgColorida);

        String txtDestinatario = ModConfig.MESSAGES.formatoTellDestinatario
                .replace("{remetente}", remetente.getScoreboardName())
                .replace("{msg}", msgColorida);
=======
        String txtRemetente = "§7[§6Eu §8-> §f" + destinatario.getScoreboardName() + "§7] §f" + msgColorida;
        String txtDestinatario = "§7[§f" + remetente.getScoreboardName() + " §8-> §6Eu§7] §f" + msgColorida;
>>>>>>> Stashed changes:src/main/java/bz/fxcore/modules/chat/ChatCommand.java

        remetente.sendSystemMessage(Component.literal(txtRemetente));
        destinatario.sendSystemMessage(Component.literal(txtDestinatario));

        ULTIMA_CONVERSA.put(destinatario.getUUID(), remetente.getUUID());
        ULTIMA_CONVERSA.put(remetente.getUUID(), destinatario.getUUID());

        enviarParaSpies(remetente, destinatario, msgColorida);
    }

    public static void enviarParaSpies(ServerPlayer remetente, ServerPlayer destinatario, String texto) {
<<<<<<< Updated upstream:src/main/java/bz/fxcore/fxchat/ChatCommand.java
        String txtSpy = ModConfig.MESSAGES.formatoSpy
                .replace("{remetente}", remetente.getScoreboardName())
                .replace("{destinatario}", destinatario.getScoreboardName())
                .replace("{msg}", texto);

        Component msgSpy = Component.literal(txtSpy);

        for (UUID spyUuid : SPY_ATIVOS) {
            ServerPlayer admin = remetente.server.getPlayerList().getPlayer(spyUuid);
            if (admin != null && !admin.getUUID().equals(remetente.getUUID()) && !admin.getUUID().equals(destinatario.getUUID())) {
=======
        String txtSpy = "§e[SPY] §7" + remetente.getScoreboardName() + " -> " + destinatario.getScoreboardName() + ": §f" + texto;
        Component msgSpy = Component.literal(txtSpy);

        for (ServerPlayer admin : remetente.server.getPlayerList().getPlayers()) {
            if (admin.getUUID().equals(remetente.getUUID()) || admin.getUUID().equals(destinatario.getUUID())) {
                continue;
            }

            FXPlayerData adminData = PlayerDataManager.get(admin.getUUID());
            if (adminData != null && adminData.spyEnabled) {
>>>>>>> Stashed changes:src/main/java/bz/fxcore/modules/chat/ChatCommand.java
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
        if (canal == null) {
            canal = ChannelManager.CHANNELS.values().stream().findFirst().orElse(null);
        }
        if (canal == null) {
            canal = new ChatChannel("Local", "l", "§e[L] {prefix}§f{player}§8: §f{msg}", 100.0, 0, 0.0);
        }

        PlayerTeam timeJogador = player.getTeam();

        String prefixo = "";
        if (timeJogador != null) {
            prefixo = timeJogador.getPlayerPrefix().getString();
        }

        String mensagemColorida = formatarCores(texto);

        String formatoFinal = canal.getFormat()
                .replace("{prefix}", prefixo)
                .replace("{player}", player.getScoreboardName())
                .replace("{msg}", mensagemColorida);

        Component componente = Component.literal(formatoFinal);

        // --- CANAL DE TIME (/tc) ---
        if (cmdLower.equals("tc")) {
            if (timeJogador == null) {
                player.sendSystemMessage(Component.literal(TeamConfig.MESSAGES.notInTeam));
                return;
            }

            String tagTexto = timeJogador.getPlayerPrefix().getString()
                    .replaceAll("§[0-9a-fk-orA-FK-OR]", "")
                    .replace("[", "")
                    .replace("]", "")
                    .trim();
            String tagColor = "§f";

            if (prefixo.contains("§") && prefixo.length() >= 5) {
                int idx = prefixo.indexOf("§", 3);
                if (idx != -1 && idx + 1 < prefixo.length()) {
                    tagColor = prefixo.substring(idx, idx + 2);
                }
            }

            String formatoChat = TeamConfig.MESSAGES.teamChatFormat
                    .replace("{TAG}", tagTexto)
                    .replace("{TAG_COLOR}", tagColor)
                    .replace("{PLAYER}", player.getScoreboardName())
                    .replace("{MESSAGE}", mensagemColorida);

            Component compTime = Component.literal(formatoChat);

            for (ServerPlayer destinatario : player.getServer().getPlayerList().getPlayers()) {
                PlayerTeam timeDestinatario = destinatario.getTeam();
                if (timeDestinatario != null && timeDestinatario.equals(timeJogador)) {
                    destinatario.sendSystemMessage(compTime);
                }
            }

            String formatoSpy = TeamConfig.MESSAGES.teamSpyFormat
                    .replace("{TAG}", tagTexto)
                    .replace("{TAG_COLOR}", tagColor)
                    .replace("{PLAYER}", player.getScoreboardName())
                    .replace("{MESSAGE}", mensagemColorida);

            Component msgSpy = Component.literal(formatoSpy);

            for (UUID spyUuid : TeamManager.TEAM_SPY_ATIVOS) {
                ServerPlayer admin = player.getServer().getPlayerList().getPlayer(spyUuid);
                if (admin != null && !admin.getUUID().equals(player.getUUID())) {
                    PlayerTeam adminTeam = admin.getTeam();
                    if (adminTeam == null || !adminTeam.equals(timeJogador)) {
                        admin.sendSystemMessage(msgSpy);
                    }
                }
            }
            return;
        }

        // --- CANAIS GLOBAIS / STAFF (Radius < 0) ---
        if (canal.getRadius() < 0) {
            for (ServerPlayer destinatario : player.getServer().getPlayerList().getPlayers()) {
                if (destinatario.hasPermissions(canal.getPermissionLevel())) {
                    destinatario.sendSystemMessage(componente);
                }
            }
        } 
        // --- CANAIS LOCAIS (POR RAIO) ---
        else {
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
<<<<<<< Updated upstream:src/main/java/bz/fxcore/fxchat/ChatCommand.java
=======
                } else {
                    FXPlayerData targetData = PlayerDataManager.get(destinatario.getUUID());
                    if (targetData != null && targetData.spyEnabled) {
                        Component spyMsg = Component.literal("§8[SPY-" + canal.getCommand().toUpperCase() + "] §7" + player.getScoreboardName() + ": §f" + mensagemColorida);
                        destinatario.sendSystemMessage(spyMsg);
                    }
>>>>>>> Stashed changes:src/main/java/bz/fxcore/modules/chat/ChatCommand.java
                }
            }

            if (ouvintesProximos == 0) {
                player.sendSystemMessage(Component.literal(ModConfig.MESSAGES.ninguemOuviu));
            }
        }
    }

    public static String formatarCores(String texto) {
        if (texto == null) return "";
        return texto.replaceAll("&([0-9a-fk-orA-FK-OR])", "§$1");
    }
}