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

package appeng.blockentity.grid;

import javax.annotation.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

import appeng.api.AEApi;
import appeng.api.networking.IGridNode;
import appeng.api.networking.IInWorldGridNodeHost;
import appeng.api.networking.IManagedGridNode;
import appeng.api.util.AECableType;
import appeng.blockentity.AEBaseBlockEntity;
import appeng.hooks.ticking.TickHandler;
import appeng.me.helpers.BlockEntityNodeListener;
import appeng.me.helpers.IGridConnectedBlockEntity;

public class AENetworkBlockEntity extends AEBaseBlockEntity implements IInWorldGridNodeHost, IGridConnectedBlockEntity {

    private final IManagedGridNode mainNode = createMainNode()
            .setVisualRepresentation(getItemFromBlockEntity())
            .setInWorldNode(true)
            .setTagName("proxy");

    protected IManagedGridNode createMainNode() {
        return AEApi.grid().createManagedNode(this, BlockEntityNodeListener.INSTANCE);
    }

    public AENetworkBlockEntity(BlockEntityType<?> blockEntityType, BlockPos pos, BlockState blockState) {
        super(blockEntityType, pos, blockState);
    }

    @Override
    public void load(final CompoundTag data) {
        super.load(data);
        this.getMainNode().loadFromNBT(data);
    }

    @Override
    public CompoundTag save(final CompoundTag data) {
        super.save(data);
        this.getMainNode().saveToNBT(data);
        return data;
    }

    public final IManagedGridNode getMainNode() {
        return this.mainNode;
    }

    @Nullable
    public IGridNode getGridNode() {
        return getMainNode().getNode();
    }

    @Override
    public IGridNode getGridNode(final Direction dir) {
        var node = this.getMainNode().getNode();

        // Check if the proxy exposes the node on this side
        if (node != null && node.isExposedOnSide(dir)) {
            return node;
        }

        return null;
    }

    @Override
    public AECableType getCableConnectionType(Direction dir) {
        return AECableType.SMART;
    }

    @Override
    public void onChunkUnloaded() {
        super.onChunkUnloaded();
        this.getMainNode().destroy();
    }

    @Override
    public void onReady() {
        super.onReady();
        this.getMainNode().create(getLevel(), getBlockEntity().getBlockPos());
    }

    @Override
    public void setRemoved() {
        super.setRemoved();
        this.getMainNode().destroy();
    }

    @Override
    public void clearRemoved() {
        super.clearRemoved();
        TickHandler.instance().addInit(this); // Required for onReady to be called
    }

}
