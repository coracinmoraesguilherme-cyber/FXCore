package bz.fxcore.modules.clear;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.resources.ResourceLocation;

import java.util.ArrayList;
import java.util.List;

public class FXClearManager {

    public static int clearEntities(MinecraftServer server) {
        int removedCount = 0;
        if (server == null) return 0;

        for (ServerLevel level : server.getAllLevels()) {
            if (level == null) continue;

            List<Entity> toDiscard = new ArrayList<>();

            for (Entity entity : level.getAllEntities()) {
                if (entity == null || entity.isRemoved()) {
                    continue;
                }

                if (entity.getType() == null) {
                    continue;
                }

                ResourceLocation entityTypeId = BuiltInRegistries.ENTITY_TYPE.getKey(entity.getType());
                if (entityTypeId == null) {
                    continue;
                }
                String entityTypeStr = entityTypeId.toString();

                if (FXClearConfig.DATA.entityTargetList != null && FXClearConfig.DATA.entityTargetList.contains(entityTypeStr)) {
                    if (entity instanceof ItemEntity itemEntity) {
                        ItemStack stack = itemEntity.getItem();
                        if (stack != null && !stack.isEmpty()) {
                            ResourceLocation itemId = BuiltInRegistries.ITEM.getKey(stack.getItem());
                            if (itemId != null && FXClearConfig.DATA.itemWhitelist != null && FXClearConfig.DATA.itemWhitelist.contains(itemId.toString())) {
                                continue;
                            }
                        }
                    }

                    toDiscard.add(entity);
                }
            }

            for (Entity entity : toDiscard) {
                if (entity != null && !entity.isRemoved()) {
                    entity.discard();
                    removedCount++;
                }
            }
        }

        return removedCount;
    }
}