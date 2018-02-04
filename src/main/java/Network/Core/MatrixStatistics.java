package Network.Core;

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
        float[] values = matrix.getValues();
        // Find partitions count and initialize partitions size
        // when an entry is discarded, partitions "-1" exists in the input and must be excluded
        int hasDiscarded = Util.contains(-1, partition) ? 1 : 0;
        statistics.count = Util.uniqueCount(partition) - hasDiscarded;
        statistics.maxGroupId = Util.max(partition);
        statistics.cellCount = new int[statistics.maxGroupId + 1];
        statistics.cellValue = new float[statistics.maxGroupId + 1];
        statistics.positiveCellCount = new int[statistics.maxGroupId + 1];
        statistics.positiveCellValue = new float[statistics.maxGroupId + 1];
        statistics.negativeCellCount = new int[statistics.maxGroupId + 1];
        statistics.negativeCellValue = new float[statistics.maxGroupId + 1];
        statistics.size = new int[statistics.maxGroupId + 1];
        // Calculate number of pairs per partitions
        for(int p = 0 ; p < rows.length ; p++){
            int rowGroupId = partition[rows[p]];
            int columnGroupId = partition[columns[p]];
            float value = values[p];
            // Some rows/columns can be discarded with partitions = -1
            // To include a pair, both ends must reside in the same partitions
            if(rowGroupId < 0 || columnGroupId < 0){
                statistics.discardedCellCount++;
            } else if(rowGroupId == columnGroupId) {
                statistics.cellCount[rowGroupId]++;
                statistics.cellValue[rowGroupId] += value;
                if(value > 0){
                    statistics.positiveCellCount[rowGroupId]++;
                    statistics.positiveCellValue[rowGroupId] += value;
                } else if (value < 0){
                    statistics.negativeCellCount[rowGroupId]++;
                    statistics.negativeCellValue[rowGroupId] -=value;
                }
            }
        }
        return statistics;
    }
}
