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

package appeng.core.stats;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;

import appeng.core.AppEng;

public enum AeStats {

    // done
    ItemsInserted("items_inserted"),

    // done
    ItemsExtracted("items_extracted"),

    // done
    TurnedCranks("turned_cranks");

    private final ResourceLocation registryName;

    AeStats(String id) {
        this.registryName = new ResourceLocation(AppEng.MOD_ID, id);
    }

    public void addToPlayer(final Player player, final int howMany) {
        player.awardStat(this.registryName, howMany);
    }

    public ResourceLocation getRegistryName() {
        return registryName;
    }

}
