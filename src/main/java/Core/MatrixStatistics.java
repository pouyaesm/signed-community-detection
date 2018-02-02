package Core;

/**
 * Common statistics of partitioning a matrix
 */
public class MatrixStatistics {
    /**
     * Number of elements in each partition
     * @param partition partition[i] = k means i-th row and column are in the k-th group
     * @param matrix
     * @return
     */
    public static PartitionStatistics partitionStatistics(int[] partition, ListMatrix matrix){
        PartitionStatistics statistics = new PartitionStatistics();
        int[] rows = matrix.getRows();
        int[] columns = matrix.getColumns();
        // Find partitions count and initialize partitions size
        // when an entry is discarded, partitions "-1" exists in the input and must be excluded
        int hasDiscarded = Util.contains(-1, partition) ? 1 : 0;
        statistics.groupCount = Util.uniqueCount(partition) - hasDiscarded;
        statistics.maxGroupId = Util.max(partition);
        statistics.groupSizes = new int[statistics.maxGroupId + 1];
        // Calculate number of pairs per partitions
        for(int p = 0 ; p < rows.length ; p++){
            int rowGroupId = partition[rows[p]];
            int columnGroupId = partition[columns[p]];
            // Some rows/columns can be discarded with partitions = -1
            // To include a pair, both ends must reside in the same partitions
            if(rowGroupId < 0 || columnGroupId < 0){
                statistics.discardedCount++;
            } else if(rowGroupId == columnGroupId) {
                statistics.groupSizes[rowGroupId]++;
            }
        }
        return statistics;
    }
}
