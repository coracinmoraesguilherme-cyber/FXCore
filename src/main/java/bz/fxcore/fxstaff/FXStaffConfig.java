package bz.fxcore.fxstaff;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.util.*;
import java.util.concurrent.CompletableFuture;

public class FXStaffConfig {

    private static final File CONFIG_FILE = new File("config/fxcore/staff.json");
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    public static class ConfigData {
        public String freezeTitle = "§c§lVOCÊ FOI CONGELADO!";
        public String freezeSubtitle = "§fAguarde as instruções da Staff.";
        public String unfreezeTitle = "§a§lVOCÊ FOI DESCONGELADO!";
        public String unfreezeSubtitle = "§fVocê já pode se mover.";
        public Map<String, List<String>> notes = new HashMap<>();
    }

    public static ConfigData data = new ConfigData();

    public static void carregar() {
        try {
            if (!CONFIG_FILE.getParentFile().exists()) {
                CONFIG_FILE.getParentFile().mkdirs();
            }

            if (CONFIG_FILE.exists()) {
                try (FileReader reader = new FileReader(CONFIG_FILE)) {
                    data = GSON.fromJson(reader, ConfigData.class);
                    if (data == null) data = new ConfigData();
                    
                    FXStaffData.clearNotes();
                    for (Map.Entry<String, List<String>> entry : data.notes.entrySet()) {
                        UUID uuid = UUID.fromString(entry.getKey());
                        for (String note : entry.getValue()) {
                            FXStaffData.addNoteSilent(uuid, note);
                        }
                    }
                }
            } else {
                salvarAsync();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // Salva em background para não travar a thread principal do servidor
    public static void salvarAsync() {
        CompletableFuture.runAsync(() -> {
            synchronized (CONFIG_FILE) {
                try {
                    data.notes.clear();
                    for (Map.Entry<UUID, List<String>> entry : FXStaffData.getAllNotes().entrySet()) {
                        data.notes.put(entry.getKey().toString(), new ArrayList<>(entry.getValue()));
                    }

                    try (FileWriter writer = new FileWriter(CONFIG_FILE)) {
                        GSON.toJson(data, writer);
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        });
    }
}