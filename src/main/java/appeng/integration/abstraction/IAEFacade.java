/*
 * This file is part of Applied Energistics 2.
 * Copyright (c) 2013 - 2018, AlgorithmX2, All rights reserved.
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

package appeng.integration.abstraction;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.state.BlockState;

import team.chisel.ctm.api.IFacade;

/**
 * Neat abstraction class for All the IFacade interfaces.
 *
 * @author covers1624
 */
public interface IAEFacade extends IFacade {

    BlockState getFacadeState(BlockGetter level, BlockPos pos, @Nullable Direction side);

    @Nonnull
    @Override
    default BlockState getFacade(@Nonnull BlockGetter level, @Nonnull BlockPos pos, @Nullable Direction side) {
        return getFacadeState(level, pos, side);
    }

    @Nonnull
    @Override
    default BlockState getFacade(@Nonnull BlockGetter level, @Nonnull BlockPos pos, @Nullable Direction side,
            @Nonnull BlockPos connection) {
        return getFacadeState(level, pos, side);
    }

}
