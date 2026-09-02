package bz.fxcore.modules.giveback;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Container;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.ShulkerBoxBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.event.level.BlockEvent;
import net.neoforged.neoforge.event.level.ExplosionEvent;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.IItemHandlerModifiable;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@EventBusSubscriber(modid = "fxcore")
public class GiveBBlockEvents {

    private static final String OWNER_NBT_KEY = "GiveBack_OwnerUUID";

    @SubscribeEvent
    public static void onBlockPlace(BlockEvent.EntityPlaceEvent event) {
        if (event.getLevel().isClientSide()) return;

        if (event.getEntity() instanceof ServerPlayer player) {
            BlockEntity blockEntity = event.getLevel().getBlockEntity(event.getPos());
            if (blockEntity != null) {
                blockEntity.getPersistentData().putUUID(OWNER_NBT_KEY, player.getUUID());
            }
        }
    }

    @SubscribeEvent
    public static void onBlockBreak(BlockEvent.BreakEvent event) {
        if (!(event.getLevel() instanceof ServerLevel serverLevel) || !(event.getPlayer() instanceof ServerPlayer player)) {
            return;
        }

        BlockPos pos = event.getPos();
        processContainerProtection(serverLevel, pos, player, "Quebrado pelo jogador");
    }

    @SubscribeEvent
    public static void onExplosionDetonate(ExplosionEvent.Detonate event) {
        if (!(event.getLevel() instanceof ServerLevel serverLevel)) return;

        for (BlockPos pos : event.getAffectedBlocks()) {
            processContainerProtection(serverLevel, pos, null, "Explosão");
        }
    }

    private static void processContainerProtection(ServerLevel serverLevel, BlockPos pos, ServerPlayer breakerPlayer, String reason) {
        BlockEntity blockEntity = serverLevel.getBlockEntity(pos);
        if (blockEntity == null || isShulkerOrBackpack(blockEntity)) return;

        UUID targetOwnerUUID = null;
        if (breakerPlayer != null) {
            targetOwnerUUID = breakerPlayer.getUUID();
        } else if (blockEntity.getPersistentData().contains(OWNER_NBT_KEY)) {
            targetOwnerUUID = blockEntity.getPersistentData().getUUID(OWNER_NBT_KEY);
        }

        IItemHandler handler = serverLevel.getCapability(Capabilities.ItemHandler.BLOCK, pos, null);
        if (handler == null) return;

        long totalItems = 0;
        Map<ItemStackHolder, Long> consolidatedDrops = new HashMap<>();

        for (int i = 0; i < handler.getSlots(); i++) {
            ItemStack stack = handler.getStackInSlot(i);
            if (!stack.isEmpty()) {
                int count = stack.getCount();
                totalItems = (totalItems < 0 || totalItems + count < 0) ? Long.MAX_VALUE : totalItems + count;

                ItemStackHolder holder = new ItemStackHolder(stack);
                consolidatedDrops.put(holder, consolidatedDrops.getOrDefault(holder, 0L) + count);
            }
        }

        if (totalItems >= GiveBConfig.DATA.maxItemsBeforeTransfer) {
            if (targetOwnerUUID != null) {
                for (Map.Entry<ItemStackHolder, Long> entry : consolidatedDrops.entrySet()) {
                    GiveBManager.addDrop(targetOwnerUUID, entry.getKey().stack, entry.getValue());
                }

                ServerPlayer recipient = serverLevel.getServer().getPlayerList().getPlayer(targetOwnerUUID);
                if (recipient != null) {
                    String msg = GiveBConfig.DATA.lagProtectionMessage.replace("%count%", String.valueOf(totalItems));
                    recipient.sendSystemMessage(GiveBManager.parseColor(msg));
                }
            }

            for (int i = 0; i < handler.getSlots(); i++) {
                if (!handler.getStackInSlot(i).isEmpty()) {
                    if (handler instanceof IItemHandlerModifiable modifiable) {
                        modifiable.setStackInSlot(i, ItemStack.EMPTY);
                    } else {
                        handler.extractItem(i, handler.getStackInSlot(i).getCount(), false);
                    }
                }
            }

            if (blockEntity instanceof Container container) {
                container.clearContent();
            }

            notifyStaff(serverLevel, breakerPlayer, pos, reason, totalItems);
        }
    }

    private static boolean isShulkerOrBackpack(BlockEntity entity) {
        if (entity.getBlockState().getBlock() instanceof ShulkerBoxBlock) {
            return true;
        }
        String className = entity.getClass().getName().toLowerCase();
        return className.contains("shulker") || className.contains("backpack") || className.contains("portable");
    }

    private static void notifyStaff(ServerLevel level, ServerPlayer player, BlockPos pos, String reason, long count) {
        String alertMsg = (player != null)
            ? GiveBConfig.DATA.staffAlertMessage.replace("%player%", player.getScoreboardName()).replace("%count%", String.valueOf(count))
            : GiveBConfig.DATA.staffWorldAlertMessage.replace("%pos%", pos.getX() + ", " + pos.getY() + ", " + pos.getZ()).replace("%reason%", reason).replace("%count%", String.valueOf(count));

        for (ServerPlayer online : level.getServer().getPlayerList().getPlayers()) {
            if (online.hasPermissions(2)) {
                online.sendSystemMessage(GiveBManager.parseColor(alertMsg));
            }
        }
    }

    private record ItemStackHolder(ItemStack stack) {
        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof ItemStackHolder other)) return false;
            return ItemStack.isSameItemSameComponents(this.stack, other.stack);
        }

        @Override
        public int hashCode() {
            return ItemStack.hashItemAndComponents(this.stack);
        }
    }
}