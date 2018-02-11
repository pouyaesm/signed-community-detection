package network.core;

import cern.colt.map.OpenIntIntHashMap;
import org.junit.Assert;
import org.junit.Test;

public class GraphIOTest {

    @Test
    public void testReadListMatrix() throws Exception{
        ListMatrix listMatrix = GraphIO.readListMatrix("testCases/threeEdges.txt", false);
        int[] expectedRows = {1, 5, 1};
        int[] expectedColumns = {2, 1, 7};
        float[] expectedValues = {10, -11.5f, 12};
        Assert.assertArrayEquals(expectedRows, listMatrix.getRows());
        Assert.assertArrayEquals(expectedColumns, listMatrix.getColumns());
        Assert.assertArrayEquals(expectedValues, listMatrix.getValues(), .00001f);
    }

    @Test
    public void testReadPartition() throws Exception{
        Graph infoMap = GraphIO.readGraph("testCases/infoMap.txt", true);
        int[] partition = GraphIO.readPartition(
                "testCases/infoMapPartition.txt", infoMap.getToNormal()[0]);
        int[] expected = {1, 1, 1, 1, 1, 1, 2, 2, 2, 2, 2, 2, 2, 3, 3, 3, 3, 3, 3, 3, 3, 4, 4, 4, 4};
        Assert.assertArrayEquals(expected, partition);
    }
}
