package bz.fxcore.modules.build;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.neoforged.fml.loading.FMLPaths;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;

public class FXBuildConfig {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final File CONFIG_DIR = FMLPaths.CONFIGDIR.get().resolve("fxcore").toFile();
    private static final File CONFIG_FILE = new File(CONFIG_DIR, "fxbuild.json");

    public static ConfigData DATA = new ConfigData();

    public static class ConfigData {
        public boolean enableBuildModule = true;
        public String buildEnableMsg = "§a[FXBuild] §fModo de construção sem físicas §aATIVADO§f!";
        public String buildDisableMsg = "§c[FXBuild] §fModo de construção sem físicas §cDESATIVADO§f!";
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
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}