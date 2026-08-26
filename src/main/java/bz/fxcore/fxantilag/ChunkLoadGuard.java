package bz.fxcore.fxantilag;

import net.minecraft.world.level.ChunkPos;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.level.ChunkEvent;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class ChunkLoadGuard {

    private static final Map<Long, Long> CHUNK_LOAD_COOLDOWN = new ConcurrentHashMap<>();

    @SubscribeEvent
    public static void onChunkLoad(ChunkEvent.Load event) {
        // Se a função estiver desativada na config ou for lado do cliente, ignora
        if (!FXAntiLagConfig.DATA.enableChunkLoadDelay || event.getLevel().isClientSide()) {
            return;
        }

        ChunkPos pos = event.getChunk().getPos();
        long chunkKey = pos.toLong();
        long now = System.currentTimeMillis();

        Long lastLoadTime = CHUNK_LOAD_COOLDOWN.get(chunkKey);
        long delayLimit = FXAntiLagConfig.DATA.chunkLoadDelayMs;

        if (lastLoadTime != null && (now - lastLoadTime) < delayLimit) {
            CHUNK_LOAD_COOLDOWN.put(chunkKey, now);
            return;
        }

        CHUNK_LOAD_COOLDOWN.put(chunkKey, now);
    }
}