/*
 * This file is part of Applied Energistics 2.
 * Copyright (c) 2021, TeamAppliedEnergistics, All rights reserved.
 *
 * Applied Energistics 2 is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Applied Energistics 2 is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with Applied Energistics 2.  If not, see <http://www.gnu.org/licenses/lgpl>.
 */

package appeng.menu.me.items;

import javax.annotation.Nullable;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.ItemStack;

import appeng.api.config.Actionable;
import appeng.api.storage.ITerminalHost;
import appeng.api.storage.StorageChannels;
import appeng.api.storage.data.IAEItemStack;
import appeng.core.AELog;
import appeng.helpers.InventoryAction;
import appeng.menu.MenuLocator;
import appeng.menu.implementations.MenuTypeBuilder;
import appeng.menu.me.common.GridInventoryEntry;
import appeng.menu.me.common.IClientRepo;
import appeng.menu.me.common.MEMonitorableMenu;
import appeng.menu.me.crafting.CraftAmountMenu;
import appeng.util.InventoryAdaptor;
import appeng.util.Platform;
import appeng.util.inv.AdaptorItemHandler;
import appeng.util.inv.WrapperCursorItemHandler;
import appeng.util.item.AEItemStack;

/**
 * @see appeng.client.gui.me.items.ItemTerminalScreen
 */
public class ItemTerminalMenu extends MEMonitorableMenu<IAEItemStack> {

    public static final MenuType<ItemTerminalMenu> TYPE = MenuTypeBuilder
            .create(ItemTerminalMenu::new, ITerminalHost.class)
            .build("item_terminal");

    public ItemTerminalMenu(int id, Inventory ip, ITerminalHost monitorable) {
        this(TYPE, id, ip, monitorable, true);
    }

    public ItemTerminalMenu(MenuType<?> menuType, int id, Inventory ip, ITerminalHost host,
            boolean bindInventory) {
        super(menuType, id, ip, host, bindInventory,
                StorageChannels.items());
    }

    @Override
    protected void handleNetworkInteraction(ServerPlayer player, @Nullable IAEItemStack stack,
            InventoryAction action) {

        // Handle interactions where the player wants to put something into the network
        if (stack == null) {
            if (action == InventoryAction.SPLIT_OR_PLACE_SINGLE || action == InventoryAction.ROLL_DOWN) {
                putCarriedItemIntoNetwork(true);
            } else if (action == InventoryAction.PICKUP_OR_SET_DOWN) {
                putCarriedItemIntoNetwork(false);
            }
            return;
        }

        switch (action) {
            case AUTO_CRAFT:
                final MenuLocator locator = getLocator();
                if (locator != null) {
                    CraftAmountMenu.open(player, locator, stack, 1);
                }
                break;

            case SHIFT_CLICK:
                moveOneStackToPlayer(stack, player);
                break;

            case ROLL_DOWN: {
                final int releaseQty = 1;
                var isg = getCarried();

                if (!isg.isEmpty() && releaseQty > 0) {
                    IAEItemStack ais = StorageChannels.items()
                            .createStack(isg);
                    ais.setStackSize(1);
                    final IAEItemStack extracted = ais.copy();

                    ais = Platform.poweredInsert(powerSource, monitor, ais,
                            this.getActionSource());
                    if (ais == null) {
                        var ia = new AdaptorItemHandler(new WrapperCursorItemHandler(this));

                        final ItemStack fail = ia.removeItems(1, extracted.getDefinition(), null);
                        if (fail.isEmpty()) {
                            monitor.extractItems(extracted, Actionable.MODULATE,
                                    this.getActionSource());
                        }
                    }
                }
            }
                break;
            case ROLL_UP:
            case PICKUP_SINGLE:
                int liftQty = 1;
                var item = getCarried();

                if (!item.isEmpty()) {
                    if (item.getCount() >= item.getMaxStackSize()) {
                        liftQty = 0;
                    }
                    if (!Platform.itemComparisons().isSameItem(stack.getDefinition(), item)) {
                        liftQty = 0;
                    }
                }

                if (liftQty > 0) {
                    IAEItemStack ais = stack.copy();
                    ais.setStackSize(1);
                    ais = Platform.poweredExtraction(powerSource, monitor, ais,
                            this.getActionSource());
                    if (ais != null) {
                        final InventoryAdaptor ia = new AdaptorItemHandler(
                                new WrapperCursorItemHandler(player.inventoryMenu));

                        final ItemStack fail = ia.addItems(ais.createItemStack());
                        if (!fail.isEmpty()) {
                            monitor.injectItems(ais, Actionable.MODULATE, this.getActionSource());
                        }
                    }
                }
                break;
            case PICKUP_OR_SET_DOWN:
                if (!getCarried().isEmpty()) {
                    putCarriedItemIntoNetwork(false);
                } else {
                    IAEItemStack ais = stack.copy();
                    ais.setStackSize(ais.getDefinition().getMaxStackSize());
                    ais = Platform.poweredExtraction(powerSource, monitor, ais,
                            this.getActionSource());
                    if (ais != null) {
                        setCarried(ais.createItemStack());
                    } else {
                        setCarried(ItemStack.EMPTY);
                    }
                }

                break;
            case SPLIT_OR_PLACE_SINGLE:
                if (!getCarried().isEmpty()) {
                    putCarriedItemIntoNetwork(true);
                } else {
                    IAEItemStack ais = stack.copy();
                    final long maxSize = ais.getDefinition().getMaxStackSize();
                    ais.setStackSize(maxSize);
                    ais = monitor.extractItems(ais, Actionable.SIMULATE, this.getActionSource());

                    if (ais != null) {
                        final long stackSize = Math.min(maxSize, ais.getStackSize());
                        ais.setStackSize(stackSize + 1 >> 1);
                        ais = Platform.poweredExtraction(powerSource, monitor, ais,
                                this.getActionSource());
                    }

                    if (ais != null) {
                        setCarried(ais.createItemStack());
                    } else {
                        setCarried(ItemStack.EMPTY);
                    }
                }

                break;
            case CREATIVE_DUPLICATE:
                if (player.getAbilities().instabuild) {
                    final ItemStack is = stack.createItemStack();
                    is.setCount(is.getMaxStackSize());
                    setCarried(is);
                }
                break;
            case MOVE_REGION:
                final int playerInv = player.getInventory().items.size();
                for (int slotNum = 0; slotNum < playerInv; slotNum++) {
                    if (!moveOneStackToPlayer(stack, player)) {
                        break;
                    }
                }
                break;
            default:
                AELog.warn("Received unhandled inventory action %s from client in %s", action, getClass());
                break;
        }
    }

    protected void putCarriedItemIntoNetwork(boolean singleItem) {
        var heldStack = getCarried();

        IAEItemStack stackToInsert = AEItemStack.fromItemStack(heldStack);
        if (stackToInsert == null) {
            return;
        }

        if (singleItem) {
            stackToInsert.setStackSize(1);
        }

        IAEItemStack remainder = Platform.poweredInsert(powerSource, monitor, stackToInsert, this.getActionSource());
        long inserted = stackToInsert.getStackSize() - (remainder == null ? 0 : remainder.getStackSize());

        if (inserted >= heldStack.getCount()) {
            setCarried(ItemStack.EMPTY);
        } else {
            heldStack = heldStack.copy();
            heldStack.setCount(heldStack.getCount() - (int) inserted);
            setCarried(heldStack);
        }
    }

    private boolean moveOneStackToPlayer(IAEItemStack stack, ServerPlayer player) {
        IAEItemStack ais = stack.copy();
        ItemStack myItem = ais.createItemStack();

        ais.setStackSize(myItem.getMaxStackSize());

        final InventoryAdaptor adp = InventoryAdaptor.getAdaptor(player);
        myItem.setCount((int) ais.getStackSize());
        myItem = adp.simulateAdd(myItem);

        if (!myItem.isEmpty()) {
            ais.setStackSize(ais.getStackSize() - myItem.getCount());
        }
        if (ais.getStackSize() <= 0) {
            return false;
        }

        ais = Platform.poweredExtraction(powerSource, monitor, ais, getActionSource());

        return ais != null && adp.addItems(ais.createItemStack()).isEmpty();
    }

    @Override
    protected ItemStack transferStackToMenu(ItemStack input) {
        if (!canInteractWithGrid()) {
            return super.transferStackToMenu(input);
        }

        final IAEItemStack ais = Platform.poweredInsert(powerSource, monitor,
                StorageChannels.items().createStack(input),
                this.getActionSource());
        return ais == null ? ItemStack.EMPTY : ais.createItemStack();
    }

    public boolean hasItemType(ItemStack itemStack, int amount) {
        IClientRepo<IAEItemStack> clientRepo = getClientRepo();

        if (clientRepo != null) {
            for (GridInventoryEntry<IAEItemStack> stack : clientRepo.getAllEntries()) {
                if (stack.getStack().equals(itemStack)) {
                    if (stack.getStoredAmount() >= amount) {
                        return true;
                    }
                    amount -= stack.getStoredAmount();
                }
            }
        }

        return false;
    }

}
