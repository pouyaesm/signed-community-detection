package Core;

/**
 * Partition statistics holder (like a struct in C++)
 */
public class PartitionStatistics {
    /**
     * Number of pairs in each group of the partition
     */
    public int[] groupSizes;
    /**
     * Number of partition groups
     */
    public int groupCount;
    /**
     * Number of discarded pairs which their row or column is assigned to partition -1
     */
    public int discardedCount;
}
