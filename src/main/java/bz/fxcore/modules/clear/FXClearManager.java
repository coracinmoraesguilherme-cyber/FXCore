package bz.fxcore.modules.clear;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.resources.ResourceLocation;

public class FXClearManager {

    public static int clearEntities(MinecraftServer server) {
        int removedCount = 0;

        for (ServerLevel level : server.getAllLevels()) {
            for (Entity entity : level.getAllEntities()) {
                ResourceLocation entityTypeId = BuiltInRegistries.ENTITY_TYPE.getKey(entity.getType());
                String entityTypeStr = entityTypeId.toString();

                if (FXClearConfig.DATA.entityTargetList.contains(entityTypeStr)) {
                    if (entity instanceof ItemEntity itemEntity) {
                        ItemStack stack = itemEntity.getItem();
                        ResourceLocation itemId = BuiltInRegistries.ITEM.getKey(stack.getItem());
                        String itemStr = itemId.toString();

                        if (FXClearConfig.DATA.itemWhitelist.contains(itemStr)) {
                            continue;
                        }
                    }

                    entity.discard();
                    removedCount++;
                }
            }
        }

        return removedCount;
    }
}