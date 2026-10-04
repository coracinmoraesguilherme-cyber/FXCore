package bz.fxcore.core.otimi.chunk;

import bz.fxcore.core.otimi.server.FXTaskExecutor;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.TagParser;
import net.minecraft.network.protocol.game.ClientboundLevelChunkWithLightPacket;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.chunk.status.ChunkStatus;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.util.ArrayList;
import java.util.List;

public class FXChunkBan {

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final File BACKUP_DIR = new File("config/fxcore/backups");

    /**
     * Gera o nome seguro do arquivo de backup, tratando coordenadas negativas para evitar bugs de leitura.
     */
    private static File getBackupFile(ServerLevel level, ChunkPos pos) {
        String dimensionName = level.dimension().location().getPath();
        // Substitui o sinal de menos por "neg" para que nomes como -10 virem neg10 no arquivo
        String safeX = String.valueOf(pos.x).replace("-", "neg");
        String safeZ = String.valueOf(pos.z).replace("-", "neg");
        
        String fileName = String.format("chunk_%s_%s_%s.json", dimensionName, safeX, safeZ);
        return new File(BACKUP_DIR, fileName);
    }

    /**
     * Limpa com segurança uma chunk suspeita, mesmo que esteja fora da área/descarregada.
     */
    public static void deleteChunk(ServerLevel level, ChunkPos pos, CommandFeedback callback) {
        LevelChunk chunk = (LevelChunk) level.getChunk(pos.x, pos.z, ChunkStatus.FULL, true);

        if (chunk == null) {
            callback.send("§c[FXCore] Não foi possível carregar a chunk (" + pos.x + ", " + pos.z + ") para limpeza!");
            return;
        }

        ensureBackupDirExists();
        File backupFile = getBackupFile(level, pos);

        JsonObject rootJson = new JsonObject();
        JsonArray blocksArray = new JsonArray();

        // 1. Teleporta jogadores na chunk para fora
        teleportPlayersOut(level, pos);

        // 2. Remove todas as entidades com segurança
        List<Entity> entitiesToRemove = new ArrayList<>();
        for (Entity entity : level.getAllEntities()) {
            if (entity != null && !entity.isRemoved() && entity.chunkPosition().equals(pos) && !(entity instanceof Player)) {
                entitiesToRemove.add(entity);
            }
        }
        entitiesToRemove.forEach(Entity::discard);

        // 3. Salva no Backup JSON e substitui por AR
        int minY = level.getMinBuildHeight();
        int maxY = level.getMaxBuildHeight();

        for (int x = 0; x < 16; x++) {
            for (int z = 0; z < 16; z++) {
                for (int y = minY; y < maxY; y++) {
                    BlockPos bPos = new BlockPos(pos.getMinBlockX() + x, y, pos.getMinBlockZ() + z);
                    BlockState state = chunk.getBlockState(bPos);

                    if (!state.isAir()) {
                        JsonObject bObj = new JsonObject();
                        bObj.addProperty("x", bPos.getX());
                        bObj.addProperty("y", bPos.getY());
                        bObj.addProperty("z", bPos.getZ());
                        bObj.addProperty("block", BuiltInRegistries.BLOCK.getKey(state.getBlock()).toString());

                        BlockEntity be = chunk.getBlockEntity(bPos);
                        if (be != null) {
                            CompoundTag tag = be.saveWithFullMetadata(level.registryAccess());
                            bObj.addProperty("nbt", tag.toString());
                            chunk.removeBlockEntity(bPos);
                        }

                        blocksArray.add(bObj);
                        level.setBlock(bPos, Blocks.AIR.defaultBlockState(), 2);
                    }
                }
            }
        }

        rootJson.add("blocks", blocksArray);

        // 4. Salva o arquivo de backup de forma assíncrona para não travar a thread do servidor
        refreshChunkForPlayers(level, chunk);
        callback.send("§a[FXCore] Chunk (" + pos.x + ", " + pos.z + ") deletada e limpada com sucesso!");

        FXTaskExecutor.runAsync(() -> {
            try (FileWriter writer = new FileWriter(backupFile)) {
                GSON.toJson(rootJson, writer);
                callback.send("§7 Backup salvo em: §e" + backupFile.getName());
            } catch (Exception e) {
                callback.send("§c[FXCore] Erro ao salvar backup no disco: " + e.getMessage());
            }
        });
    }

    /**
     * Restaura a chunk a partir do backup JSON e atualiza os clientes.
     */
    public static void restoreChunk(ServerLevel level, ChunkPos pos, CommandFeedback callback) {
        File backupFile = getBackupFile(level, pos);

        if (!backupFile.exists()) {
            callback.send("§c[FXCore] Nenhum backup encontrado para a chunk (" + pos.x + ", " + pos.z + ").");
            return;
        }

        LevelChunk chunk = (LevelChunk) level.getChunk(pos.x, pos.z, ChunkStatus.FULL, true);

        try (FileReader reader = new FileReader(backupFile)) {
            JsonObject rootJson = GSON.fromJson(reader, JsonObject.class);
            JsonArray blocksArray = rootJson.getAsJsonArray("blocks");

            int restoredBlocks = 0;
            for (var elem : blocksArray) {
                JsonObject bObj = elem.getAsJsonObject();
                int x = bObj.get("x").getAsInt();
                int y = bObj.get("y").getAsInt();
                int z = bObj.get("z").getAsInt();
                String blockId = bObj.get("block").getAsString();

                BlockPos bPos = new BlockPos(x, y, z);
                var block = BuiltInRegistries.BLOCK.get(ResourceLocation.parse(blockId));

                if (block != null) {
                    level.setBlock(bPos, block.defaultBlockState(), 2);
                    if (bObj.has("nbt")) {
                        try {
                            CompoundTag tag = TagParser.parseTag(bObj.get("nbt").getAsString());
                            BlockEntity be = level.getBlockEntity(bPos);
                            if (be != null) {
                                be.loadWithComponents(tag, level.registryAccess());
                                be.setChanged();
                            }
                        } catch (Exception ignored) {}
                    }
                    restoredBlocks++;
                }
            }

            if (chunk != null) {
                refreshChunkForPlayers(level, chunk);
            }

            callback.send("§a[FXCore] Chunk (" + pos.x + ", " + pos.z + ") restaurada com sucesso!");
            callback.send("§7 Total de blocos restaurados: §e" + restoredBlocks);

        } catch (Exception e) {
            callback.send("§c[FXCore] Falha ao restaurar o backup: " + e.getMessage());
        }
    }

    /**
     * Regera a chunk limpando o cache atual e forçando a recriação pelo servidor.
     */
    public static void regenerateChunk(ServerLevel level, ChunkPos pos, CommandFeedback callback) {
        teleportPlayersOut(level, pos);

        // Remove entidades da chunk (exceto jogadores)
        List<Entity> entitiesToRemove = new ArrayList<>();
        for (Entity entity : level.getAllEntities()) {
            if (entity != null && !entity.isRemoved() && entity.chunkPosition().equals(pos) && !(entity instanceof Player)) {
                entitiesToRemove.add(entity);
            }
        }
        entitiesToRemove.forEach(Entity::discard);

        try {
            net.minecraft.server.level.ServerChunkCache chunkCache = level.getChunkSource();
            
            // Salva o estado atual e atualiza o cache
            chunkCache.save(false);

            // Força a obtenção/recriação da chunk utilizando o cache do servidor
            net.minecraft.world.level.chunk.LevelChunk newChunk = (net.minecraft.world.level.chunk.LevelChunk) chunkCache.getChunk(pos.x, pos.z, ChunkStatus.FULL, true);

            if (newChunk != null) {
                newChunk.setUnsaved(true);
                refreshChunkForPlayers(level, newChunk);
            }

            callback.send("§a[FXCore] Chunk (" + pos.x + ", " + pos.z + ") regerada com sucesso!");
        } catch (Exception e) {
            callback.send("§c[FXCore] Erro ao regerar a chunk: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private static void refreshChunkForPlayers(ServerLevel level, LevelChunk chunk) {
        ChunkPos pos = chunk.getPos();
        ClientboundLevelChunkWithLightPacket packet = new ClientboundLevelChunkWithLightPacket(chunk, level.getLightEngine(), null, null);

        for (ServerPlayer player : level.players()) {
            if (player.chunkPosition().getChessboardDistance(pos) <= 8) {
                player.connection.send(packet);
            }
        }
    }

    private static void teleportPlayersOut(ServerLevel level, ChunkPos pos) {
        for (Player player : level.players()) {
            if (player.chunkPosition().equals(pos)) {
                double safeY = level.getHeightmapPos(net.minecraft.world.level.levelgen.Heightmap.Types.WORLD_SURFACE, player.blockPosition()).getY() + 2;
                player.teleportTo(player.getX() + 16, safeY, player.getZ() + 16);
                player.sendSystemMessage(net.minecraft.network.chat.Component.literal("§c[FXCore] Você foi movido pois a chunk onde estava entrou em manutenção."));
            }
        }
    }

    private static void ensureBackupDirExists() {
        if (!BACKUP_DIR.exists()) {
            BACKUP_DIR.mkdirs();
        }
    }

    @FunctionalInterface
    public interface CommandFeedback {
        void send(String message);
    }
}