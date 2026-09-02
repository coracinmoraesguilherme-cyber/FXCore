package bz.fxcore.modules.giveback;

import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.ShulkerBoxBlock;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;

@EventBusSubscriber(modid = "fxcore")
public class GiveBItemEvents {

    // 1. Torna a Shulker / Mochila invulnerável a qualquer dano e impede o despawn (5 min)
    @SubscribeEvent
    public static void onItemSpawn(EntityJoinLevelEvent event) {
        if (event.getLevel().isClientSide()) return;

        if (event.getEntity() instanceof ItemEntity itemEntity) {
            ItemStack stack = itemEntity.getItem();

            if (!stack.isEmpty() && isShulkerOrBackpackItem(stack)) {
                // Torna o item 100% imune a dano (lava, cacto, fogo, explosão, etc.) no motor Vanilla
                itemEntity.setInvulnerable(true);
                
                // Impede o despawn do item após 5 minutos no chão
                itemEntity.lifespan = Integer.MAX_VALUE;
            }
        }
    }

    private static boolean isShulkerOrBackpackItem(ItemStack stack) {
        if (stack.getItem() instanceof BlockItem blockItem && blockItem.getBlock() instanceof ShulkerBoxBlock) {
            return true;
        }
        String name = stack.getItem().toString().toLowerCase();
        return name.contains("shulker") || name.contains("backpack") || name.contains("chest");
    }
}