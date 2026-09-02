package bz.fxcore.modules.build;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.LeavesBlock;
import net.minecraft.world.level.block.LeverBlock;
import net.minecraft.world.level.block.WallTorchBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.AttachFace;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

@EventBusSubscriber
public class FXBuildManager {

    private static final Set<UUID> BUILD_MODE_PLAYERS = new HashSet<>();

    public static boolean isBuildMode(ServerPlayer player) {
        return BUILD_MODE_PLAYERS.contains(player.getUUID());
    }

    public static void toggleBuildMode(ServerPlayer player, boolean enable) {
        UUID uuid = player.getUUID();
        if (enable) {
            BUILD_MODE_PLAYERS.add(uuid);
            player.sendSystemMessage(net.minecraft.network.chat.Component.literal(FXBuildConfig.DATA.buildEnableMsg));
        } else {
            BUILD_MODE_PLAYERS.remove(uuid);
            player.sendSystemMessage(net.minecraft.network.chat.Component.literal(FXBuildConfig.DATA.buildDisableMsg));
        }
    }

    @SubscribeEvent
    public static void onPlayerLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            BUILD_MODE_PLAYERS.remove(player.getUUID());
        }
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onRightClickBlock(PlayerInteractEvent.RightClickBlock event) {
        if (!FXBuildConfig.DATA.enableBuildModule) return;
        if (event.getLevel().isClientSide()) return;

        if (event.getEntity() instanceof ServerPlayer player && player.hasPermissions(2)) {
            if (!isBuildMode(player)) return;

            ItemStack heldItem = player.getItemInHand(event.getHand());

            if (heldItem.getItem() instanceof BlockItem blockItem) {
                ServerLevel level = (ServerLevel) event.getLevel();
                Direction clickedFace = event.getFace();
                
                BlockPos targetPos = event.getPos().relative(clickedFace);

                BlockState stateToPlace = getCustomDirectionState(heldItem, clickedFace, player);

                // Flag 82: Coloca o bloco de forma estática sem física ou updates nos vizinhos
                boolean placed = level.setBlock(targetPos, stateToPlace, 82);

                if (placed) {
                    event.setCanceled(true);
                    if (!player.isCreative()) {
                        heldItem.shrink(1);
                    }
                }
            }
        }
    }

    private static BlockState getCustomDirectionState(ItemStack heldItem, Direction face, ServerPlayer player) {
        Block block = ((BlockItem) heldItem.getItem()).getBlock();
        BlockState defaultState = block.defaultBlockState();

        if (block instanceof LeavesBlock && defaultState.hasProperty(LeavesBlock.PERSISTENT)) {
            defaultState = defaultState.setValue(LeavesBlock.PERSISTENT, true);
        }

        if (heldItem.is(Items.TORCH)) {
            if (face.getAxis().isHorizontal()) {
                return Blocks.WALL_TORCH.defaultBlockState().setValue(WallTorchBlock.FACING, face);
            }
            return Blocks.TORCH.defaultBlockState();
        }

        if (heldItem.is(Items.REDSTONE_TORCH)) {
            if (face.getAxis().isHorizontal()) {
                return Blocks.REDSTONE_WALL_TORCH.defaultBlockState().setValue(WallTorchBlock.FACING, face);
            }
            return Blocks.REDSTONE_TORCH.defaultBlockState();
        }

        if (heldItem.is(Items.LEVER)) {
            BlockState leverState = Blocks.LEVER.defaultBlockState();
            Direction playerFacing = player.getDirection();

            if (face == Direction.UP) {
                return leverState.setValue(LeverBlock.FACE, AttachFace.FLOOR).setValue(LeverBlock.FACING, playerFacing);
            } else if (face == Direction.DOWN) {
                return leverState.setValue(LeverBlock.FACE, AttachFace.CEILING).setValue(LeverBlock.FACING, playerFacing);
            } else {
                return leverState.setValue(LeverBlock.FACE, AttachFace.WALL).setValue(LeverBlock.FACING, face);
            }
        }

        if (defaultState.hasProperty(HorizontalDirectionalBlock.FACING)) {
            return defaultState.setValue(HorizontalDirectionalBlock.FACING, player.getDirection().getOpposite());
        }

        return defaultState;
    }
}