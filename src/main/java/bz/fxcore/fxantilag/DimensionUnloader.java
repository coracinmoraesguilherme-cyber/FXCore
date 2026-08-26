package bz.fxcore.fxantilag;

import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

import java.util.HashMap;
import java.util.Map;

public class DimensionUnloader {

    private static final Map<ResourceKey<Level>, Long> EMPTY_DIMENSION_TIMERS = new HashMap<>();
    private static int tickCounter = 0;

    @SubscribeEvent
    public static void onServerTick(ServerTickEvent.Post event) {
        if (!FXAntiLagConfig.DATA.enableDimensionUnloader) {
            return;
        }

        // Executa a checagem a cada 100 ticks (5 segundos) para economizar CPU
        tickCounter++;
        if (tickCounter < 100) {
            return;
        }
        tickCounter = 0;

        var server = event.getServer();
        long now = System.currentTimeMillis();
        long maxDelayMs = FXAntiLagConfig.DATA.dimensionUnloadDelayMinutes * 60L * 1000L;

        for (ServerLevel level : server.getAllLevels()) {
            ResourceKey<Level> dimKey = level.dimension();

            // Ignora o Overworld principal
            if (dimKey.equals(Level.OVERWORLD)) {
                continue;
            }

            // Se a dimensão não possui jogadores online
            if (level.players().isEmpty()) {
                long emptySince = EMPTY_DIMENSION_TIMERS.computeIfAbsent(dimKey, k -> now);

                // Se excedeu o tempo limite sem nenhum jogador
                if ((now - emptySince) >= maxDelayMs) {
                    // Limpa entities desnecessárias e força a descarga de chunks ociosas
                    if (level.getChunkSource().getLoadedChunksCount() > 0) {
                        level.save(null, false, false);
                        
                        // Reseta a lista para evitar chamadas contínuas a cada 5 segundos
                        EMPTY_DIMENSION_TIMERS.put(dimKey, now);
                    }
                }
            } else {
                // Jogador entrou na dimensão, reseta o temporizador
                EMPTY_DIMENSION_TIMERS.remove(dimKey);
            }
        }
    }
}