package bz.fxcore.modules.build.particles;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.io.File;
import java.io.FileReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ParticleShapeJson {
    public String name;
    public List<List<Double>> points;                     // Forma 2D/3D estática
    public List<List<List<Double>>> frames;               // Animação quadro a quadro (opcional)

    private static final Map<String, ParticleShapeJson> SHAPES = new HashMap<>();
    private static final Gson GSON = new GsonBuilder().create();

    public static void loadShapes(Path configDir) {
        SHAPES.clear();
        Path shapeDir = configDir.resolve("fxcore/particles/shapes");

        try {
            if (!Files.exists(shapeDir)) {
                Files.createDirectories(shapeDir);
                return;
            }

            File[] files = shapeDir.toFile().listFiles((dir, name) -> name.endsWith(".json"));
            if (files != null) {
                for (File file : files) {
                    try (FileReader reader = new FileReader(file)) {
                        ParticleShapeJson shape = GSON.fromJson(reader, ParticleShapeJson.class);
                        if (shape != null && shape.name != null) {
                            SHAPES.put(shape.name.toLowerCase(), shape);
                        }
                    } catch (Exception e) {
                        System.err.println("[FXCore] Erro ao ler forma JSON: " + file.getName());
                    }
                }
            }
            System.out.println("[FXCore] Carregadas " + SHAPES.size() + " formas JSON com sucesso!");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static ParticleShapeJson get(String name) {
        return SHAPES.get(name.toLowerCase());
    }
}