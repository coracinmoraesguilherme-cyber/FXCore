package bz.fxcore.fxantilag;

import com.google.common.collect.Streams;
import com.mojang.brigadier.CommandDispatcher;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.RegisterCommandsEvent;

public class FXCoreCommands {

    @SubscribeEvent
    public static void onRegisterCommands(RegisterCommandsEvent event) {
        CommandDispatcher<CommandSourceStack> dispatcher = event.getDispatcher();

        dispatcher.register(
            Commands.literal("fxcore")
                .requires(source -> source.hasPermission(2)) // Permissão de Staff (Level 2 / OP)
                
                // --- SUBCOMANDO: /fxcore reload ---
                .then(Commands.literal("reload")
                    .executes(context -> {
                        FXAntiLagConfig.load();
                        context.getSource().sendSuccess(
                            () -> Component.literal("§a[FXCore] Configuração do AntiLag recarregada com sucesso!"), 
                            true
                        );
                        return 1;
                    })
                )

                // --- SUBCOMANDO: /fxcore status ---
                .then(Commands.literal("status")
                    .executes(context -> {
                        CommandSourceStack source = context.getSource();
                        ServerLevel level = source.getLevel();

                        // Forma correta de contar Iterable<Entity> no NeoForge 1.21.1
                        long totalEntities = Streams.stream(level.getAllEntities()).count();
                        int loadedChunks = level.getChunkSource().getLoadedChunksCount();

                        source.sendSuccess(() -> Component.literal("§e================ §c§lFXCore AntiLag Status §e================"), false);
                        
                        // Módulos
                        source.sendSuccess(() -> Component.literal("§7* Chunk Delay: " + (FXAntiLagConfig.DATA.enableChunkLoadDelay ? "§aAtivado" : "§cDesativado") + " §8(" + FXAntiLagConfig.DATA.chunkLoadDelayMs + "ms)"), false);
                        source.sendSuccess(() -> Component.literal("§7* Redstone Guard: " + (FXAntiLagConfig.DATA.enableRedstoneGuard ? "§aAtivado" : "§cDesativado") + " §8(Max: " + FXAntiLagConfig.DATA.maxRedstoneUpdatesPerSecond + "/s)"), false);
                        source.sendSuccess(() -> Component.literal("§7* Smart AFK Guard: " + (FXAntiLagConfig.DATA.enableAfkFarmGuard ? "§aAtivado" : "§cDesativado") + " §8(Tempo: " + FXAntiLagConfig.DATA.maxAfkClickSeconds + "s)"), false);
                        source.sendSuccess(() -> Component.literal("§7* Dimension Unloader: " + (FXAntiLagConfig.DATA.enableDimensionUnloader ? "§aAtivado" : "§cDesativado")), false);
                        
                        // Status do Mundo
                        source.sendSuccess(() -> Component.literal("§e--------------------------------------------------"), false);
                        source.sendSuccess(() -> Component.literal("§7Mundo Atual: §f" + level.dimension().location().getPath()), false);
                        source.sendSuccess(() -> Component.literal("§7Chunks Carregadas: §e" + loadedChunks), false);
                        source.sendSuccess(() -> Component.literal("§7Entidades Ativas: §e" + totalEntities), false);
                        source.sendSuccess(() -> Component.literal("§e=================================================="), false);

                        return 1;
                    })
                )
        );
    }
}