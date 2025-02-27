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

package appeng.me.service.helpers;

import java.util.LongSummaryStatistics;

import javax.annotation.Nonnull;

import net.minecraft.CrashReportCategory;

import appeng.api.networking.IGridNode;
import appeng.api.networking.ticking.IGridTickable;
import appeng.api.networking.ticking.TickingRequest;
import appeng.api.parts.IPart;

public class TickTracker implements Comparable<TickTracker> {

    private final TickingRequest request;
    private final IGridTickable gt;
    private final IGridNode node;
    private final LongSummaryStatistics statistics;

    private long lastTick;
    private int currentRate;

    public TickTracker(final TickingRequest req, final IGridNode node, final IGridTickable gt, final long currentTick) {
        this.request = req;
        this.gt = gt;
        this.node = node;
        this.setCurrentRate((req.minTickRate + req.maxTickRate) / 2);
        this.setLastTick(currentTick);
        this.statistics = new LongSummaryStatistics();
    }

    @Override
    public int compareTo(@Nonnull final TickTracker t) {
        int next = Long.compare(this.getNextTick(), t.getNextTick());

        if (next != 0) {
            return next;
        }

        int last = Long.compare(this.getLastTick(), t.getLastTick());

        if (last != 0) {
            return last;
        }

        return Integer.compare(this.getCurrentRate(), t.getCurrentRate());

    }

    public void addEntityCrashInfo(final CrashReportCategory crashreportcategory) {
        if (this.getGridTickable() instanceof IPart part) {
            part.addEntityCrashInfo(crashreportcategory);
        }

        crashreportcategory.setDetail("CurrentTickRate", this.getCurrentRate());
        crashreportcategory.setDetail("MinTickRate", this.getRequest().minTickRate);
        crashreportcategory.setDetail("MaxTickRate", this.getRequest().maxTickRate);
        crashreportcategory.setDetail("Node", this.getNode());
        crashreportcategory.setDetail("ConnectedSides", this.getNode().getConnectedSides());
    }

    public int getCurrentRate() {
        return this.currentRate;
    }

    public void setCurrentRate(final int currentRate) {
        this.currentRate = Math.min(this.getRequest().maxTickRate,
                Math.max(this.getRequest().minTickRate, currentRate));
    }

    public long getNextTick() {
        return this.lastTick + this.currentRate;
    }

    public long getLastTick() {
        return this.lastTick;
    }

    public void setLastTick(final long lastTick) {
        this.lastTick = lastTick;
    }

    public IGridNode getNode() {
        return this.node;
    }

    public IGridTickable getGridTickable() {
        return this.gt;
    }

    public TickingRequest getRequest() {
        return this.request;
    }

    public LongSummaryStatistics getStatistics() {
        return statistics;
    }
}
