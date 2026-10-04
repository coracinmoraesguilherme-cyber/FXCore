package bz.fxcore.core.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.neoforged.fml.loading.FMLPaths;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;

public class FXCoreConfig {

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final File CONFIG_DIR = FMLPaths.CONFIGDIR.get().resolve("fxcore").toFile();
    private static final File CONFIG_FILE = new File(CONFIG_DIR, "core.json");

    public static ConfigData DATA = new ConfigData();

    public static class ConfigData {
        public boolean warningsEnabled = true;

        public AfkSettings afk = new AfkSettings();

        public static class AfkSettings {
            public boolean enabled = true;
            public int maxAfkSeconds = 300; // 5 minutos padrão
            public boolean ignoreStaffAfk = true;
            public int countdownSeconds = 15;

            // Mensagens do sistema AFK
            public String afkHudMessage = "§c§l[AFK] §fVocê será desconectado em §e§l%s§fs! Mova-se!";
            public String afkKickMessage = "§c[FXCore AntiLag]\n\n§fVocê foi desconectado por inatividade / Farm AFK.";
            public String staffAfkAlert = "§c§l[FXCore] §fO jogador §e%s §ffoi desconectado por §cAFK§f.";
            public String afkCancelMessage = "§a[FXCore] Contagem AFK cancelada!";
        }
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
            ConfigData loaded = GSON.fromJson(reader, ConfigData.class);
            if (loaded != null) {
                DATA = loaded;
            }
        } catch (Exception e) {
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

