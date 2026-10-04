package bz.fxcore.core.otimi.chunk;

import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.phys.AABB;

import java.util.HashMap;
import java.util.Map;

public class FXChunkAnalyzer {

    /**
     * Analisa uma chunk específica e retorna um relatório de entidades e BlockEntities (TileEntities)
     */
    public static void analyzeChunk(ServerLevel level, ChunkPos pos, CommandSourceStackFeedback callback) {
        LevelChunk chunk = level.getChunkSource().getChunkNow(pos.x, pos.z);

        if (chunk == null) {
            callback.send("§c[FXCore] A chunk (" + pos.x + ", " + pos.z + ") não está carregada na memória!");
            return;
        }

        // 1. Contagem de Entidades normais delimitada pelo AABB da chunk
        Map<String, Integer> entityCounts = new HashMap<>();
        int totalEntities = 0;

        AABB chunkBox = new AABB(
            pos.getMinBlockX(), level.getMinBuildHeight(), pos.getMinBlockZ(),
            pos.getMaxBlockX() + 1, level.getMaxBuildHeight(), pos.getMaxBlockZ() + 1
        );

        for (Entity entity : level.getEntities((Entity) null, chunkBox, e -> !e.isRemoved() && e.chunkPosition().equals(pos))) {
            String name = entity.getType() != null ? entity.getType().getDescription().getString() : "desconhecido";
            entityCounts.put(name, entityCounts.getOrDefault(name, 0) + 1);
            totalEntities++;
        }

        // 2. Contagem de TileEntities (BlockEntities)
        Map<String, Integer> blockEntityCounts = new HashMap<>();
        int totalBlockEntities = 0;

        for (Map.Entry<BlockPos, BlockEntity> entry : chunk.getBlockEntities().entrySet()) {
            ResourceLocation key = BuiltInRegistries.BLOCK_ENTITY_TYPE.getKey(entry.getValue().getType());
            String name = key != null ? key.toString() : "desconhecido";
            blockEntityCounts.put(name, blockEntityCounts.getOrDefault(name, 0) + 1);
            totalBlockEntities++;
        }

        // 3. Exibe o Relatório no Chat
        callback.send("§e==== [Análise de Chunk: (" + pos.x + ", " + pos.z + ")] ====");
        callback.send("§7> Dimensão: §f" + level.dimension().location());
        callback.send("§7> Total Entidades: §a" + totalEntities + " §7| Total BlockEntities: §a" + totalBlockEntities);
        
        if (!entityCounts.isEmpty()) {
            callback.send("§e> Principais Entidades:");
            entityCounts.entrySet().stream()
                .sorted((a, b) -> Integer.compare(b.getValue(), a.getValue()))
                .limit(5)
                .forEach(e -> callback.send("  §7- §f" + e.getKey() + ": §c" + e.getValue()));
        }

        if (!blockEntityCounts.isEmpty()) {
            callback.send("§e> Principais BlockEntities (Máquinas/Containers):");
            blockEntityCounts.entrySet().stream()
                .sorted((a, b) -> Integer.compare(b.getValue(), a.getValue()))
                .limit(5)
                .forEach(e -> callback.send("  §7- §f" + e.getKey() + ": §c" + e.getValue()));
        }
    }

    @FunctionalInterface
    public interface CommandSourceStackFeedback {
        void send(String message);
    }
}