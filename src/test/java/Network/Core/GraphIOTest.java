package Network.Core;

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
    public void testReadFromDirectory() throws Exception{
//        ListMatrix[] listMatrix = GraphIO.readListMatrix("testCases", false);
    }
}
