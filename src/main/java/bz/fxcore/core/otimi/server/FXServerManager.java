package bz.fxcore.core.otimi.server;

import bz.fxcore.core.otimi.FXCoreWarnings;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;

public class FXServerManager {

    private static double mobCapMultiplier = 1.0;
    private static int tickCounter = 0;
    private static boolean activeOptimization = false; // Guarda o estado atual da otimização

    // Método que monitora e ajusta as configurações automaticamente a cada segundo
    public static void onServerTick(MinecraftServer server) {
        if (server == null || !FXServerConfig.DATA.autoOptimizationEnabled) return;

        // Roda a verificação a cada 20 ticks (1 segundo)
        tickCounter++;
        if (tickCounter < 20) return;
        tickCounter = 0;

        double mspt = server.getAverageTickTimeNanos() / 1_000_000.0;
        double tps = Math.min(20.0, 1000.0 / Math.max(mspt, 50.0));

        // Se o TPS cair abaixo do limite e a otimização AINDA NÃO estiver ativa
        if (tps < FXServerConfig.DATA.targetTpsThreshold && !activeOptimization) {
            activeOptimization = true;

            setViewDistance(server, FXServerConfig.DATA.minViewDistance);
            setSimulationDistance(server, FXServerConfig.DATA.minSimulationDistance);
            setMobCapMultiplier(FXServerConfig.DATA.minMobCapMultiplier);

            FXCoreWarnings.warnStaff(server, "§c[FXCore] TPS baixo (" + String.format("%.1f", tps) + ")! Otimização automática ATIVADA.");
        } 
        // Se o TPS normalizar e a otimização ESTIVER ativa, restaura o padrão
        else if (tps >= FXServerConfig.DATA.restoreTpsThreshold && activeOptimization) {
            activeOptimization = false;

            setViewDistance(server, FXServerConfig.DATA.defaultViewDistance);
            setSimulationDistance(server, FXServerConfig.DATA.defaultSimulationDistance);
            setMobCapMultiplier(FXServerConfig.DATA.defaultMobCapMultiplier);

            FXCoreWarnings.warnStaff(server, "§a[FXCore] TPS normalizado (" + String.format("%.1f", tps) + "). Configurações padrão RESTAURADAS.");
        }
    }

    public static boolean isOptimized(MinecraftServer server) {
        return activeOptimization; // Retorna o estado real do sistema
    }

    public static void setViewDistance(MinecraftServer server, int distance) {
        server.getPlayerList().setViewDistance(distance);
    }

    public static void setSimulationDistance(MinecraftServer server, int distance) {
        server.getPlayerList().setSimulationDistance(distance);
    }

    public static void setMobCapMultiplier(double multiplier) {
        mobCapMultiplier = multiplier;
    }

    public static double getCurrentMobCapMultiplier() {
        return mobCapMultiplier;
    }

    public static int getCurrentViewDistance() {
        return FXServerConfig.DATA.defaultViewDistance;
    }

    public static int getCurrentSimulationDistance() {
        return FXServerConfig.DATA.defaultSimulationDistance;
    }

    public static int getTotalLoadedChunks(MinecraftServer server) {
        int chunks = 0;
        for (ServerLevel level : server.getAllLevels()) {
            chunks += level.getChunkSource().getLoadedChunksCount();
        }
        return chunks;
    }

    public static int getTotalLoadedEntities(MinecraftServer server) {
        int entities = 0;
        for (ServerLevel level : server.getAllLevels()) {
            int count = 0;
            for (Object ignored : level.getAllEntities()) {
                count++;
            }
            entities += count;
        }
        return entities;
    }
}