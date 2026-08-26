package bz.fxcore.fxstaff;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundSetSubtitleTextPacket;
import net.minecraft.network.protocol.game.ClientboundSetTitleTextPacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.scores.PlayerTeam;

import java.util.List;

public class FXStaffCommands {

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(
            Commands.literal("fx")
                .requires(source -> source.hasPermission(2))
                
                // --- /fx freeze <player> ---
                .then(Commands.literal("freeze")
                    .then(Commands.argument("target", EntityArgument.player())
                        .executes(ctx -> freezePlayer(ctx.getSource(), EntityArgument.getPlayer(ctx, "target")))))
                
                // --- /fx unfreeze <player> ---
                .then(Commands.literal("unfreeze")
                    .then(Commands.argument("target", EntityArgument.player())
                        .executes(ctx -> unfreezePlayer(ctx.getSource(), EntityArgument.getPlayer(ctx, "target")))))

                // --- /fx info <player> ---
                .then(Commands.literal("info")
                    .then(Commands.argument("target", EntityArgument.player())
                        .executes(ctx -> showPlayerInfo(ctx.getSource(), EntityArgument.getPlayer(ctx, "target")))))

                // --- /fx nota ---
                .then(Commands.literal("nota")
                    .then(Commands.literal("add")
                        .then(Commands.argument("target", EntityArgument.player())
                            .then(Commands.argument("texto", StringArgumentType.greedyString())
                                .executes(ctx -> addNote(
                                    ctx.getSource(), 
                                    EntityArgument.getPlayer(ctx, "target"), 
                                    StringArgumentType.getString(ctx, "texto")
                                )))))
                    .then(Commands.literal("remove")
                        .then(Commands.argument("target", EntityArgument.player())
                            .then(Commands.argument("id", IntegerArgumentType.integer(1))
                                .executes(ctx -> removeNote(
                                    ctx.getSource(), 
                                    EntityArgument.getPlayer(ctx, "target"), 
                                    IntegerArgumentType.getInteger(ctx, "id") - 1
                                ))))))
        );
    }

    private static int freezePlayer(CommandSourceStack source, ServerPlayer target) {
        if (!FXStaffData.isFrozen(target.getUUID())) {
            FXStaffData.toggleFreeze(target.getUUID());
        }
        
        target.connection.send(new ClientboundSetTitleTextPacket(Component.literal(FXStaffConfig.data.freezeTitle)));
        target.connection.send(new ClientboundSetSubtitleTextPacket(Component.literal(FXStaffConfig.data.freezeSubtitle)));

        source.sendSuccess(() -> Component.literal("§a[FXStaff] " + target.getScoreboardName() + " foi congelado."), true);
        return 1;
    }

    private static int unfreezePlayer(CommandSourceStack source, ServerPlayer target) {
        if (FXStaffData.isFrozen(target.getUUID())) {
            FXStaffData.toggleFreeze(target.getUUID());
        }

        target.connection.send(new ClientboundSetTitleTextPacket(Component.literal(FXStaffConfig.data.unfreezeTitle)));
        target.connection.send(new ClientboundSetSubtitleTextPacket(Component.literal(FXStaffConfig.data.unfreezeSubtitle)));

        source.sendSuccess(() -> Component.literal("§a[FXStaff] " + target.getScoreboardName() + " foi descongelado."), true);
        return 1;
    }

    private static int showPlayerInfo(CommandSourceStack source, ServerPlayer target) {
        PlayerTeam team = target.getTeam();
        String teamName = team != null ? team.getDisplayName().getString() : "Nenhum";
        
        String posStr = String.format("X: %.1f | Y: %.1f | Z: %.1f", target.getX(), target.getY(), target.getZ());
        String dimStr = target.level().dimension().location().getPath();

        source.sendSuccess(() -> Component.literal("§e========== INFO: " + target.getScoreboardName() + " =========="), false);
        source.sendSuccess(() -> Component.literal("§fVida: §c" + (int) target.getHealth() + "/" + (int) target.getMaxHealth() + " HP §f| Armadura: §b" + target.getArmorValue()), false);
        source.sendSuccess(() -> Component.literal("§fXP Nível: §a" + target.experienceLevel + " §f| Ping: §a" + target.connection.latency() + "ms"), false);
        source.sendSuccess(() -> Component.literal("§fDimensão: §d" + dimStr + " §f| Pos: §7" + posStr), false);
        source.sendSuccess(() -> Component.literal("§fTime: §b" + teamName), false);

        List<String> notes = FXStaffData.getNotes(target.getUUID());
        source.sendSuccess(() -> Component.literal("§fNotas (§e" + notes.size() + "§f):"), false);
        for (int i = 0; i < notes.size(); i++) {
            int index = i + 1;
            String noteText = notes.get(i);
            source.sendSuccess(() -> Component.literal(" §7[" + index + "] §f" + noteText), false);
        }
        source.sendSuccess(() -> Component.literal("§e========================================"), false);
        return 1;
    }

    private static int addNote(CommandSourceStack source, ServerPlayer target, String text) {
        FXStaffData.addNote(target.getUUID(), text);
        source.sendSuccess(() -> Component.literal("§a[FXStaff] Nota adicionada para " + target.getScoreboardName() + "."), true);
        return 1;
    }

    private static int removeNote(CommandSourceStack source, ServerPlayer target, int index) {
        if (FXStaffData.removeNote(target.getUUID(), index)) {
            source.sendSuccess(() -> Component.literal("§a[FXStaff] Nota #" + (index + 1) + " removida de " + target.getScoreboardName() + "."), true);
        } else {
            source.sendFailure(Component.literal("§c[FXStaff] Índice de nota inválido!"));
        }
        return 1;
    }
}