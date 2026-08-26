package bz.fxcore.fxclearitems;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.neoforged.fml.loading.FMLPaths;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class ClearItemsConfig {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final File CONFIG_DIR = FMLPaths.CONFIGDIR.get().resolve("fxcore").toFile();
    private static final File CONFIG_FILE = new File(CONFIG_DIR, "fxclearitems.json");

    public static ConfigData DATA = new ConfigData();

    public static class ConfigData {
        public boolean enabled = true;
        public int intervalSeconds = 300;
        public List<Integer> warningTimes = Arrays.asList(60, 30, 10);
        public List<String> whitelist = new ArrayList<>(List.of("minecraft:netherite_pickaxe", "minecraft:diamond_block"));

        public Messages messages = new Messages();

        public static class Messages {
            public String warningMessage = "§c[FXClearItems] §eLimpeza de itens do chão em §c{seconds}s§e!";
            public String clearedMessage = "§a[FXClearItems] §eVarredura concluída! §a{count} §eitem(ns) removido(s) do chão.";
            public String manualClearStarted = "§a[FXClearItems] §eForçando varredura de itens agora...";
            public String reloadSuccess = "§a[FXClearItems] §eConfigurações e whitelist recarregadas!";
            public String noItemInHand = "§c[FXClearItems] Você precisa segurar um item na mão principal!";
            public String itemAddedToWhitelist = "§a[FXClearItems] O item §e{item} §afoi adicionado à whitelist!";
            public String itemAlreadyInWhitelist = "§c[FXClearItems] O item §e{item} §cjá está na whitelist.";
            public String itemRemovedFromWhitelist = "§c[FXClearItems] O item §e{item} §cfoi removido da whitelist!";
            public String itemNotInWhitelist = "§c[FXClearItems] O item §e{item} §cnão está na whitelist.";
            public String statusHeader = "§e==== §a[FXClearItems Status] §e====";
            public String statusNextClear = "§7> Próxima limpeza em: §a{seconds}s";
            public String statusWarnings = "§7> Avisos programados: §e{warnings}";
            public String statusInterval = "§7> Intervalo configurado: §e{seconds}s";
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