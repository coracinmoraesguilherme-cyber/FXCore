package bz.fxcore.fxunify;

import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class FXUnifyManager {

    public static final Map<UUID, List<StoredDrop>> RECOVERABLE_DROPS = new ConcurrentHashMap<>();

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

    public static void addDrop(UUID playerUUID, ItemStack stack, long amount) {
        if (stack.isEmpty() || amount <= 0) return;

        List<StoredDrop> drops = RECOVERABLE_DROPS.computeIfAbsent(playerUUID, k -> new ArrayList<>());

        for (StoredDrop drop : drops) {
            if (ItemStack.isSameItemSameComponents(drop.item, stack)) {
                // Aplica a trava de 256K (ou o limite definido na config)
                long maxAllowed = FXUnifyConfig.DATA.maxItemsPerType;
                if (drop.count < maxAllowed) {
                    drop.count = Math.min(maxAllowed, drop.count + amount);
                }
                return;
            }
        }

        // Novo item adicionado respeitando o teto
        long initialAmount = Math.min(FXUnifyConfig.DATA.maxItemsPerType, amount);
        drops.add(new StoredDrop(stack.copy(), initialAmount));
    }

    public static void cleanExpiredDrops(UUID playerUUID) {
        List<StoredDrop> drops = RECOVERABLE_DROPS.get(playerUUID);
        if (drops == null) return;

        long retentionMillis = FXUnifyConfig.DATA.retentionTimeMinutes * 60L * 1000L;
        long now = System.currentTimeMillis();

        drops.removeIf(drop -> (now - drop.timestamp) > retentionMillis);

        if (drops.isEmpty()) {
            RECOVERABLE_DROPS.remove(playerUUID);
        }
    }

    public static Component parseColor(String text) {
        return Component.literal(text.replace("&", "§"));
    }
}