package bz.fxcore.fxunify;

import com.mojang.brigadier.CommandDispatcher;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ChestMenu;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.RegisterCommandsEvent;

import java.util.List;

public class FXUnifyCommand {

    private static final int ITEMS_PER_PAGE = 45; // 5 linhas para itens, 1 linha para botões

    @SubscribeEvent
    public static void onRegisterCommands(RegisterCommandsEvent event) {
        register(event.getDispatcher());
    }

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("fxgiveback")
            .executes(context -> {
                if (!(context.getSource().getEntity() instanceof ServerPlayer player)) {
                    return 0;
                }
<<<<<<< Updated upstream:src/main/java/bz/fxcore/fxunify/FXUnifyCommand.java
                return openForPlayer(player, player, 0);
=======
                if (!GiveBManager.canAccessGiveback(player)) {
                    return 0;
                }
                return openForPlayer(player, player.getUUID(), player.getScoreboardName(), 0);
>>>>>>> Stashed changes:src/main/java/bz/fxcore/modules/giveback/GiveBCommand.java
            })
            .then(Commands.literal("view")
                .requires(source -> source.hasPermission(2))
                .then(Commands.argument("target", EntityArgument.player())
                    .executes(context -> {
                        ServerPlayer staff = context.getSource().getPlayerOrException();
                        ServerPlayer target = EntityArgument.getPlayer(context, "target");
                        return openForPlayer(staff, target, 0);
                    })
                )
            )
            .then(Commands.literal("clear")
                .requires(source -> source.hasPermission(2))
                .then(Commands.argument("target", EntityArgument.player())
                    .executes(context -> {
                        ServerPlayer target = EntityArgument.getPlayer(context, "target");
                        FXUnifyManager.RECOVERABLE_DROPS.remove(target.getUUID());
                        
<<<<<<< Updated upstream:src/main/java/bz/fxcore/fxunify/FXUnifyCommand.java
                        String clearMsg = FXUnifyConfig.DATA.staffClearSuccessMessage
                            .replace("%player%", target.getScoreboardName());
=======
                        UUID targetUuid = PlayerDataManager.getUUIDByName(server, targetName);
                        if (targetUuid == null) {
                            context.getSource().sendFailure(Component.literal("§c[FXCore] O jogador '" + targetName + "' nunca entrou no servidor."));
                            return 0;
                        }

                        GiveBManager.clearDrops(targetUuid);
                        
                        String clearMsg = GiveBConfig.DATA.staffClearSuccessMessage
                            .replace("%player%", targetName);
>>>>>>> Stashed changes:src/main/java/bz/fxcore/modules/giveback/GiveBCommand.java
                            
                        context.getSource().sendSuccess(() -> FXUnifyManager.parseColor(clearMsg), true);
                        return 1;
                    })
                )
            )
        );
    }

    private static int openForPlayer(ServerPlayer viewer, ServerPlayer owner, int page) {
        FXUnifyManager.cleanExpiredDrops(owner.getUUID());
        List<FXUnifyManager.StoredDrop> drops = FXUnifyManager.RECOVERABLE_DROPS.get(owner.getUUID());

        if (drops == null || drops.isEmpty()) {
            String msg = viewer.getUUID().equals(owner.getUUID())
                ? FXUnifyConfig.DATA.emptyInventoryMessage
                : FXUnifyConfig.DATA.staffNoItemsMessage.replace("%player%", owner.getScoreboardName());

            viewer.sendSystemMessage(FXUnifyManager.parseColor(msg));
            return 1;
        }

        openRecoveryMenu(viewer, owner, drops, page);
        return 1;
    }

<<<<<<< Updated upstream:src/main/java/bz/fxcore/fxunify/FXUnifyCommand.java
    private static void openRecoveryMenu(ServerPlayer viewer, ServerPlayer owner, List<FXUnifyManager.StoredDrop> drops, int page) {
        int maxPages = (int) Math.ceil((double) drops.size() / ITEMS_PER_PAGE);
        int currentPage = Math.max(0, Math.min(page, maxPages - 1));

        SimpleContainer container = new SimpleContainer(54);
=======
    private static void populateContainer(SimpleContainer container, List<GiveBManager.StoredDrop> drops, int currentPage, int maxPages) {
        container.clearContent();
>>>>>>> Stashed changes:src/main/java/bz/fxcore/modules/giveback/GiveBCommand.java
        int startIndex = currentPage * ITEMS_PER_PAGE;
        int endIndex = Math.min(startIndex + ITEMS_PER_PAGE, drops.size());

        for (int i = startIndex; i < endIndex; i++) {
            FXUnifyManager.StoredDrop drop = drops.get(i);
            ItemStack displayStack = drop.item.copyWithCount((int) Math.min(drop.count, 64L));

            displayStack.set(DataComponents.CUSTOM_NAME, 
                FXUnifyManager.parseColor("&a" + drop.item.getHoverName().getString() + " &7(x" + drop.count + ")"));

            container.setItem(i - startIndex, displayStack);
        }

        if (currentPage > 0) {
            ItemStack prev = new ItemStack(Items.ARROW);
            prev.set(DataComponents.CUSTOM_NAME, FXUnifyManager.parseColor("&e← Página Anterior"));
            container.setItem(45, prev);
        }

        ItemStack info = new ItemStack(Items.PAPER);
        info.set(DataComponents.CUSTOM_NAME, FXUnifyManager.parseColor("&fPágina &a" + (currentPage + 1) + "&f de &a" + maxPages));
        container.setItem(49, info);

        if (currentPage < maxPages - 1) {
            ItemStack next = new ItemStack(Items.ARROW);
            next.set(DataComponents.CUSTOM_NAME, FXUnifyManager.parseColor("&ePróxima Página →"));
            container.setItem(53, next);
        }
    }

    private static void openRecoveryMenu(ServerPlayer viewer, UUID ownerUuid, String ownerName, List<GiveBManager.StoredDrop> drops, int page) {
        int maxPages = (int) Math.ceil((double) drops.size() / ITEMS_PER_PAGE);
        int currentPage = Math.max(0, Math.min(page, maxPages - 1));

        SimpleContainer container = new SimpleContainer(54);
        populateContainer(container, drops, currentPage, maxPages);

        String rawTitle = viewer.getUUID().equals(owner.getUUID()) 
            ? FXUnifyConfig.DATA.playerGuiTitle 
            : FXUnifyConfig.DATA.staffGuiTitle.replace("%player%", owner.getScoreboardName());

        rawTitle = rawTitle.replace("%page%", String.valueOf(currentPage + 1))
                           .replace("%max%", String.valueOf(maxPages));

        Component title = FXUnifyManager.parseColor(rawTitle);

        viewer.openMenu(new net.minecraft.world.SimpleMenuProvider(
            (containerId, playerInventory, p) -> new ChestMenu(MenuType.GENERIC_9x6, containerId, playerInventory, container, 6) {
                @Override
                public ItemStack quickMoveStack(Player player, int index) {
                    // Impede transferência de itens por shift-click para o SimpleContainer de resgate
                    return ItemStack.EMPTY;
                }

                @Override
                public void clicked(int slotId, int button, net.minecraft.world.inventory.ClickType clickType, Player clickPlayer) {
                    if (slotId >= 0 && slotId < 54) {
                        int currentMaxPages = Math.max(1, (int) Math.ceil((double) drops.size() / ITEMS_PER_PAGE));

                        // Botão Página Anterior
                        if (slotId == 45 && currentPage > 0) {
                            openRecoveryMenu((ServerPlayer) clickPlayer, owner, drops, currentPage - 1);
                            return;
                        }
                        
                        // Botão Próxima Página
<<<<<<< Updated upstream:src/main/java/bz/fxcore/fxunify/FXUnifyCommand.java
                        if (slotId == 53 && currentPage < maxPages - 1) {
                            openRecoveryMenu((ServerPlayer) clickPlayer, owner, drops, currentPage + 1);
=======
                        if (slotId == 53 && currentPage < currentMaxPages - 1) {
                            openRecoveryMenu((ServerPlayer) clickPlayer, ownerUuid, ownerName, drops, currentPage + 1);
>>>>>>> Stashed changes:src/main/java/bz/fxcore/modules/giveback/GiveBCommand.java
                            return;
                        }

                        // Clique em um item recuperável
                        if (slotId < ITEMS_PER_PAGE) {
<<<<<<< Updated upstream:src/main/java/bz/fxcore/fxunify/FXUnifyCommand.java
                            int realIndex = startIndex + slotId;
                            if (realIndex < drops.size()) {
                                FXUnifyManager.StoredDrop drop = drops.get(realIndex);
                                
                                long toGiveCount = drop.count;
                                while (toGiveCount > 0) {
                                    int amount = (int) Math.min(toGiveCount, (long) drop.item.getMaxStackSize());
                                    ItemStack giveStack = drop.item.copyWithCount(amount);
=======
                            if (!viewer.getUUID().equals(ownerUuid)) {
                                viewer.sendSystemMessage(Component.literal("§c[FXCore] Você está apenas visualizando o inventário deste jogador offline/online."));
                                return;
                            }

                            int realIndex = (currentPage * ITEMS_PER_PAGE) + slotId;
                            synchronized (drops) {
                                if (realIndex < drops.size()) {
                                    GiveBManager.StoredDrop drop = drops.get(realIndex);
                                    
                                    long toGiveCount = drop.count;
                                    while (toGiveCount > 0) {
                                        int amount = (int) Math.min(toGiveCount, (long) drop.item.getMaxStackSize());
                                        ItemStack giveStack = drop.item.copyWithCount(amount);
>>>>>>> Stashed changes:src/main/java/bz/fxcore/modules/giveback/GiveBCommand.java

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

                                    if (drops.isEmpty()) {
                                        GiveBManager.clearDrops(ownerUuid);
                                        clickPlayer.closeContainer();
                                        viewer.sendSystemMessage(GiveBManager.parseColor(GiveBConfig.DATA.emptyInventoryMessage));
                                        return;
                                    }

                                    int updatedMaxPages = (int) Math.ceil((double) drops.size() / ITEMS_PER_PAGE);
                                    int targetPage = Math.min(currentPage, updatedMaxPages - 1);
                                    if (targetPage != currentPage) {
                                        openRecoveryMenu((ServerPlayer) clickPlayer, ownerUuid, ownerName, drops, targetPage);
                                    } else {
                                        populateContainer(container, drops, targetPage, updatedMaxPages);
                                        this.broadcastChanges();
                                    }
                                }
<<<<<<< Updated upstream:src/main/java/bz/fxcore/fxunify/FXUnifyCommand.java

                                drop.count = toGiveCount;
                                if (drop.count <= 0) {
                                    drops.remove(realIndex);
                                }

                                openRecoveryMenu((ServerPlayer) clickPlayer, owner, drops, currentPage);
                                return;
=======
>>>>>>> Stashed changes:src/main/java/bz/fxcore/modules/giveback/GiveBCommand.java
                            }
                            return;
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