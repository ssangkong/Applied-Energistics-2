package appeng.api.networking.crafting;

import java.util.Map;

import appeng.api.storage.data.IAEItemStack;
import appeng.api.storage.data.IItemList;

/**
 * Result of a {@linkplain ICraftingService#beginCraftingJob crafting job calculation}. Do not edit any of the
 * map/lists, they are exposed directly!
 */
public interface ICraftingPlan {
    /**
     * Final output of the job.
     */
    IAEItemStack finalOutput();

    /**
     * Total bytes used by the job.
     */
    long bytes();

    /**
     * True if some things were missing and this is just a simulation.
     */
    boolean simulation();

    /**
     * List of items that were used. (They would need to be extracted to start the job).
     */
    IItemList<IAEItemStack> usedItems();

    /**
     * List of items that need to be emitted for this job.
     */
    IItemList<IAEItemStack> emittedItems();

    /**
     * List of missing items if this is a simulation.
     */
    IItemList<IAEItemStack> missingItems();

    /**
     * Map of each pattern to the number of times it needs to be crafted. Can be used to retrieve the crafted items:
     * outputs * times.
     */
    Map<ICraftingPatternDetails, Long> patternTimes();
}
