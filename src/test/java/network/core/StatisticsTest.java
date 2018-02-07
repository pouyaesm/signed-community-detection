package network.core;

import org.junit.Assert;
import org.junit.jupiter.api.Test;

public class StatisticsTest {

    @Test
    public void testArrayStatistics(){
        int[] values = {-1, 1, 2, -1, -1, 3, -1};
        ArrayStatistics statistics = Statistics.array(values);
        Assert.assertEquals(4, statistics.uniqueCount);
        Assert.assertEquals(-1, statistics.minValue);
        Assert.assertEquals(3, statistics.maxValue);
        // frequency of -1, 0, 1, 2, 3 respectively
        Assert.assertArrayEquals(new int[]{4, 0, 1, 1, 1}, statistics.frequency);
    }

    @Test
    public void testPartitionStatistics() throws Exception {
        ListMatrix listMatrix = GraphIO
                .readListMatrix("testCases/squareConflict.txt", true).normalize();
        // {1, 2, 3}, {4}, all positive except (1, 3) and (2, 4)
        int[] partition = {0, 0, 0, 1};
        PartitionStatistics statistics = Statistics.partition(partition, listMatrix);
        int[] expectedNodeCount = {3, 1};
        int[] expectedEdgeCount = {6, 0}; // each symmetric edge is counted twice
        double[] expectedWeight = {2, 0}; // 2 + 2 - 2 for {1, 2, 3}
        double [] expectedPositiveWeight = {4, 0};
        double [] expectedNegativeWeight = {2, 0};
        Assert.assertArrayEquals(expectedNodeCount, statistics.size);
        Assert.assertArrayEquals(expectedEdgeCount, statistics.cellCount);
        Assert.assertArrayEquals(expectedPositiveWeight, statistics.positiveCellValue, 0f);
        Assert.assertArrayEquals(expectedNegativeWeight, statistics.negativeCellValue, 0f);
        Assert.assertArrayEquals(expectedWeight, statistics.cellValue, 0f);
        Assert.assertEquals(1, statistics.maxGroupId);


    }
}
