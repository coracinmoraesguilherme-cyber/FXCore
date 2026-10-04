package bz.fxcore.modules.giveback;

import bz.fxcore.FXCore;
import bz.fxcore.core.database.FXPlayerData;
import bz.fxcore.core.database.PlayerDataManager;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@EventBusSubscriber(modid = FXCore.MODID)
public class GiveBManager {

    public static final Map<UUID, List<StoredDrop>> RECOVERABLE_DROPS = new ConcurrentHashMap<>();
    private static int tickCounter = 0;

    public static class StoredDrop {
        public final ItemStack item;
        public long count;
        public final long timestamp;

        public StoredDrop(ItemStack item, long count) {
            this.item = item;
            this.count = count;
            this.timestamp = System.currentTimeMillis();
        }
    }

    @SubscribeEvent
    public static void onServerTick(ServerTickEvent.Post event) {
        if (++tickCounter % 100 == 0) { // Verifica a cada 5 segundos
            cleanAllExpiredDrops();
        }
    }

    public static void addDrop(UUID playerUUID, ItemStack stack, long amount) {
        if (stack.isEmpty() || amount <= 0) return;

        List<StoredDrop> drops = RECOVERABLE_DROPS.computeIfAbsent(playerUUID, k -> Collections.synchronizedList(new ArrayList<>()));

        synchronized (drops) {
            boolean updated = false;
            for (StoredDrop drop : drops) {
                if (ItemStack.isSameItemSameComponents(drop.item, stack)) {
                    long maxAllowed = GiveBConfig.DATA.maxItemsPerType;
                    if (drop.count < maxAllowed) {
                        drop.count = Math.min(maxAllowed, drop.count + amount);
                    }
                    updated = true;
                    break;
                }
            }

            if (!updated) {
                long initialAmount = Math.min(GiveBConfig.DATA.maxItemsPerType, amount);
                drops.add(new StoredDrop(stack.copy(), initialAmount));
            }
        }

        // Sincroniza a flag no FXPlayerData para aparecer no /fxs info
        updateGivebackStatus(playerUUID);
    }

    public static void cleanExpiredDrops(UUID playerUUID) {
        List<StoredDrop> drops = RECOVERABLE_DROPS.get(playerUUID);
        if (drops == null) return;

        long retentionMillis = GiveBConfig.DATA.retentionTimeMinutes * 60L * 1000L;
        long now = System.currentTimeMillis();

        synchronized (drops) {
            drops.removeIf(drop -> (now - drop.timestamp) > retentionMillis);

            if (drops.isEmpty()) {
                RECOVERABLE_DROPS.remove(playerUUID);
            }
        }

        // Atualiza a flag indicando se ainda restaram itens ativos
        updateGivebackStatus(playerUUID);
    }

    public static void cleanAllExpiredDrops() {
        if (RECOVERABLE_DROPS.isEmpty()) return;
        long retentionMillis = GiveBConfig.DATA.retentionTimeMinutes * 60L * 1000L;
        long now = System.currentTimeMillis();

        RECOVERABLE_DROPS.entrySet().removeIf(entry -> {
            List<StoredDrop> drops = entry.getValue();
            if (drops != null) {
                synchronized (drops) {
                    drops.removeIf(drop -> (now - drop.timestamp) > retentionMillis);
                    if (drops.isEmpty()) {
                        updateGivebackStatus(entry.getKey());
                        return true;
                    }
                }
            }
            return false;
        });
    }

    // Trava de segurança para ser chamada antes de abrir a GUI do Giveback ou resgatar itens
    public static boolean canAccessGiveback(ServerPlayer player) {
        FXPlayerData data = PlayerDataManager.get(player.getUUID());
        if (data != null && data.isFrozen) {
            player.sendSystemMessage(Component.literal("§c[FXCore] Você está congelado e não pode acessar o Giveback."));
            return false;
        }
        return true;
    }

    // Limpa os drops do jogador após o resgate total ou cancelamento da Staff
    public static void clearDrops(UUID playerUUID) {
        RECOVERABLE_DROPS.remove(playerUUID);
        updateGivebackStatus(playerUUID);
    }

    // Atualiza a flag na classe de dados da Staff / Player Data
    public static void updateGivebackStatus(UUID playerUUID) {
        List<StoredDrop> drops = RECOVERABLE_DROPS.get(playerUUID);
        boolean hasActiveDrops = drops != null && !drops.isEmpty();

        FXPlayerData data = PlayerDataManager.get(playerUUID);
        if (data != null) {
            data.hasGivebackActive = hasActiveDrops;
        }
    }

    public static Component parseColor(String text) {
        return Component.literal(text.replace("&", "§"));
    }
}