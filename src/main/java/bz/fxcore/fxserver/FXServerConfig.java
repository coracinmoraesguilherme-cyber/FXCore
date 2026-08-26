package bz.fxcore.fxserver;

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

        public Messages messages = new Messages();

        public static class Messages {
            public String statusHeader = "§e==== §a[FXServer Performance Status] §e====";
            public String statusTps = "§7> TPS Atual: §e{tps} §7| MSPT: §e{mspt}ms";
            public String statusAuto = "§7> Otimizacao automatica: §e{enabled}";
            public String statusView = "§7> View Distance: §a{view} §7(Padrao: {defView})";
            public String statusTick = "§7> Tick/Simulation Distance: §a{tick} §7(Padrao: {defTick})";
            public String statusMobCap = "§7> MobCap Multiplier: §a{mobcap}%";
            public String forceViewSuccess = "§a[FXServer] View Distance alterada para: §e{view}";
            public String forceTickSuccess = "§a[FXServer] Simulation/Tick Distance alterada para: §e{tick}";
            public String forceMobCapSuccess = "§a[FXServer] Multiplicador de MobCap alterado para: §e{mobcap}%";
            public String reloadSuccess = "§a[FXServer] Configurações de performance recarregadas com sucesso!";
        }
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

    public static void salvar() {
        try {
            if (!CONFIG_DIR.exists()) CONFIG_DIR.mkdirs();
            try (FileWriter writer = new FileWriter(CONFIG_FILE)) {
                GSON.toJson(DATA, writer);
            }
        } catch (Exception e) { e.printStackTrace(); }
    }
}