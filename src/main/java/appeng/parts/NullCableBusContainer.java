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

package appeng.parts;

import java.util.Random;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

import appeng.api.parts.SelectedPart;
import appeng.api.util.AEColor;
import appeng.client.render.cablebus.CableBusRenderState;

public class NullCableBusContainer implements ICableBusContainer {

    @Override
    public int isProvidingStrongPower(final Direction opposite) {
        return 0;
    }

    @Override
    public int isProvidingWeakPower(final Direction opposite) {
        return 0;
    }

    @Override
    public boolean canConnectRedstone(final Direction opposite) {
        return false;
    }

    @Override
    public void onEntityCollision(final Entity e) {

    }

    @Override
    public boolean activate(final Player player, final InteractionHand hand, final Vec3 vecFromPool) {
        return false;
    }

    @Override
    public void onNeighborChanged(BlockGetter level, BlockPos pos, BlockPos neighbor) {

    }

    @Override
    public boolean isEmpty() {
        return true;
    }

    @Override
    public SelectedPart selectPart(final Vec3 v3) {
        return new SelectedPart();
    }

    @Override
    public boolean recolourBlock(final Direction side, final AEColor colour, final Player who) {
        return false;
    }

    @Override
    public boolean isLadder(final LivingEntity entity) {
        return false;
    }

    @Override
    public void animateTick(final Level level, final BlockPos pos, final Random r) {

    }

    @Override
    public int getLightValue() {
        return 0;
    }

    @Override
    public CableBusRenderState getRenderState() {
        return new CableBusRenderState();
    }

    @Override
    public boolean clicked(Player player, InteractionHand hand, Vec3 hitVec) {
        return false;
    }

}
