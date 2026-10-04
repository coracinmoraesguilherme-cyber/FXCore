package bz.fxcore.core.util;

import com.mojang.brigadier.CommandDispatcher;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;

import java.util.Locale;

public class SimpleCommand {

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        
        // 1. Comando /tps
        dispatcher.register(Commands.literal("tps")
            .executes(context -> {
                CommandSourceStack source = context.getSource();
                MinecraftServer server = source.getServer();
                
                double mspt = server.getAverageTickTimeNanos() / 1_000_000.0;
                double tps = mspt > 0 ? Math.min(20.0, 1000.0 / mspt) : 20.0;

                String color = tps >= 18.0 ? "§a" : tps >= 15.0 ? "§e" : "§c";
                
                source.sendSuccess(() -> Component.literal("§e[FXCore] TPS: " + color + String.format(Locale.US, "%.1f", tps) + " §7| MSPT: §f" + String.format(Locale.US, "%.2f", mspt) + "ms"), false);
                return 1;
            })
        );

        // 2. Comando /playtime
        dispatcher.register(Commands.literal("playtime")
            .executes(context -> {
                CommandSourceStack source = context.getSource();
                if (!(source.getEntity() instanceof ServerPlayer player)) {
                    source.sendFailure(Component.literal("§c[FXCore] Este comando só pode ser executado por jogadores."));
                    return 0;
                }

                int ticksPlayed = player.getStats().getValue(net.minecraft.stats.Stats.CUSTOM.get(net.minecraft.stats.Stats.PLAY_TIME));
                long totalSeconds = ticksPlayed / 20;
                
                long hours = totalSeconds / 3600;
                long minutes = (totalSeconds % 3600) / 60;
                long seconds = totalSeconds % 60;

                source.sendSuccess(() -> Component.literal("§e[FXCore] Seu tempo de jogo: §a" + hours + "h " + minutes + "m " + seconds + "s"), false);
                return 1;
            })
        );

        // 3. Comando /repair
        dispatcher.register(Commands.literal("repair")
            .requires(source -> source.hasPermission(2))
            .executes(context -> {
                CommandSourceStack source = context.getSource();
                if (!(source.getEntity() instanceof ServerPlayer player)) {
                    source.sendFailure(Component.literal("§c[FXCore] Este comando só pode ser executado por jogadores."));
                    return 0;
                }

                ItemStack itemInHand = player.getMainHandItem();

                if (itemInHand.isEmpty()) {
                    source.sendFailure(Component.literal("§c[FXCore] Você precisa segurar um item na mão principal para repará-lo."));
                    return 0;
                }

                if (!itemInHand.isDamaged()) {
                    source.sendFailure(Component.literal("§c[FXCore] O item que você está segurando não está danificado."));
                    return 0;
                }

                itemInHand.setDamageValue(0);
                source.sendSuccess(() -> Component.literal("§a[FXCore] Item reparado com sucesso!"), false);
                return 1;
            })
        );
    }
}