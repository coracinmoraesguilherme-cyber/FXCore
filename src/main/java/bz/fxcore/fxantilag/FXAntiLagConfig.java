package bz.fxcore.fxantilag;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.neoforged.fml.loading.FMLPaths;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;

public class FXAntiLagConfig {

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final File CONFIG_DIR = FMLPaths.CONFIGDIR.get().resolve("fxcore").toFile();
    private static final File CONFIG_FILE = new File(CONFIG_DIR, "fxantilag.json");

    public static ConfigData DATA = new ConfigData();

    public static class ConfigData {
        // Chunk Load Guard
        public boolean enableChunkLoadDelay = true;
        public long chunkLoadDelayMs = 1000L;

        // Redstone Guard
        public boolean enableRedstoneGuard = true;
        public int maxRedstoneUpdatesPerSecond = 20;

        // Dimension Unloader
        public boolean enableDimensionUnloader = true;
        public long dimensionUnloadDelayMinutes = 5;

        // Smart AFK & Auto-Clicker Guard
        public boolean enableAfkFarmGuard = true;
        public double cpsThreshold = 15.0;
        public int maxAfkClickSeconds = 10; // Tempo limite em SEGUNDOS (ex: 10 para 10s)
        public boolean ignoreStaffAfk = false;

        // Textos do AFK / Anti-Macro
        public String afkHudMessage = "§c§l[AFK] §fVocê será desconectado em §e§l%s§fs! Mova-se!";
        public String afkKickMessage = "§c[FXCore AntiLag]\n\n§fVocê foi desconectado por inatividade / Farm AFK.";
        public String staffAfkAlert = "§c§l[FXCore] §fO jogador §e%s §ffoi desconectado por §cAFK§f.";
        public String autoClickerWarning = "§c[FXCore AntiLag] §fAção bloqueada: Auto-Clicker / Macro detectado!";
        public String afkCancelMessage = "§a[FXCore] Contagem AFK cancelada!";

        // Textos do Redstone Guard
        public String redstoneSignLine1 = "§c[SISTEMA]";
        public String redstoneSignLine2 = "§4CLOCK DETECTADO";
        public String redstoneSignLine3 = "§cRedstone removida";
        public String redstoneSignLine4 = "§cpor segurança";
        public String redstoneStaffAlert = "§c§l[FXCore] §fClock de Redstone destruído em: §eX: %d, Y: %d, Z: %d §f(%s)";
    }

    public static void load() {
        if (!CONFIG_DIR.exists()) {
            CONFIG_DIR.mkdirs();
        }

        if (!CONFIG_FILE.exists()) {
            save();
            return;
        }

        try (FileReader reader = new FileReader(CONFIG_FILE)) {
            DATA = GSON.fromJson(reader, ConfigData.class);
            if (DATA == null) {
                DATA = new ConfigData();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void save() {
        try {
            if (!CONFIG_DIR.exists()) {
                CONFIG_DIR.mkdirs();
            }
            try (FileWriter writer = new FileWriter(CONFIG_FILE)) {
                GSON.toJson(DATA, writer);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}