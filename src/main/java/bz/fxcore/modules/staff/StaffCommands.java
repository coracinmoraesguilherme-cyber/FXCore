package bz.fxcore.modules.staff;

import bz.fxcore.core.database.FXPlayerData;
import bz.fxcore.core.database.PlayerDataManager;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.players.UserBanListEntry;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Date;
import java.util.List;
import java.util.UUID;

public class StaffCommands {

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")
            .withZone(ZoneId.systemDefault());

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("fxs")
            .requires(source -> source.hasPermission(2)) // Nível 2 (OP/Staff)

            // /fxs ban <player> <motivo> [<tempo>]
            .then(Commands.literal("ban")
                .then(Commands.argument("player", StringArgumentType.word())
                    .suggests((ctx, builder) -> SharedSuggestionProvider.suggest(ctx.getSource().getServer().getPlayerNames(), builder))
                    .then(Commands.argument("motivo", StringArgumentType.string())
                        .executes(ctx -> executeBan(ctx.getSource(), StringArgumentType.getString(ctx, "player"), StringArgumentType.getString(ctx, "motivo"), "Permanente"))
                        .then(Commands.argument("tempo", StringArgumentType.word())
                            .executes(ctx -> executeBan(ctx.getSource(), StringArgumentType.getString(ctx, "player"), StringArgumentType.getString(ctx, "motivo"), StringArgumentType.getString(ctx, "tempo")))))))

            // /fxs banlist
            .then(Commands.literal("banlist")
                .executes(ctx -> executeBanList(ctx.getSource())))

            // /fxs unban <player_name>
            .then(Commands.literal("unban")
                .then(Commands.argument("player_name", StringArgumentType.word())
                    .executes(ctx -> executeUnban(ctx.getSource(), StringArgumentType.getString(ctx, "player_name")))))

            // /fxs kick <player> <motivo>
            .then(Commands.literal("kick")
                .then(Commands.argument("player", StringArgumentType.word())
                    .suggests((ctx, builder) -> SharedSuggestionProvider.suggest(ctx.getSource().getServer().getPlayerNames(), builder))
                    .then(Commands.argument("motivo", StringArgumentType.greedyString())
                        .executes(ctx -> executeKick(ctx.getSource(), StringArgumentType.getString(ctx, "player"), StringArgumentType.getString(ctx, "motivo"))))))

            // /fxs mute <player> <motivo> <tempo>
            .then(Commands.literal("mute")
                .then(Commands.argument("player", StringArgumentType.word())
                    .suggests((ctx, builder) -> SharedSuggestionProvider.suggest(ctx.getSource().getServer().getPlayerNames(), builder))
                    .then(Commands.argument("motivo", StringArgumentType.string())
                        .then(Commands.argument("tempo", StringArgumentType.word())
                            .executes(ctx -> executeMute(ctx.getSource(), StringArgumentType.getString(ctx, "player"), StringArgumentType.getString(ctx, "motivo"), StringArgumentType.getString(ctx, "tempo")))))))

            // /fxs unmute <player>
            .then(Commands.literal("unmute")
                .then(Commands.argument("player", StringArgumentType.word())
                    .suggests((ctx, builder) -> SharedSuggestionProvider.suggest(ctx.getSource().getServer().getPlayerNames(), builder))
                    .executes(ctx -> executeUnmute(ctx.getSource(), StringArgumentType.getString(ctx, "player")))))

            // /fxs alt <player>
            .then(Commands.literal("alt")
                .then(Commands.argument("player", StringArgumentType.word())
                    .suggests((ctx, builder) -> SharedSuggestionProvider.suggest(ctx.getSource().getServer().getPlayerNames(), builder))
                    .executes(ctx -> executeAlt(ctx.getSource(), StringArgumentType.getString(ctx, "player")))))

            // /fxs history <player>
            .then(Commands.literal("history")
                .then(Commands.argument("player", StringArgumentType.word())
                    .suggests((ctx, builder) -> SharedSuggestionProvider.suggest(ctx.getSource().getServer().getPlayerNames(), builder))
                    .executes(ctx -> executeHistory(ctx.getSource(), StringArgumentType.getString(ctx, "player")))))

            // /fxs note add/remove
            .then(Commands.literal("note")
                .then(Commands.literal("add")
                    .then(Commands.argument("player", StringArgumentType.word())
                        .suggests((ctx, builder) -> SharedSuggestionProvider.suggest(ctx.getSource().getServer().getPlayerNames(), builder))
                        .then(Commands.argument("texto", StringArgumentType.greedyString())
                            .executes(ctx -> executeNoteAdd(ctx.getSource(), StringArgumentType.getString(ctx, "player"), StringArgumentType.getString(ctx, "texto"))))))
                .then(Commands.literal("remove")
                    .then(Commands.argument("player", StringArgumentType.word())
                        .suggests((ctx, builder) -> SharedSuggestionProvider.suggest(ctx.getSource().getServer().getPlayerNames(), builder))
                        .then(Commands.argument("id", IntegerArgumentType.integer(1))
                            .executes(ctx -> executeNoteRemove(ctx.getSource(), StringArgumentType.getString(ctx, "player"), IntegerArgumentType.getInteger(ctx, "id")))))))

            // /fxs info <player>
            .then(Commands.literal("info")
                .then(Commands.argument("player", StringArgumentType.word())
                    .suggests((ctx, builder) -> SharedSuggestionProvider.suggest(ctx.getSource().getServer().getPlayerNames(), builder))
                    .executes(ctx -> executeInfo(ctx.getSource(), StringArgumentType.getString(ctx, "player")))))

            // Subcomando SPY
            .then(Commands.literal("spy")
               .executes(context -> {
                ServerPlayer player = context.getSource().getPlayerOrException();
                FXPlayerData data = PlayerDataManager.get(player.getUUID());

                data.spyEnabled = !data.spyEnabled;

                if (data.spyEnabled) {
                    player.sendSystemMessage(Component.literal("§a[FXStaff] Modo Spy ATIVADO."));
                } else {
                    player.sendSystemMessage(Component.literal("§c[FXStaff] Modo Spy DESATIVADO."));
                }

                return 1;
            }))

            // /fxs freeze <player>
            .then(Commands.literal("freeze")
                .then(Commands.argument("player", StringArgumentType.word())
                    .suggests((ctx, builder) -> SharedSuggestionProvider.suggest(ctx.getSource().getServer().getPlayerNames(), builder))
                    .executes(ctx -> executeFreeze(ctx.getSource(), StringArgumentType.getString(ctx, "player")))))

            // /fxs unfreeze <player>
            .then(Commands.literal("unfreeze")
                .then(Commands.argument("player", StringArgumentType.word())
                    .suggests((ctx, builder) -> SharedSuggestionProvider.suggest(ctx.getSource().getServer().getPlayerNames(), builder))
                    .executes(ctx -> executeUnfreeze(ctx.getSource(), StringArgumentType.getString(ctx, "player")))))
        );
    }

    private static int executeInfo(CommandSourceStack source, String targetName) {
        MinecraftServer server = source.getServer();
        UUID uuid = PlayerDataManager.getUUIDByName(server, targetName);

        if (uuid == null) {
            source.sendFailure(Component.literal("§c[FXCore] O jogador '" + targetName + "' nunca entrou no servidor."));
            return 0;
        }

        FXPlayerData data = PlayerDataManager.get(uuid);
        ServerPlayer target = server.getPlayerList().getPlayerByName(targetName);

        source.sendSuccess(() -> Component.literal("§8§m--------------------------------------------------"), false);
        source.sendSuccess(() -> Component.literal("§e§lINFORMAÇÕES DO JOGADOR: §f" + targetName), false);
        
        if (target != null) {
            String pos = String.format("X: %d | Y: %d | Z: %d", target.getBlockX(), target.getBlockY(), target.getBlockZ());
            source.sendSuccess(() -> Component.literal("§7 Status (Online): §aVida: §f" + String.format("%.1f", target.getHealth()) + "/" + (int)target.getMaxHealth() + " ❤ §7| §bArmadura: §f" + target.getArmorValue()), false);
            source.sendSuccess(() -> Component.literal("§7 Posição: §e" + pos), false);
            source.sendSuccess(() -> Component.literal("§7 Conexão: §aPing: §f" + target.connection.latency() + "ms"), false);
        } else {
            source.sendSuccess(() -> Component.literal("§7 Status: §c[OFFLINE]"), false);
        }

        String lastConn = data.lastConnectionTimestamp > 0 
            ? DATE_FORMATTER.format(Instant.ofEpochMilli(data.lastConnectionTimestamp)) 
            : "Primeiro acesso";

        source.sendSuccess(() -> Component.literal("§7 Último login: §f" + lastConn), false);
        source.sendSuccess(() -> Component.literal("§7 Giveback Ativo: " + (data.hasGivebackActive ? "§aSIM" : "§cNÃO")), false);
        source.sendSuccess(() -> Component.literal("§7 Anotações Ativas: §f" + data.notes.size() + " §8(Use /fxs history para ler)"), false);
        source.sendSuccess(() -> Component.literal("§8§m--------------------------------------------------"), false);

        return 1;
    }

    private static int executeHistory(CommandSourceStack source, String targetName) {
        MinecraftServer server = source.getServer();
        UUID uuid = PlayerDataManager.getUUIDByName(server, targetName);

        if (uuid == null) {
            source.sendFailure(Component.literal("§c[FXCore] O jogador '" + targetName + "' nunca entrou no servidor."));
            return 0;
        }

        FXPlayerData data = PlayerDataManager.get(uuid);

        source.sendSuccess(() -> Component.literal("§8§m--------------------------------------------------"), false);
        source.sendSuccess(() -> Component.literal("§c§lHISTÓRICO & REGISTROS: §f" + targetName), false);
        
        source.sendSuccess(() -> Component.literal("§e[Anotações Cadastradas]"), false);
        if (data.notes.isEmpty()) {
            source.sendSuccess(() -> Component.literal(" §7Nenhuma anotação registrada."), false);
        } else {
            for (FXPlayerData.NoteEntry note : data.notes) {
                source.sendSuccess(() -> Component.literal(" §8[#" + note.id + "] §f" + note.text + " §8(Autor: " + note.author + ")"), false);
            }
        }

        source.sendSuccess(() -> Component.literal("§c[Histórico de Punições]"), false);
        if (data.history.isEmpty()) {
            source.sendSuccess(() -> Component.literal(" §7Nenhuma punição registrada."), false);
        } else {
            for (FXPlayerData.HistoryEntry entry : data.history) {
                source.sendSuccess(() -> Component.literal(" §7- §c" + entry.type + " §7| Motivo: §f" + entry.reason + " §7| Autor: §f" + entry.author + " §7| Duração: §f" + entry.duration), false);
            }
        }
        source.sendSuccess(() -> Component.literal("§8§m--------------------------------------------------"), false);

        return 1;
    }

    private static int executeKick(CommandSourceStack source, String targetName, String reason) {
        ServerPlayer target = source.getServer().getPlayerList().getPlayerByName(targetName);
        if (target == null) {
            source.sendFailure(Component.literal("§c[FXCore] O jogador '" + targetName + "' precisa estar online para ser expulso."));
            return 0;
        }

        Component kickMsg = Component.literal("§c[FXCore] Você foi expulso do servidor.\n§7Motivo: " + reason);
        target.connection.disconnect(kickMsg);

        source.getServer().getPlayerList().broadcastSystemMessage(
            Component.literal("§c[ANÚNCIO STAFF] §f" + targetName + " §7foi expulso do servidor. Motivo: §f" + reason), 
            false
        );
        return 1;
    }

    private static int executeFreeze(CommandSourceStack source, String targetName) {
        ServerPlayer target = source.getServer().getPlayerList().getPlayerByName(targetName);
        if (target == null) {
            source.sendFailure(Component.literal("§c[FXCore] O jogador precisa estar online para ser congelado."));
            return 0;
        }

        FXPlayerData data = PlayerDataManager.get(target.getUUID());
        data.isFrozen = true;
        
        target.sendSystemMessage(Component.literal("§c§l[FXCore] VOCÊ FOI CONGELADO POR UM ADMINISTRADOR!\n§7Não desconecte do servidor."));
        source.sendSuccess(() -> Component.literal("§a[FXCore] Jogador §f" + targetName + " §afoi congelado com sucesso."), false);
        return 1;
    }

    private static int executeUnfreeze(CommandSourceStack source, String targetName) {
        ServerPlayer target = source.getServer().getPlayerList().getPlayerByName(targetName);
        if (target == null) {
            source.sendFailure(Component.literal("§c[FXCore] O jogador precisa estar online para ser descongelado."));
            return 0;
        }

        FXPlayerData data = PlayerDataManager.get(target.getUUID());
        data.isFrozen = false;

        target.sendSystemMessage(Component.literal("§a[FXCore] Você foi descongelado."));
        source.sendSuccess(() -> Component.literal("§a[FXCore] Jogador §f" + targetName + " §afoi descongelado."), false);
        return 1;
    }

    private static int executeBanList(CommandSourceStack source) {
        source.sendSuccess(() -> Component.literal("§e[FXCore] §7Carregando lista de banimentos..."), false);
        bz.fxcore.core.otimi.server.FXTaskExecutor.runAsync(() -> {
            List<String> bannedPlayers = PlayerDataManager.getBannedList();
            source.getServer().execute(() -> {
                source.sendSuccess(() -> Component.literal("§c[FXCore] Lista de Banimentos Ativos:"), false);
                if (bannedPlayers.isEmpty()) {
                    source.sendSuccess(() -> Component.literal(" §7Nenhum jogador banido no momento."), false);
                } else {
                    for (String banInfo : bannedPlayers) {
                        source.sendSuccess(() -> Component.literal(" §8- §f" + banInfo), false);
                    }
                }
            });
        });
        return 1;
    }

    private static int executeUnban(CommandSourceStack source, String playerName) {
        source.sendSuccess(() -> Component.literal("§e[FXCore] §7Processando desbanimento de §f" + playerName + "§7..."), false);
        
        MinecraftServer server = source.getServer();
        
        // Remove da lista oficial de banidos do Minecraft para permitir a entrada imediata
        server.getProfileCache().get(playerName).ifPresent(profile -> {
            server.getPlayerList().getBans().remove(profile);
        });

        bz.fxcore.core.otimi.server.FXTaskExecutor.runAsync(() -> {
            boolean success = PlayerDataManager.unbanPlayer(playerName);
            server.execute(() -> {
                if (success) {
                    source.sendSuccess(() -> Component.literal("§a[FXCore] Jogador §f" + playerName + " §adesbanido com sucesso."), false);
                } else {
                    source.sendFailure(Component.literal("§c[FXCore] Jogador não encontrado na base de dados de banidos."));
                }
            });
        });
        return 1;
    }

    private static int executeAlt(CommandSourceStack source, String targetName) {
        MinecraftServer server = source.getServer();
        UUID uuid = PlayerDataManager.getUUIDByName(server, targetName);

        if (uuid == null) {
            source.sendFailure(Component.literal("§c[FXCore] O jogador '" + targetName + "' nunca entrou no servidor."));
            return 0;
        }

        FXPlayerData data = PlayerDataManager.get(uuid);
        String playerIp = null;
        ServerPlayer target = server.getPlayerList().getPlayerByName(targetName);
        if (target != null) {
            playerIp = target.getIpAddress();
        } else if (data.ipHistory != null && !data.ipHistory.isEmpty()) {
            playerIp = data.ipHistory.get(data.ipHistory.size() - 1);
        }

        if (playerIp == null) {
            source.sendFailure(Component.literal("§c[FXCore] Não foi possível encontrar o IP registrado para este jogador."));
            return 0;
        }

        final String finalIp = playerIp;
        source.sendSuccess(() -> Component.literal("§e[FXCore] §7Buscando contas vinculadas ao IP de §f" + targetName + "§7..."), false);
        bz.fxcore.core.otimi.server.FXTaskExecutor.runAsync(() -> {
            List<String> alts = PlayerDataManager.findAlts(finalIp);
            server.execute(() -> {
                source.sendSuccess(() -> Component.literal("§e[FXCore] §7Contas associadas ao IP §f" + finalIp + "§7:"), false);
                if (alts.isEmpty()) {
                    source.sendSuccess(() -> Component.literal(" §7Nenhuma outra conta encontrada."), false);
                } else {
                    for (String altName : alts) {
                        source.sendSuccess(() -> Component.literal(" §8- §f" + altName), false);
                    }
                }
            });
        });
        return 1;
    }

    private static int executeNoteAdd(CommandSourceStack source, String targetName, String text) {
        MinecraftServer server = source.getServer();
        UUID uuid = PlayerDataManager.getUUIDByName(server, targetName);

        if (uuid == null) {
            source.sendFailure(Component.literal("§c[FXCore] O jogador '" + targetName + "' nunca entrou no servidor."));
            return 0;
        }

        String author = source.getTextName();
        PlayerDataManager.addNote(uuid, author, text);
        source.sendSuccess(() -> Component.literal("§a[FXCore] Nota adicionada com sucesso para §f" + targetName), false);
        return 1;
    }

    private static int executeNoteRemove(CommandSourceStack source, String targetName, int noteId) {
        MinecraftServer server = source.getServer();
        UUID uuid = PlayerDataManager.getUUIDByName(server, targetName);

        if (uuid == null) {
            source.sendFailure(Component.literal("§c[FXCore] O jogador '" + targetName + "' nunca entrou no servidor."));
            return 0;
        }

        boolean removed = PlayerDataManager.removeNote(uuid, noteId);
        if (removed) {
            source.sendSuccess(() -> Component.literal("§a[FXCore] Nota #" + noteId + " removida."), false);
        } else {
            source.sendFailure(Component.literal("§c[FXCore] Nota #" + noteId + " não encontrada para este jogador."));
        }
        return 1;
    }

    private static int executeBan(CommandSourceStack source, String targetName, String reason, String duration) {
        MinecraftServer server = source.getServer();
        UUID uuid = PlayerDataManager.getUUIDByName(server, targetName);

        if (uuid == null) {
            source.sendFailure(Component.literal("§c[FXCore] O jogador '" + targetName + "' nunca entrou no servidor."));
            return 0;
        }

        long durationMillis = TimeUtil.parseTime(duration);
        long expireTimestamp = durationMillis <= 0 ? 0L : System.currentTimeMillis() + durationMillis;
        
        // 1. Salva no banco de dados do plugin
        PlayerDataManager.banPlayer(uuid, source.getTextName(), reason, duration, expireTimestamp);

        // 2. Adiciona nativamente à lista de banimentos do Minecraft (Bloqueia a entrada no pre-login)
        server.getProfileCache().get(targetName).ifPresent(profile -> {
            UserBanListEntry banEntry = new UserBanListEntry(
                profile, 
                new Date(), 
                source.getTextName(), 
                durationMillis <= 0 ? null : new Date(expireTimestamp), 
                reason
            );
            server.getPlayerList().getBans().add(banEntry);
        });

        // 3. Se estiver online, derruba o jogador imediatamente
        ServerPlayer target = server.getPlayerList().getPlayerByName(targetName);
        if (target != null) {
            target.connection.disconnect(Component.literal("§c[FXCore] Você foi banido!\n§7Motivo: " + reason + "\n§7Duração: " + duration));
        }

        source.sendSuccess(() -> Component.literal("§a[FXCore] Punição de banimento aplicada a §f" + targetName + "§a."), false);
        return 1;
    }

    private static int executeMute(CommandSourceStack source, String targetName, String reason, String duration) {
        MinecraftServer server = source.getServer();
        UUID uuid = PlayerDataManager.getUUIDByName(server, targetName);

        if (uuid == null) {
            source.sendFailure(Component.literal("§c[FXCore] O jogador '" + targetName + "' nunca entrou no servidor."));
            return 0;
        }

        long timeMillis = TimeUtil.parseTime(duration);
        long expireTimestamp = timeMillis <= 0 ? 0L : System.currentTimeMillis() + timeMillis;

        PlayerDataManager.mutePlayer(uuid, source.getTextName(), reason, duration, expireTimestamp);

        String durationFormatted = timeMillis <= 0 ? "Permanente" : TimeUtil.formatTime(timeMillis);
        
        ServerPlayer target = server.getPlayerList().getPlayerByName(targetName);
        if (target != null) {
            target.sendSystemMessage(Component.literal("§c[FXCore] Você foi mutado por §f" + durationFormatted + "§c. Motivo: §f" + reason));
        }

        source.sendSuccess(() -> Component.literal("§a[FXCore] Jogador §f" + targetName + " §afoi mutado por §f" + durationFormatted + "§a."), false);
        return 1;
    }

    private static int executeUnmute(CommandSourceStack source, String targetName) {
        MinecraftServer server = source.getServer();
        UUID uuid = PlayerDataManager.getUUIDByName(server, targetName);

        if (uuid == null) {
            source.sendFailure(Component.literal("§c[FXCore] O jogador '" + targetName + "' nunca entrou no servidor."));
            return 0;
        }

        boolean unmuted = PlayerDataManager.unmutePlayer(uuid);
        if (unmuted) {
            ServerPlayer target = server.getPlayerList().getPlayerByName(targetName);
            if (target != null) {
                target.sendSystemMessage(Component.literal("§a[FXCore] Você foi desmutado por um administrador."));
            }
            source.sendSuccess(() -> Component.literal("§a[FXCore] Jogador §f" + targetName + " §afoi desmutado com sucesso."), false);
        } else {
            source.sendFailure(Component.literal("§c[FXCore] Este jogador não está mutado."));
        }
        return 1;
    }
}