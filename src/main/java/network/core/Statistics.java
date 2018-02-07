package network.core;

/**
 * Common statistics of partitioning a matrix
 */
public class Statistics {
    /**
     * Number of elements in each partition
     * @param partition partition[i] = k means i-th row and column are in the k-th group
     * @param matrix
     * @return
     */
    public static PartitionStatistics partition(int[] partition, ListMatrix matrix){
        PartitionStatistics statistics = new PartitionStatistics();
        int[] rows = matrix.getRows();
        int[] columns = matrix.getColumns();
        float[] values = matrix.getValues();
        // Find partitions groupCount and initialize partitions size
        // when an entry is discarded, partitions "-1" exists in the input and must be excluded
        int hasDiscarded = Util.contains(-1, partition) ? 1 : 0;
        ArrayStatistics arrayStatistics = array(partition);
        statistics.groupCount = arrayStatistics.uniqueCount - hasDiscarded;
        statistics.maxGroupId = arrayStatistics.maxValue;
        statistics.cellCount = new int[statistics.maxGroupId + 1];
        statistics.cellValue = new float[statistics.maxGroupId + 1];
        statistics.positiveCellCount = new int[statistics.maxGroupId + 1];
        statistics.positiveCellValue = new float[statistics.maxGroupId + 1];
        statistics.negativeCellCount = new int[statistics.maxGroupId + 1];
        statistics.negativeCellValue = new float[statistics.maxGroupId + 1];
        statistics.size = arrayStatistics.frequency;
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

    /**
     * Number of unique values in all arrays, frequency of values, min and max of values
     * @param values
     * @return
     */
    public static ArrayStatistics array(int[]...values) {
        int minValue = Util.min(values);
        int maxValue = Util.max(values);
        int valueRange = maxValue - minValue;
        int uniqueCount = 0;
        boolean[] visitedValues = new boolean[valueRange + 1];
        int[] frequency = new int[valueRange + 1];
        for(int[] array : values) {
            for (int i = 0; i < array.length; i++) {
                int adjustedValue = array[i] - minValue;
                if (!visitedValues[adjustedValue]) {
                    uniqueCount++;
                    visitedValues[adjustedValue] = true;
                }
                frequency[adjustedValue]++;
            }
        }
        ArrayStatistics statistics = new ArrayStatistics();
        statistics.minValue = minValue;
        statistics.maxValue = maxValue;
        statistics.uniqueCount = uniqueCount;
        statistics.frequency = frequency;
        return statistics;
    }

    public static ArrayStatistics array(int[] values){
        int[][] arrays = new int[1][];
        arrays[0] = values;
        return array(arrays);
    }
}
