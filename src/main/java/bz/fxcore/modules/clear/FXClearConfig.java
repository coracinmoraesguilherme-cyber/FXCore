package bz.fxcore.modules.clear;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.neoforged.fml.loading.FMLPaths;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.util.ArrayList;
import java.util.List;

public class FXClearConfig {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final File CONFIG_DIR = FMLPaths.CONFIGDIR.get().resolve("fxcore").toFile();
    private static final File CONFIG_FILE = new File(CONFIG_DIR, "fxclear.json");

    public static ConfigData DATA = new ConfigData();

    public static class ConfigData {
        public boolean enableAutoClear = true;
        public int clearRectIntervalMinutes = 5;
        public List<Integer> warningSeconds = new ArrayList<>(List.of(60, 30, 10, 5, 4, 3, 2, 1));
        public String warningMsg = "§e[FXClear] §fA limpeza do chão ocorrerá em §c{time} §fsegundos!";
        
        public List<String> itemWhitelist = new ArrayList<>(List.of("minecraft:netherite_block", "minecraft:diamond_block"));
        public List<String> entityTargetList = new ArrayList<>(List.of("minecraft:item", "minecraft:experience_orb"));
        public String clearSuccessMsg = "§a[FXClear] §fRemovidos §e{count} §fitens/entidades do chão!";
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