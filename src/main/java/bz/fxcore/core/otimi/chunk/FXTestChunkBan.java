package bz.fxcore.core.otimi.chunk;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.monster.Slime;
import net.minecraft.world.entity.vehicle.MinecartHopper;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.ObserverBlock;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.chunk.status.ChunkStatus;
import net.minecraft.world.level.levelgen.Heightmap;

public class FXTestChunkBan {

    /**
     * NÍVEL APOCALÍPTICO / DESTRUIÇÃO DE TICK (EXOTEST 1.21.1)
     */
    public static void spawnTestChunkBan(ServerLevel level, ChunkPos pos, FXChunkBan.CommandFeedback callback) {
        LevelChunk chunk = (LevelChunk) level.getChunk(pos.x, pos.z, ChunkStatus.FULL, true);

        if (chunk == null) {
            callback.send("§c[FXCore] Não foi possível carregar a chunk.");
            return;
        }

        int startY = Math.max(level.getMinBuildHeight() + 10, getSurfaceY(level, pos));
        int maxY = Math.min(startY + 30, level.getMaxBuildHeight() - 10);

        int hopperCount = 0;
        int observerCount = 0;

        // 1. GRADE 3D MACIÇA DE HOPPERS E REDSTONE (30 Camadas Verticais x 16x16)
        for (int y = startY; y < maxY; y++) {
            for (int x = 0; x < 16; x++) {
                for (int z = 0; z < 16; z++) {
                    BlockPos bPos = new BlockPos(pos.getMinBlockX() + x, y, pos.getMinBlockZ() + z);

                    if ((x + y + z) % 3 == 0) {
                        level.setBlock(bPos, Blocks.HOPPER.defaultBlockState(), 3);
                        hopperCount++;
                    } else if ((x + y + z) % 3 == 1) {
                        level.setBlock(bPos, Blocks.OBSERVER.defaultBlockState().setValue(ObserverBlock.FACING, Direction.UP), 3);
                        observerCount++;
                    } else {
                        level.setBlock(bPos, Blocks.FURNACE.defaultBlockState(), 3);
                    }
                }
            }
        }

        // 2. MINE CART HOPPER STRESS (400 Carrinhos com Hopper no mesmo ponto)
        BlockPos center = new BlockPos(pos.getMinBlockX() + 8, startY + 32, pos.getMinBlockZ() + 8);
        level.setBlock(center.below(), Blocks.BEDROCK.defaultBlockState(), 3);

        int minecartCount = 400;
        for (int i = 0; i < minecartCount; i++) {
            MinecartHopper cart = new MinecartHopper(level, center.getX() + 0.5, center.getY(), center.getZ() + 0.5);
            cart.setCustomName(Component.literal("§c[LagCart #" + i + "]"));
            level.addFreshEntity(cart);
        }

        // 3. ITEM ENTITY SPAM (1.000 Itens Únicos via DataComponents na 1.21.1)
        int itemCount = 1000;
        for (int i = 0; i < itemCount; i++) {
            ItemStack stack = new ItemStack(Items.DIAMOND_SWORD);
            
            // Injeta dados no componente CUSTOM_DATA para impedir o stack na 1.21.1
            CompoundTag customTag = new CompoundTag();
            customTag.putInt("UniqueUUIDLag", i);
            stack.set(DataComponents.CUSTOM_DATA, CustomData.of(customTag));

            ItemEntity itemEntity = new ItemEntity(level, center.getX() + 0.5, center.getY() + 1, center.getZ() + 0.5, stack);
            itemEntity.setPickUpDelay(32767);
            itemEntity.setNeverPickUp();
            level.addFreshEntity(itemEntity);
        }

        // 4. SLIMES GIGANTES (Caixa de Colisão Gigante)
        for (int i = 0; i < 30; i++) {
            Slime slime = new Slime(EntityType.SLIME, level);
            slime.setPos(center.getX() + 0.5, center.getY() + 2, center.getZ() + 0.5);
            slime.setSize(16, true);
            slime.setPersistenceRequired();
            level.addFreshEntity(slime);
        }

        callback.send("§4§l[FXCore NUKER 1.21.1] CHUNK BAN CRÍTICA GERADA!");
        callback.send("§7 -> Bloco Maciço 3D: §e" + hopperCount + " Hoppers §7& §e" + observerCount + " Observers");
        callback.send("§7 -> Minecarts com Hopper: §c" + minecartCount);
        callback.send("§7 -> Entities de Itens Unstackable: §c" + itemCount);
        callback.send("§c Teste com /fxcore lagview e depois faça o /fxcore chunkban delete!");
    }

    private static int getSurfaceY(ServerLevel level, ChunkPos pos) {
        return level.getHeightmapPos(
            Heightmap.Types.WORLD_SURFACE,
            new BlockPos(pos.getMinBlockX() + 8, 0, pos.getMinBlockZ() + 8)
        ).getY();
    }
}