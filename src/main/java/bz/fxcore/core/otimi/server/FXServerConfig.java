package bz.fxcore.core.otimi.server;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.neoforged.fml.loading.FMLPaths;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;

public class FXServerConfig {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final File CONFIG_DIR = FMLPaths.CONFIGDIR.get().resolve("fxcore").toFile();
    private static final File CONFIG_FILE = new File(CONFIG_DIR, "fxserver.json");

    public static ConfigData DATA = new ConfigData();

    public static class ConfigData {
        public String _comment_autoOptimization = "Ativa o monitoramento automatico de TPS para ajustar configuracoes quando o servidor lagar.";
        public boolean autoOptimizationEnabled = true;

        public String _comment_warnings = "Ativa ou desativa os alertas de performance enviados no chat da Staff.";
        public boolean warningsEnabled = true;

        public String _comment_tpsThresholds = "Valores de TPS para acionar otimizacao ou restaurar configuracoes normais.";
        public double targetTpsThreshold = 18.0;
        public double restoreTpsThreshold = 19.5;

        public String _comment_defaultSettings = "Valores padrao do servidor (estado normal).";
        public int defaultViewDistance = 10;
        public int defaultSimulationDistance = 8;
        public double defaultMobCapMultiplier = 1.0;

        public String _comment_optimizedSettings = "Valores reduzidos quando o servidor estiver sob estresse de lag.";
        public int minViewDistance = 6;
        public int minSimulationDistance = 4;
        public double minMobCapMultiplier = 0.5;

        public String _comment_antiLagSettings = "Configuracoes do FXAntiLag (AFK Farm, Redstone Guard e Unloader).";
        public boolean enableChunkLoadDelay = true;
        public long chunkLoadDelayMs = 1000L;
        public boolean enableRedstoneGuard = true;
        public int maxRedstoneUpdatesPerSecond = 20;
        public boolean enableDimensionUnloader = true;
        public long dimensionUnloadDelayMinutes = 5;
        public boolean enableAfkFarmGuard = true;
        public double cpsThreshold = 15.0;
        public int maxAfkClickSeconds = 10;
        public boolean ignoreStaffAfk = false;

        public Messages messages = new Messages();

        public static class Messages {
            public String statusHeader = "\u00A7e==== \u00A7a[FXServer Status] \u00A7e====";
            public String statusTps = "\u00A77> TPS: \u00A7e{tps} \u00A77| MSPT: \u00A7e{mspt}ms";
            public String statusWorld = "\u00A77> Chunks carregadas: \u00A7a{chunks} \u00A77| Entidades: \u00A7a{entities}";
            public String statusPlayers = "\u00A77> Players Online: \u00A7a{players}";
            public String statusCore = "\u00A77> Otimizacao automatica: \u00A7e{enabled} \u00A77| Status: \u00A7e{coreStatus}";
            public String statusView = "\u00A77> View Distance: \u00A7a{view} \u00A77(Padrao: {defView})";
            public String statusTick = "\u00A77> Simulation/Tick Distance: \u00A7a{tick} \u00A77(Padrao: {defTick})";
            public String statusMobCap = "\u00A77> MobCap Multiplier: \u00A7a{mobcap}%";
            public String forceViewSuccess = "\u00A7a[FXServer] View Distance alterada para: \u00A7e{view}";
            public String forceTickSuccess = "\u00A7a[FXServer] Simulation/Tick Distance alterada para: \u00A7e{tick}";
            public String forceMobCapSuccess = "\u00A7a[FXServer] Multiplicador de MobCap alterado para: \u00A7e{mobcap}%";
            public String reloadSuccess = "\u00A7a[FXServer] Configurações de performance recarregadas com sucesso!";

            // Mensagens AntiLag / AFK
            public String afkHudMessage = "\u00A7c\u00A7l[AFK] \u00A7fVocê será desconectado em \u00A7e\u00A7l%s\u00A7fs! Mova-se!";
            public String afkKickMessage = "\u00A7c[FXCore AntiLag]\n\n\u00A7fVocê foi desconectado por inatividade / Farm AFK.";
            public String staffAfkAlert = "\u00A7c\u00A7l[FXCore] \u00A7fO jogador \u00A7e%s \u00A7ffoi desconectado por \u00A7cAFK\u00A7f.";
            public String autoClickerWarning = "\u00A7c[FXCore AntiLag] \u00A7fAção bloqueada: Auto-Clicker / Macro detectado!";
            public String afkCancelMessage = "\u00A7a[FXCore] Contagem AFK cancelada!";
        }
    }

    public static void load() {
        carregar();
    }

    public static void carregar() {
        if (!CONFIG_DIR.exists()) CONFIG_DIR.mkdirs();
        if (CONFIG_FILE.exists()) {
            try (FileReader reader = new FileReader(CONFIG_FILE)) {
                ConfigData loaded = GSON.fromJson(reader, ConfigData.class);
                if (loaded != null) DATA = loaded;
            } catch (Exception e) { e.printStackTrace(); }
        } else {
            salvar();
        }
    }

    public static void save() {
        salvar();
    }

    public static void salvar() {
        try {
            if (!CONFIG_DIR.exists()) CONFIG_DIR.mkdirs();
            try (FileWriter writer = new FileWriter(CONFIG_FILE)) {
                GSON.toJson(DATA, writer);
            }
        } catch (Exception e) { e.printStackTrace(); }
    }
}