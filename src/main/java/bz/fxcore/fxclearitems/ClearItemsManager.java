package bz.fxcore.fxclearitems;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;

public class ClearItemsManager {

    private static int timerTicks = 0;

    public static void onServerTick(MinecraftServer server) {
        if (!ClearItemsConfig.DATA.enabled) return;

        timerTicks++;
        int intervalTicks = ClearItemsConfig.DATA.intervalSeconds * 20;
        int remainingTicks = intervalTicks - timerTicks;
        int remainingSeconds = remainingTicks / 20;

        if (remainingTicks % 20 == 0 && ClearItemsConfig.DATA.warningTimes.contains(remainingSeconds)) {
            String msg = ClearItemsConfig.DATA.messages.warningMessage.replace("{seconds}", String.valueOf(remainingSeconds));
            server.getPlayerList().broadcastSystemMessage(Component.literal(msg), false);
        }

        if (timerTicks >= intervalTicks) {
            executarLimpeza(server);
            timerTicks = 0;
        }
    }

    public static int executarLimpeza(MinecraftServer server) {
        int totalRemovido = 0;

        for (ServerLevel level : server.getAllLevels()) {
            for (Entity entity : level.getAllEntities()) {
                if (entity instanceof ItemEntity itemEntity) {
                    ItemStack stack = itemEntity.getItem();
                    String itemId = BuiltInRegistries.ITEM.getKey(stack.getItem()).toString();

                    if (!ClearItemsConfig.DATA.whitelist.contains(itemId)) {
                        itemEntity.discard();
                        totalRemovido++;
                    }
                }
            }
        }

        String msg = ClearItemsConfig.DATA.messages.clearedMessage.replace("{count}", String.valueOf(totalRemovido));
        server.getPlayerList().broadcastSystemMessage(Component.literal(msg), false);

        return totalRemovido;
    }

    public static int getSegundosRestantes() {
        int intervalTicks = ClearItemsConfig.DATA.intervalSeconds * 20;
        return (intervalTicks - timerTicks) / 20;
    }

    public static void resetTimer() {
        timerTicks = 0;
    }
}