package bz.fxcore.fxserver;

import net.minecraft.server.MinecraftServer;
import net.minecraft.world.entity.MobCategory;
import java.lang.reflect.Field;

public class FXServerManager {

    private static int currentViewDistance = -1;
    private static int currentSimulationDistance = -1;
    private static double currentMobCapMultiplier = 1.0;
    private static boolean isOptimized = false;

    private static int checkTicks = 0;

    // Valores padrão originais do Vanilla para cada categoria
    private static final int BASE_MONSTER_CAP = 70;
    private static final int BASE_CREATURE_CAP = 10;
    private static final int BASE_AMBIENT_CAP = 15;
    private static final int BASE_WATER_CAP = 5;

    public static void initDefaults() {
        if (currentViewDistance == -1) currentViewDistance = FXServerConfig.DATA.defaultViewDistance;
        if (currentSimulationDistance == -1) currentSimulationDistance = FXServerConfig.DATA.defaultSimulationDistance;
    }

    public static void onServerTick(MinecraftServer server) {
        initDefaults();

        checkTicks++;
        if (checkTicks < 100) return;
        checkTicks = 0;

        if (!FXServerConfig.DATA.autoOptimizationEnabled) return;

        double mspt = server.getAverageTickTimeNanos() / 1_000_000.0;
        double tps = Math.min(20.0, 1000.0 / Math.max(mspt, 50.0));

        if (tps < FXServerConfig.DATA.targetTpsThreshold && !isOptimized) {
            isOptimized = true;
            applyPerformanceProfile(server,
                    FXServerConfig.DATA.minViewDistance,
                    FXServerConfig.DATA.minSimulationDistance,
                    FXServerConfig.DATA.minMobCapMultiplier
            );
        } else if (tps >= FXServerConfig.DATA.restoreTpsThreshold && isOptimized) {
            isOptimized = false;
            applyPerformanceProfile(server,
                    FXServerConfig.DATA.defaultViewDistance,
                    FXServerConfig.DATA.defaultSimulationDistance,
                    FXServerConfig.DATA.defaultMobCapMultiplier
            );
        }
    }

    public static void applyPerformanceProfile(MinecraftServer server, int viewDist, int simDist, double mobCap) {
        setViewDistance(server, viewDist);
        setSimulationDistance(server, simDist);
        setMobCapMultiplier(mobCap);
    }

    public static void setViewDistance(MinecraftServer server, int viewDist) {
        currentViewDistance = viewDist;
        if (server.getPlayerList() != null) {
            server.getPlayerList().setViewDistance(viewDist);
        }
    }

    public static void setSimulationDistance(MinecraftServer server, int simDist) {
        currentSimulationDistance = simDist;
        if (server.getPlayerList() != null) {
            server.getPlayerList().setSimulationDistance(simDist);
        }
    }

    public static void setMobCapMultiplier(double mobCap) {
    currentMobCapMultiplier = mobCap;

    updateCategoryMax(MobCategory.MONSTER, (int) (BASE_MONSTER_CAP * mobCap));
    updateCategoryMax(MobCategory.CREATURE, (int) (BASE_CREATURE_CAP * mobCap));
    updateCategoryMax(MobCategory.AMBIENT, (int) (BASE_AMBIENT_CAP * mobCap));
    updateCategoryMax(MobCategory.WATER_CREATURE, (int) (BASE_WATER_CAP * mobCap));
    updateCategoryMax(MobCategory.WATER_AMBIENT, (int) (BASE_WATER_CAP * mobCap));
    updateCategoryMax(MobCategory.UNDERGROUND_WATER_CREATURE, (int) (BASE_WATER_CAP * mobCap));
    updateCategoryMax(MobCategory.AXOLOTLS, (int) (BASE_WATER_CAP * mobCap));
    }
    private static void updateCategoryMax(MobCategory category, int newMax) {
    try {
        // Tenta buscar pelo nome mapeado do Mojang/NeoForge ("max")
        Field maxField;
        try {
            maxField = MobCategory.class.getDeclaredField("max");
        } catch (NoSuchFieldException e) {
            // Fallback para obfuscado caso esteja rodando em ambiente de produção sem mappings (ex: f_21585_)
            maxField = MobCategory.class.getDeclaredField("f_21585_");
        }

        maxField.setAccessible(true);
        maxField.setInt(category, newMax);
    } catch (Exception e) {
        e.printStackTrace();
    }
    }

    public static int getCurrentViewDistance() { return currentViewDistance; }
    public static int getCurrentSimulationDistance() { return currentSimulationDistance; }
    public static double getCurrentMobCapMultiplier() { return currentMobCapMultiplier; }
    public static boolean isOptimized() { return isOptimized; }
}