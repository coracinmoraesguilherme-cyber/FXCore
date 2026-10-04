package bz.fxcore.core.otimi.chunk;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.chunk.LevelChunk;

import java.util.*;

public class FXLagView {

    public static class ChunkLagData {
        public final ChunkPos pos;
        public final int entityCount;
        public final int blockEntityCount;
        public final int totalScore;

        public ChunkLagData(ChunkPos pos, int entityCount, int blockEntityCount) {
            this.pos = pos;
            this.entityCount = entityCount;
            this.blockEntityCount = blockEntityCount;
            // BlockEntities possuem peso dobrado no processamento do tick
            this.totalScore = (entityCount * 1) + (blockEntityCount * 2); 
        }
    }

    public static void generateReport(ServerLevel level, CommandSourceFeedback callback) {
        Map<ChunkPos, Integer> entityMap = new HashMap<>();
        Set<ChunkPos> activeChunks = new HashSet<>();

        // 1. Mapeia todas as entidades e identifica chunks ocupadas por entidades
        for (Entity entity : level.getAllEntities()) {
            if (entity == null || entity.isRemoved()) continue;
            ChunkPos cPos = entity.chunkPosition();
            entityMap.put(cPos, entityMap.getOrDefault(cPos, 0) + 1);
            activeChunks.add(cPos);
        }

        // 2. Adiciona chunks no raio de visualização dos jogadores online (pega chunks de máquinas/baús sem entidades)
        int viewDist = Math.min(level.getServer().getPlayerList().getViewDistance(), 8);
        for (ServerPlayer player : level.players()) {
            ChunkPos playerChunk = player.chunkPosition();
            for (int dx = -viewDist; dx <= viewDist; dx++) {
                for (int dz = -viewDist; dz <= viewDist; dz++) {
                    activeChunks.add(new ChunkPos(playerChunk.x + dx, playerChunk.z + dz));
                }
            }
        }

        List<ChunkLagData> ranking = new ArrayList<>();

        // 3. Analisa apenas as chunks ativas carregadas
        for (ChunkPos pos : activeChunks) {
            if (!level.hasChunk(pos.x, pos.z)) continue;

            LevelChunk chunk = level.getChunkSource().getChunkNow(pos.x, pos.z);
            if (chunk == null) continue;

            int entities = entityMap.getOrDefault(pos, 0);
            int blockEntities = chunk.getBlockEntities().size();

            if (entities > 0 || blockEntities > 0) {
                ranking.add(new ChunkLagData(pos, entities, blockEntities));
            }
        }

        // 4. Ordena do maior peso para o menor
        ranking.sort((a, b) -> Integer.compare(b.totalScore, a.totalScore));

        callback.send("§e==== [Ranking de Chunks - " + level.dimension().location().getPath().toUpperCase() + "] ====");

        if (ranking.isEmpty()) {
            callback.send("§a[FXCore] Nenhuma chunk com acúmulo de peso encontrada nesta dimensão.");
            return;
        }

        int maxScore = ranking.get(0).totalScore;
        int limit = Math.min(10, ranking.size());

        for (int i = 0; i < limit; i++) {
            ChunkLagData data = ranking.get(i);
            String progressBar = renderProgressBar(data.totalScore, maxScore);
            
            callback.send(String.format("§7#%d §f(%d, %d) %s §c%d ptn §7[E: §a%d §7| TE: §e%d§7]", 
                (i + 1), 
                data.pos.x, 
                data.pos.z, 
                progressBar, 
                data.totalScore, 
                data.entityCount, 
                data.blockEntityCount
            ));
        }
    }

    private static String renderProgressBar(int score, int maxScore) {
        if (maxScore <= 0) return "§7[░░░░░░░░░░]";
        int totalBars = 10;
        int filled = (int) Math.round(((double) score / maxScore) * totalBars);
        filled = Math.max(1, Math.min(totalBars, filled));

        StringBuilder bar = new StringBuilder("§c[");
        for (int i = 0; i < totalBars; i++) {
            if (i < filled) {
                bar.append("█");
            } else {
                bar.append("§7░");
            }
        }
        bar.append("§c]");
        return bar.toString();
    }

    @FunctionalInterface
    public interface CommandSourceFeedback {
        void send(String message);
    }
}