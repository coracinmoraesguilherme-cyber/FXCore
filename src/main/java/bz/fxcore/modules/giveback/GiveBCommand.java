package bz.fxcore.modules.giveback;

import bz.fxcore.core.database.PlayerDataManager;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ChestMenu;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import java.util.List;
import java.util.UUID;

public class GiveBCommand {

    private static final int ITEMS_PER_PAGE = 45; // 5 linhas para itens, 1 linha para botões

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("fxgiveback")
            .executes(context -> {
                if (!(context.getSource().getEntity() instanceof ServerPlayer player)) {
                    return 0;
                }
                return openForPlayer(player, player.getUUID(), player.getScoreboardName(), 0);
            })
            .then(Commands.literal("view")
                .requires(source -> source.hasPermission(2))
                .then(Commands.argument("target", StringArgumentType.word())
                    .executes(context -> {
                        ServerPlayer staff = context.getSource().getPlayerOrException();
                        String targetName = StringArgumentType.getString(context, "target");
                        MinecraftServer server = context.getSource().getServer();
                        
                        UUID targetUuid = PlayerDataManager.getUUIDByName(server, targetName);
                        if (targetUuid == null) {
                            staff.sendSystemMessage(Component.literal("§c[FXCore] O jogador '" + targetName + "' nunca entrou no servidor."));
                            return 0;
                        }

                        return openForPlayer(staff, targetUuid, targetName, 0);
                    })
                )
            )
            .then(Commands.literal("clear")
                .requires(source -> source.hasPermission(2))
                .then(Commands.argument("target", StringArgumentType.word())
                    .executes(context -> {
                        String targetName = StringArgumentType.getString(context, "target");
                        MinecraftServer server = context.getSource().getServer();
                        
                        UUID targetUuid = PlayerDataManager.getUUIDByName(server, targetName);
                        if (targetUuid == null) {
                            context.getSource().sendFailure(Component.literal("§c[FXCore] O jogador '" + targetName + "' nunca entrou no servidor."));
                            return 0;
                        }

                        GiveBManager.RECOVERABLE_DROPS.remove(targetUuid);
                        
                        String clearMsg = GiveBConfig.DATA.staffClearSuccessMessage
                            .replace("%player%", targetName);
                            
                        context.getSource().sendSuccess(() -> GiveBManager.parseColor(clearMsg), true);
                        return 1;
                    })
                )
            )
        );
    }

    private static int openForPlayer(ServerPlayer viewer, UUID ownerUuid, String ownerName, int page) {
        GiveBManager.cleanExpiredDrops(ownerUuid);
        List<GiveBManager.StoredDrop> drops = GiveBManager.RECOVERABLE_DROPS.get(ownerUuid);

        if (drops == null || drops.isEmpty()) {
            boolean isSelf = viewer.getUUID().equals(ownerUuid);
            String msg = isSelf
                ? GiveBConfig.DATA.emptyInventoryMessage
                : GiveBConfig.DATA.staffNoItemsMessage.replace("%player%", ownerName);

            viewer.sendSystemMessage(GiveBManager.parseColor(msg));
            return 1;
        }

        openRecoveryMenu(viewer, ownerUuid, ownerName, drops, page);
        return 1;
    }

    private static void openRecoveryMenu(ServerPlayer viewer, UUID ownerUuid, String ownerName, List<GiveBManager.StoredDrop> drops, int page) {
        int maxPages = (int) Math.ceil((double) drops.size() / ITEMS_PER_PAGE);
        int currentPage = Math.max(0, Math.min(page, maxPages - 1));

        SimpleContainer container = new SimpleContainer(54);
        int startIndex = currentPage * ITEMS_PER_PAGE;
        int endIndex = Math.min(startIndex + ITEMS_PER_PAGE, drops.size());

        // Preenche os itens da página atual
        for (int i = startIndex; i < endIndex; i++) {
            GiveBManager.StoredDrop drop = drops.get(i);
            ItemStack displayStack = drop.item.copyWithCount((int) Math.min(drop.count, 64L));

            displayStack.set(DataComponents.CUSTOM_NAME, 
                GiveBManager.parseColor("&a" + drop.item.getHoverName().getString() + " &7(x" + drop.count + ")"));

            container.setItem(i - startIndex, displayStack);
        }

        // Painel de navegação (Linha inferior: slots 45 a 53)
        if (currentPage > 0) {
            ItemStack prev = new ItemStack(Items.ARROW);
            prev.set(DataComponents.CUSTOM_NAME, GiveBManager.parseColor("&e← Página Anterior"));
            container.setItem(45, prev);
        }

        ItemStack info = new ItemStack(Items.PAPER);
        info.set(DataComponents.CUSTOM_NAME, GiveBManager.parseColor("&fPágina &a" + (currentPage + 1) + "&f de &a" + maxPages));
        container.setItem(49, info);

        if (currentPage < maxPages - 1) {
            ItemStack next = new ItemStack(Items.ARROW);
            next.set(DataComponents.CUSTOM_NAME, GiveBManager.parseColor("&ePróxima Página →"));
            container.setItem(53, next);
        }

        boolean isSelf = viewer.getUUID().equals(ownerUuid);
        String rawTitle = isSelf 
            ? GiveBConfig.DATA.playerGuiTitle 
            : GiveBConfig.DATA.staffGuiTitle.replace("%player%", ownerName);

        rawTitle = rawTitle.replace("%page%", String.valueOf(currentPage + 1))
                           .replace("%max%", String.valueOf(maxPages));

        Component title = GiveBManager.parseColor(rawTitle);

        viewer.openMenu(new net.minecraft.world.SimpleMenuProvider(
            (containerId, playerInventory, p) -> new ChestMenu(MenuType.GENERIC_9x6, containerId, playerInventory, container, 6) {
                @Override
                public void clicked(int slotId, int button, net.minecraft.world.inventory.ClickType clickType, Player clickPlayer) {
                    if (slotId >= 0 && slotId < 54) {
                        
                        // Botão Página Anterior
                        if (slotId == 45 && currentPage > 0) {
                            openRecoveryMenu((ServerPlayer) clickPlayer, ownerUuid, ownerName, drops, currentPage - 1);
                            return;
                        }
                        
                        // Botão Próxima Página
                        if (slotId == 53 && currentPage < maxPages - 1) {
                            openRecoveryMenu((ServerPlayer) clickPlayer, ownerUuid, ownerName, drops, currentPage + 1);
                            return;
                        }

                        // Clique em um item recuperável (Apenas o próprio dono pode resgatar para o inventário)
                        if (slotId < ITEMS_PER_PAGE) {
                            if (!viewer.getUUID().equals(ownerUuid)) {
                                viewer.sendSystemMessage(Component.literal("§c[FXCore] Você está apenas visualizando o inventário deste jogador offline/online."));
                                return;
                            }

                            int realIndex = startIndex + slotId;
                            if (realIndex < drops.size()) {
                                GiveBManager.StoredDrop drop = drops.get(realIndex);
                                
                                long toGiveCount = drop.count;
                                while (toGiveCount > 0) {
                                    int amount = (int) Math.min(toGiveCount, (long) drop.item.getMaxStackSize());
                                    ItemStack giveStack = drop.item.copyWithCount(amount);

                                    if (clickPlayer.getInventory().add(giveStack)) {
                                        int given = amount - giveStack.getCount();
                                        toGiveCount -= given;
                                        if (given == 0) break;
                                    } else {
                                        break;
                                    }
                                }

                                drop.count = toGiveCount;
                                if (drop.count <= 0) {
                                    drops.remove(realIndex);
                                }

                                openRecoveryMenu((ServerPlayer) clickPlayer, ownerUuid, ownerName, drops, currentPage);
                                return;
                            }
                        }
                        return;
                    }
                    super.clicked(slotId, button, clickType, clickPlayer);
                }
            },
            title
        ));
    }
}