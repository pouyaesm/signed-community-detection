package network.core;

/**
 * Partition statistics holder (like a struct in C++)
 */
public class PartitionStatistics {
    /**
     * Number of pairs (non-empty cells) in each group of the partition
     */
    public int[] cellCount;

    /**
     * Number of row or column ids assigned to each group
     */
    public int[] size;
    /**
     * Total cell value of group
     */
    public double[] cellValue;

    /**
     * Number of positive cells
     */
    public int[] positiveCellCount;

    /**
     * Total positive cell value of partition
     */
    public double[] positiveCellValue;

    /**
     * Number of negative cells
     */
    public int[] negativeCellCount;

    /**
     * Total positive cell value of partition
     */
    public double[] negativeCellValue;

    /**
     * Number of groups in the partition
     */
    public int groupCount;
    /**
     * Number of discarded pairs which their row or column is assigned to partition -1
     */
    public int discardedCellCount;
    /**
     * Maximum group id
     */
    public int maxGroupId = Integer.MIN_VALUE;
}
