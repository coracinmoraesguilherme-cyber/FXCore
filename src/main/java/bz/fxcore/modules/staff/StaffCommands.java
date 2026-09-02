package bz.fxcore.modules.staff;

import bz.fxcore.core.database.FXPlayerData;
import bz.fxcore.core.database.PlayerDataManager;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Collection;
import java.util.List;

public class StaffCommands {

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")
            .withZone(ZoneId.systemDefault());

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("fxs")
            .requires(source -> source.hasPermission(2)) // Nível 2 (OP/Staff)

            // /fxs ban <player> <motivo> [<tempo>]
            .then(Commands.literal("ban")
                .then(Commands.argument("player", EntityArgument.players())
                    .then(Commands.argument("motivo", StringArgumentType.string())
                        .executes(ctx -> executeBan(ctx.getSource(), EntityArgument.getPlayers(ctx, "player"), StringArgumentType.getString(ctx, "motivo"), "Permanente"))
                        .then(Commands.argument("tempo", StringArgumentType.word())
                            .executes(ctx -> executeBan(ctx.getSource(), EntityArgument.getPlayers(ctx, "player"), StringArgumentType.getString(ctx, "motivo"), StringArgumentType.getString(ctx, "tempo")))))))

            // /fxs banlist
            .then(Commands.literal("banlist")
                .executes(ctx -> executeBanList(ctx.getSource())))

            // /fxs unban <player_name>
            .then(Commands.literal("unban")
                .then(Commands.argument("player_name", StringArgumentType.word())
                    .executes(ctx -> executeUnban(ctx.getSource(), StringArgumentType.getString(ctx, "player_name")))))

            // /fxs kick <player> <motivo>
            .then(Commands.literal("kick")
                .then(Commands.argument("player", EntityArgument.player())
                    .then(Commands.argument("motivo", StringArgumentType.greedyString())
                        .executes(ctx -> executeKick(ctx.getSource(), EntityArgument.getPlayer(ctx, "player"), StringArgumentType.getString(ctx, "motivo"))))))

            // /fxs mute <player> <motivo> <tempo>
            .then(Commands.literal("mute")
                .then(Commands.argument("player", EntityArgument.player())
                    .then(Commands.argument("motivo", StringArgumentType.string())
                        .then(Commands.argument("tempo", StringArgumentType.word())
                            .executes(ctx -> executeMute(ctx.getSource(), EntityArgument.getPlayer(ctx, "player"), StringArgumentType.getString(ctx, "motivo"), StringArgumentType.getString(ctx, "tempo")))))))

            // /fxs alt <player>
            .then(Commands.literal("alt")
                .then(Commands.argument("player", EntityArgument.player())
                    .executes(ctx -> executeAlt(ctx.getSource(), EntityArgument.getPlayer(ctx, "player")))))

            // /fxs history <player>
            .then(Commands.literal("history")
                .then(Commands.argument("player", EntityArgument.player())
                    .executes(ctx -> executeHistory(ctx.getSource(), EntityArgument.getPlayer(ctx, "player")))))

            // /fxs note add/remove
            .then(Commands.literal("note")
                .then(Commands.literal("add")
                    .then(Commands.argument("player", EntityArgument.player())
                        .then(Commands.argument("texto", StringArgumentType.greedyString())
                            .executes(ctx -> executeNoteAdd(ctx.getSource(), EntityArgument.getPlayer(ctx, "player"), StringArgumentType.getString(ctx, "texto"))))))
                .then(Commands.literal("remove")
                    .then(Commands.argument("player", EntityArgument.player())
                        .then(Commands.argument("id", IntegerArgumentType.integer(1))
                            .executes(ctx -> executeNoteRemove(ctx.getSource(), EntityArgument.getPlayer(ctx, "player"), IntegerArgumentType.getInteger(ctx, "id")))))))

            // /fxs info <player>
            .then(Commands.literal("info")
                .then(Commands.argument("player", EntityArgument.player())
                    .executes(ctx -> executeInfo(ctx.getSource(), EntityArgument.getPlayer(ctx, "player")))))

            // Subcomando SPY
            .then(Commands.literal("spy")
               .executes(context -> {
                ServerPlayer player = context.getSource().getPlayerOrException();
                FXPlayerData data = PlayerDataManager.get(player.getUUID());

                // Alterna o estado do spy
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
                .then(Commands.argument("player", EntityArgument.player())
                    .executes(ctx -> executeFreeze(ctx.getSource(), EntityArgument.getPlayer(ctx, "player")))))

            // /fxs unfreeze <player>
            .then(Commands.literal("unfreeze")
                .then(Commands.argument("player", EntityArgument.player())
                    .executes(ctx -> executeUnfreeze(ctx.getSource(), EntityArgument.getPlayer(ctx, "player")))))
        );
    }

    // /fxs info <player>
    private static int executeInfo(CommandSourceStack source, ServerPlayer target) {
        FXPlayerData data = PlayerDataManager.get(target.getUUID());

        String pos = String.format("X: %d | Y: %d | Z: %d", target.getBlockX(), target.getBlockY(), target.getBlockZ());
        String lastConn = data.lastConnectionTimestamp > 0 
            ? DATE_FORMATTER.format(Instant.ofEpochMilli(data.lastConnectionTimestamp)) 
            : "Primeiro acesso";

        source.sendSuccess(() -> Component.literal("§8§m--------------------------------------------------"), false);
        source.sendSuccess(() -> Component.literal("§e§lINFORMAÇÕES EM TEMPO REAL: §f" + target.getName().getString()), false);
        source.sendSuccess(() -> Component.literal("§7 Status: §aVida: §f" + String.format("%.1f", target.getHealth()) + "/" + (int)target.getMaxHealth() + " ❤ §7| §bArmadura: §f" + target.getArmorValue() + " §7| §aXP Level: §f" + target.experienceLevel), false);
        source.sendSuccess(() -> Component.literal("§7 Posição: §e" + pos + " §7(§f" + target.level().dimension().location().getPath() + "§7)"), false);
        source.sendSuccess(() -> Component.literal("§7 Conexão: §aPing: §f" + target.connection.latency() + "ms §7| §aÚltimo login: §f" + lastConn), false);
        source.sendSuccess(() -> Component.literal("§7 Giveback Ativo: " + (data.hasGivebackActive ? "§aSIM" : "§cNÃO")), false);
        source.sendSuccess(() -> Component.literal("§7 Anotações Ativas: §f" + data.notes.size() + " §8(Use /fxs history para ler)"), false);
        source.sendSuccess(() -> Component.literal("§8§m--------------------------------------------------"), false);

        return 1;
    }

    // /fxs history <player>
    private static int executeHistory(CommandSourceStack source, ServerPlayer target) {
        FXPlayerData data = PlayerDataManager.get(target.getUUID());

        source.sendSuccess(() -> Component.literal("§8§m--------------------------------------------------"), false);
        source.sendSuccess(() -> Component.literal("§c§lHISTÓRICO & REGISTROS: §f" + target.getName().getString()), false);
        
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

    // /fxs kick <player> <motivo>
    private static int executeKick(CommandSourceStack source, ServerPlayer target, String reason) {
        Component kickMsg = Component.literal("§c[FXCore] Você foi expulso do servidor.\n§7Motivo: " + reason);
        target.connection.disconnect(kickMsg);

        // Anúncio global
        source.getServer().getPlayerList().broadcastSystemMessage(
            Component.literal("§c[ANÚNCIO STAFF] §f" + target.getName().getString() + " §7foi expulso do servidor. Motivo: §f" + reason), 
            false
        );
        return 1;
    }

    // /fxs freeze <player>
    private static int executeFreeze(CommandSourceStack source, ServerPlayer target) {
        FXPlayerData data = PlayerDataManager.get(target.getUUID());
        data.isFrozen = true;
        
        target.sendSystemMessage(Component.literal("§c§l[FXCore] VOCÊ FOI CONGELADO POR UM ADMINISTRADOR!\n§7Não desconecte do servidor."));
        source.sendSuccess(() -> Component.literal("§a[FXCore] Jogador §f" + target.getName().getString() + " §afoi congelado com sucesso."), false);
        return 1;
    }

    // /fxs unfreeze <player>
    private static int executeUnfreeze(CommandSourceStack source, ServerPlayer target) {
        FXPlayerData data = PlayerDataManager.get(target.getUUID());
        data.isFrozen = false;

        target.sendSystemMessage(Component.literal("§a[FXCore] Você foi descongelado."));
        source.sendSuccess(() -> Component.literal("§a[FXCore] Jogador §f" + target.getName().getString() + " §afoi descongelado."), false);
        return 1;
    }

    // /fxs spy
    private static int executeSpy(CommandSourceStack source) {
        if (source.getEntity() instanceof ServerPlayer player) {
            FXPlayerData data = PlayerDataManager.get(player.getUUID());
            data.spyEnabled = !data.spyEnabled;
            
            source.sendSuccess(() -> Component.literal("§a[FXCore] Modo Spy (Tells/Teams) " + (data.spyEnabled ? "§aATIVADO" : "§cDESATIVADO")), false);
        }
        return 1;
    }

    // /fxs banlist
    private static int executeBanList(CommandSourceStack source) {
        List<String> bannedPlayers = PlayerDataManager.getBannedList();
        
        source.sendSuccess(() -> Component.literal("§c[FXCore] Lista de Banimentos Ativos:"), false);
        if (bannedPlayers.isEmpty()) {
            source.sendSuccess(() -> Component.literal(" §7Nenhum jogador banido no momento."), false);
        } else {
            for (String banInfo : bannedPlayers) {
                source.sendSuccess(() -> Component.literal(" §8- §f" + banInfo), false);
            }
        }
        return 1;
    }

    private static int executeUnban(CommandSourceStack source, String playerName) {
        boolean success = PlayerDataManager.unbanPlayer(playerName);
        if (success) {
            source.sendSuccess(() -> Component.literal("§a[FXCore] Jogador §f" + playerName + " §adesbanido com sucesso."), false);
        } else {
            source.sendFailure(Component.literal("§c[FXCore] Jogador não encontrado na lista de banidos."));
        }
        return 1;
    }

    private static int executeAlt(CommandSourceStack source, ServerPlayer target) {
        String playerIp = target.getIpAddress();
        List<String> alts = PlayerDataManager.findAlts(playerIp);

        source.sendSuccess(() -> Component.literal("§e[FXCore] §7Contas associadas ao IP §f" + playerIp + "§7:"), false);
        if (alts.isEmpty()) {
            source.sendSuccess(() -> Component.literal("§7Nenhuma outra conta encontrada."), false);
        } else {
            for (String altName : alts) {
                source.sendSuccess(() -> Component.literal("§8- §f" + altName), false);
            }
        }
        return 1;
    }

    private static int executeNoteAdd(CommandSourceStack source, ServerPlayer target, String text) {
        String author = source.getTextName();
        PlayerDataManager.addNote(target.getUUID(), author, text);
        source.sendSuccess(() -> Component.literal("§a[FXCore] Nota adicionada com sucesso para §f" + target.getName().getString()), false);
        return 1;
    }

    private static int executeNoteRemove(CommandSourceStack source, ServerPlayer target, int noteId) {
        boolean removed = PlayerDataManager.removeNote(target.getUUID(), noteId);
        if (removed) {
            source.sendSuccess(() -> Component.literal("§a[FXCore] Nota #" + noteId + " removida."), false);
        } else {
            source.sendFailure(Component.literal("§c[FXCore] Nota #" + noteId + " não encontrada para este jogador."));
        }
        return 1;
    }

    private static int executeBan(CommandSourceStack source, Collection<ServerPlayer> players, String reason, String duration) {
        for (ServerPlayer player : players) {
            PlayerDataManager.banPlayer(player.getUUID(), source.getTextName(), reason, duration);
            player.connection.disconnect(Component.literal("§c[FXCore] Você foi banido!\n§7Motivo: " + reason + "\n§7Duração: " + duration));
        }
        source.sendSuccess(() -> Component.literal("§a[FXCore] Punição de banimento aplicada."), false);
        return 1;
    }

    private static int executeMute(CommandSourceStack source, ServerPlayer target, String reason, String duration) {
    long timeMillis = TimeUtil.parseTime(duration);
    
    FXStaffData.mute(target.getUUID(), timeMillis);
    PlayerDataManager.mutePlayer(target.getUUID(), source.getTextName(), reason, duration);

    String durationFormatted = timeMillis <= 0 ? "Permanente" : TimeUtil.formatTime(timeMillis);
    
    target.sendSystemMessage(Component.literal("§c[FXCore] Você foi mutado por §f" + durationFormatted + "§c. Motivo: §f" + reason));
    source.sendSuccess(() -> Component.literal("§a[FXCore] Jogador §f" + target.getName().getString() + " §afoi mutado por §f" + durationFormatted + "§a."), false);
    
    return 1;
    }
}