/*
 * This file is part of Applied Energistics 2.
 * Copyright (c) 2013 - 2014, AlgorithmX2, All rights reserved.
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

package appeng.blockentity.crafting;

import java.io.IOException;
import java.util.List;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.inventory.CraftingContainer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.items.IItemHandler;

import appeng.api.AEApi;
import appeng.api.config.Actionable;
import appeng.api.config.PowerMultiplier;
import appeng.api.config.Upgrades;
import appeng.api.implementations.IPowerChannelState;
import appeng.api.implementations.IUpgradeInventory;
import appeng.api.implementations.IUpgradeableObject;
import appeng.api.implementations.blockentities.ICraftingMachine;
import appeng.api.implementations.blockentities.ISegmentedInventory;
import appeng.api.networking.IGridNode;
import appeng.api.networking.IGridNodeListener;
import appeng.api.networking.crafting.ICraftingPatternDetails;
import appeng.api.networking.ticking.IGridTickable;
import appeng.api.networking.ticking.TickRateModulation;
import appeng.api.networking.ticking.TickingRequest;
import appeng.api.storage.data.IAEItemStack;
import appeng.api.util.AECableType;
import appeng.blockentity.grid.AENetworkInvBlockEntity;
import appeng.blockentity.inventory.AppEngInternalInventory;
import appeng.client.render.crafting.AssemblerAnimationStatus;
import appeng.core.AppEng;
import appeng.core.definitions.AEBlocks;
import appeng.core.sync.network.NetworkHandler;
import appeng.core.sync.network.TargetPoint;
import appeng.core.sync.packets.AssemblerAnimationPacket;
import appeng.crafting.CraftingEvent;
import appeng.items.misc.EncodedPatternItem;
import appeng.menu.NullMenu;
import appeng.parts.automation.DefinitionUpgradeInventory;
import appeng.parts.automation.UpgradeInventory;
import appeng.util.InventoryAdaptor;
import appeng.util.Platform;
import appeng.util.helpers.ItemHandlerUtil;
import appeng.util.inv.InvOperation;
import appeng.util.inv.WrapperChainedItemHandler;
import appeng.util.inv.WrapperFilteredItemHandler;
import appeng.util.inv.filter.IAEItemFilter;
import appeng.util.item.AEItemStack;

public class MolecularAssemblerBlockEntity extends AENetworkInvBlockEntity
        implements IUpgradeableObject, IGridTickable, ICraftingMachine, IPowerChannelState {

    /**
     * Identifies the sub-inventory used by molecular assemblers to store the input items for the crafting process.
     */
    public static final ResourceLocation INV_MAIN = AppEng.makeId("molecular_assembler");

    private final CraftingContainer craftingInv;
    private final AppEngInternalInventory gridInv = new AppEngInternalInventory(this, 9 + 1, 1);
    private final AppEngInternalInventory patternInv = new AppEngInternalInventory(this, 1, 1);
    private final IItemHandler gridInvExt = new WrapperFilteredItemHandler(this.gridInv, new CraftingGridFilter());
    private final IItemHandler internalInv = new WrapperChainedItemHandler(this.gridInv, this.patternInv);
    private final UpgradeInventory upgrades;
    private boolean isPowered = false;
    private Direction pushDirection = null;
    private ItemStack myPattern = ItemStack.EMPTY;
    private ICraftingPatternDetails myPlan = null;
    private double progress = 0;
    private boolean isAwake = false;
    private boolean forcePlan = false;
    private boolean reboot = true;

    @OnlyIn(Dist.CLIENT)
    private AssemblerAnimationStatus animationStatus;

    public MolecularAssemblerBlockEntity(BlockEntityType<?> blockEntityType, BlockPos pos, BlockState blockState) {
        super(blockEntityType, pos, blockState);

        this.getMainNode()
                .setIdlePowerUsage(0.0)
                .addService(IGridTickable.class, this);
        this.upgrades = new DefinitionUpgradeInventory(AEBlocks.MOLECULAR_ASSEMBLER, this, this.getUpgradeSlots());
        this.craftingInv = new CraftingContainer(new NullMenu(), 3, 3);

    }

    private int getUpgradeSlots() {
        return 5;
    }

    @Override
    public boolean pushPattern(final ICraftingPatternDetails patternDetails, final CraftingContainer table,
            final Direction where) {
        if (this.myPattern.isEmpty()) {
            boolean isEmpty = ItemHandlerUtil.isEmpty(this.gridInv) && ItemHandlerUtil.isEmpty(this.patternInv);

            if (isEmpty && patternDetails.isCraftable()) {
                this.forcePlan = true;
                this.myPlan = patternDetails;
                this.pushDirection = where;

                for (int x = 0; x < table.getContainerSize(); x++) {
                    this.gridInv.setStackInSlot(x, table.getItem(x));
                }

                this.updateSleepiness();
                this.saveChanges();
                return true;
            }
        }
        return false;
    }

    private void updateSleepiness() {
        final boolean wasEnabled = this.isAwake;
        this.isAwake = this.myPlan != null && this.hasMats() || this.canPush();
        if (wasEnabled != this.isAwake) {
            getMainNode().ifPresent((grid, node) -> {
                if (this.isAwake) {
                    grid.getTickManager().wakeDevice(node);
                } else {
                    grid.getTickManager().sleepDevice(node);
                }
            });
        }
    }

    private boolean canPush() {
        return !this.gridInv.getStackInSlot(9).isEmpty();
    }

    private boolean hasMats() {
        if (this.myPlan == null) {
            return false;
        }

        for (int x = 0; x < this.craftingInv.getContainerSize(); x++) {
            this.craftingInv.setItem(x, this.gridInv.getStackInSlot(x));
        }

        return !this.myPlan.getOutput(this.craftingInv, this.getLevel()).isEmpty();
    }

    @Override
    public boolean acceptsPlans() {
        return ItemHandlerUtil.isEmpty(this.patternInv);
    }

    @Override
    protected boolean readFromStream(final FriendlyByteBuf data) throws IOException {
        final boolean c = super.readFromStream(data);
        final boolean oldPower = this.isPowered;
        this.isPowered = data.readBoolean();
        return this.isPowered != oldPower || c;
    }

    @Override
    protected void writeToStream(final FriendlyByteBuf data) throws IOException {
        super.writeToStream(data);
        data.writeBoolean(this.isPowered);
    }

    @Override
    public CompoundTag save(final CompoundTag data) {
        super.save(data);
        if (this.forcePlan && this.myPlan != null) {
            final ItemStack pattern = this.myPlan.getPattern();
            if (!pattern.isEmpty()) {
                final CompoundTag compound = new CompoundTag();
                pattern.save(compound);
                data.put("myPlan", compound);
                data.putInt("pushDirection", this.pushDirection.ordinal());
            }
        }

        this.upgrades.writeToNBT(data, "upgrades");
        return data;
    }

    @Override
    public void load(final CompoundTag data) {
        super.load(data);
        if (data.contains("myPlan")) {
            final ItemStack myPat = ItemStack.of(data.getCompound("myPlan"));

            if (!myPat.isEmpty() && myPat.getItem() instanceof EncodedPatternItem) {
                final Level level = this.getLevel();
                final ICraftingPatternDetails ph = AEApi.crafting().decodePattern(myPat, level);
                if (ph != null && ph.isCraftable()) {
                    this.forcePlan = true;
                    this.myPlan = ph;
                    this.pushDirection = Direction.values()[data.getInt("pushDirection")];
                }
            }
        }

        this.upgrades.readFromNBT(data, "upgrades");
        this.recalculatePlan();
    }

    private void recalculatePlan() {
        this.reboot = true;

        if (this.forcePlan) {
            return;
        }

        final ItemStack is = this.patternInv.getStackInSlot(0);

        if (!is.isEmpty() && is.getItem() instanceof EncodedPatternItem) {
            if (!ItemStack.isSame(is, this.myPattern)) {
                final Level level = this.getLevel();
                final ICraftingPatternDetails ph = AEApi.crafting().decodePattern(is, level);

                if (ph != null && ph.isCraftable()) {
                    this.progress = 0;
                    this.myPattern = is;
                    this.myPlan = ph;
                }
            }
        } else {
            this.progress = 0;
            this.forcePlan = false;
            this.myPlan = null;
            this.myPattern = ItemStack.EMPTY;
            this.pushDirection = null;
        }

        this.updateSleepiness();
    }

    @Override
    public AECableType getCableConnectionType(Direction dir) {
        return AECableType.COVERED;
    }

    @Override
    public IItemHandler getSubInventory(ResourceLocation id) {
        if (id.equals(ISegmentedInventory.UPGRADES)) {
            return this.upgrades;
        } else if (id.equals(INV_MAIN)) {
            return this.internalInv;
        }

        return super.getSubInventory(id);
    }

    @Override
    public IItemHandler getInternalInventory() {
        return this.internalInv;
    }

    @Override
    protected IItemHandler getItemHandlerForSide(Direction side) {
        return this.gridInvExt;
    }

    @Override
    public void onChangeInventory(final IItemHandler inv, final int slot, final InvOperation mc,
            final ItemStack removed, final ItemStack added) {
        if (inv == this.gridInv || inv == this.patternInv) {
            this.recalculatePlan();
        }
    }

    public int getCraftingProgress() {
        return (int) this.progress;
    }

    @Override
    public void getDrops(final Level level, final BlockPos pos, final List<ItemStack> drops) {
        super.getDrops(level, pos, drops);

        for (int h = 0; h < this.upgrades.getSlots(); h++) {
            final ItemStack is = this.upgrades.getStackInSlot(h);
            if (!is.isEmpty()) {
                drops.add(is);
            }
        }
    }

    @Override
    public TickingRequest getTickingRequest(final IGridNode node) {
        this.recalculatePlan();
        this.updateSleepiness();
        return new TickingRequest(1, 1, !this.isAwake, false);
    }

    @Override
    public TickRateModulation tickingRequest(final IGridNode node, int ticksSinceLastCall) {
        if (!this.gridInv.getStackInSlot(9).isEmpty()) {
            this.pushOut(this.gridInv.getStackInSlot(9));

            // did it eject?
            if (this.gridInv.getStackInSlot(9).isEmpty()) {
                this.saveChanges();
            }

            this.ejectHeldItems();
            this.updateSleepiness();
            this.progress = 0;
            return this.isAwake ? TickRateModulation.IDLE : TickRateModulation.SLEEP;
        }

        if (this.myPlan == null) {
            this.updateSleepiness();
            return TickRateModulation.SLEEP;
        }

        if (this.reboot) {
            ticksSinceLastCall = 1;
        }

        if (!this.isAwake) {
            return TickRateModulation.SLEEP;
        }

        this.reboot = false;
        int speed = 10;
        switch (this.upgrades.getInstalledUpgrades(Upgrades.SPEED)) {
            case 0 -> this.progress += this.userPower(ticksSinceLastCall, speed = 10, 1.0);
            case 1 -> this.progress += this.userPower(ticksSinceLastCall, speed = 13, 1.3);
            case 2 -> this.progress += this.userPower(ticksSinceLastCall, speed = 17, 1.7);
            case 3 -> this.progress += this.userPower(ticksSinceLastCall, speed = 20, 2.0);
            case 4 -> this.progress += this.userPower(ticksSinceLastCall, speed = 25, 2.5);
            case 5 -> this.progress += this.userPower(ticksSinceLastCall, speed = 50, 5.0);
        }

        if (this.progress >= 100) {
            for (int x = 0; x < this.craftingInv.getContainerSize(); x++) {
                this.craftingInv.setItem(x, this.gridInv.getStackInSlot(x));
            }

            this.progress = 0;
            final ItemStack output = this.myPlan.getOutput(this.craftingInv, this.getLevel());
            if (!output.isEmpty()) {
                CraftingEvent.fireAutoCraftingEvent(getLevel(), this.myPlan, output, this.craftingInv);

                this.pushOut(output.copy());

                for (int x = 0; x < this.craftingInv.getContainerSize(); x++) {
                    this.gridInv.setStackInSlot(x, Platform.getContainerItem(this.craftingInv.getItem(x)));
                }

                if (ItemHandlerUtil.isEmpty(this.patternInv)) {
                    this.forcePlan = false;
                    this.myPlan = null;
                    this.pushDirection = null;
                }

                this.ejectHeldItems();

                final IAEItemStack item = AEItemStack.fromItemStack(output);
                if (item != null) {
                    final TargetPoint where = new TargetPoint(this.worldPosition.getX(), this.worldPosition.getY(),
                            this.worldPosition.getZ(), 32,
                            this.level);
                    NetworkHandler.instance()
                            .sendToAllAround(new AssemblerAnimationPacket(this.worldPosition, (byte) speed, item),
                                    where);
                }

                this.saveChanges();
                this.updateSleepiness();
                return this.isAwake ? TickRateModulation.IDLE : TickRateModulation.SLEEP;
            }
        }

        return TickRateModulation.FASTER;
    }

    private void ejectHeldItems() {
        if (this.gridInv.getStackInSlot(9).isEmpty()) {
            for (int x = 0; x < 9; x++) {
                final ItemStack is = this.gridInv.getStackInSlot(x);
                if (!is.isEmpty() && (this.myPlan == null || !this.myPlan.isValidItemForSlot(x, is, this.level))) {
                    this.gridInv.setStackInSlot(9, is);
                    this.gridInv.setStackInSlot(x, ItemStack.EMPTY);
                    this.saveChanges();
                    return;
                }
            }
        }
    }

    private int userPower(final int ticksPassed, final int bonusValue, final double acceleratorTax) {
        var grid = getMainNode().getGrid();
        if (grid != null) {
            return (int) (grid.getEnergyService().extractAEPower(ticksPassed * bonusValue * acceleratorTax,
                    Actionable.MODULATE, PowerMultiplier.CONFIG) / acceleratorTax);
        } else {
            return 0;
        }
    }

    private void pushOut(ItemStack output) {
        if (this.pushDirection == null) {
            for (final Direction d : Direction.values()) {
                output = this.pushTo(output, d);
            }
        } else {
            output = this.pushTo(output, this.pushDirection);
        }

        if (output.isEmpty() && this.forcePlan) {
            this.forcePlan = false;
            this.recalculatePlan();
        }

        this.gridInv.setStackInSlot(9, output);
    }

    private ItemStack pushTo(ItemStack output, final Direction d) {
        if (output.isEmpty()) {
            return output;
        }

        final BlockEntity te = this.getLevel().getBlockEntity(this.worldPosition.relative(d));

        if (te == null) {
            return output;
        }

        final InventoryAdaptor adaptor = InventoryAdaptor.getAdaptor(te, d.getOpposite());

        if (adaptor == null) {
            return output;
        }

        final int size = output.getCount();
        output = adaptor.addItems(output);
        final int newSize = output.isEmpty() ? 0 : output.getCount();

        if (size != newSize) {
            this.saveChanges();
        }

        return output;
    }

    @Override
    public void onMainNodeStateChanged(IGridNodeListener.State reason) {
        boolean newState = false;

        var grid = getMainNode().getGrid();
        if (grid != null) {
            newState = this.getMainNode().isActive()
                    && grid.getEnergyService().extractAEPower(1, Actionable.SIMULATE,
                            PowerMultiplier.CONFIG) > 0.0001;
        }

        if (newState != this.isPowered) {
            this.isPowered = newState;
            this.markForUpdate();
        }
    }

    @Override
    public boolean isPowered() {
        return this.isPowered;
    }

    @Override
    public boolean isActive() {
        return this.isPowered;
    }

    @OnlyIn(Dist.CLIENT)
    public void setAnimationStatus(@Nullable AssemblerAnimationStatus status) {
        this.animationStatus = status;
    }

    @OnlyIn(Dist.CLIENT)
    @Nullable
    public AssemblerAnimationStatus getAnimationStatus() {
        return this.animationStatus;
    }

    @Nonnull
    @Override
    public IUpgradeInventory getUpgrades() {
        return upgrades;
    }

    private class CraftingGridFilter implements IAEItemFilter {
        private boolean hasPattern() {
            return MolecularAssemblerBlockEntity.this.myPlan != null
                    && !ItemHandlerUtil.isEmpty(MolecularAssemblerBlockEntity.this.patternInv);
        }

        @Override
        public boolean allowExtract(IItemHandler inv, int slot, int amount) {
            return slot == 9;
        }

        @Override
        public boolean allowInsert(IItemHandler inv, int slot, ItemStack stack) {
            if (slot >= 9) {
                return false;
            }

            if (this.hasPattern()) {
                return MolecularAssemblerBlockEntity.this.myPlan.isValidItemForSlot(slot, stack,
                        MolecularAssemblerBlockEntity.this.getLevel());
            }
            return false;
        }
    }
}
