package network.core;

import cern.colt.map.OpenIntIntHashMap;
import org.junit.Assert;
import org.junit.Test;

public class ListMatrixTest {

    @Test
    public void testListMatrixSortUniquely(){
        int[] rows = {      1,  2,      2,      30, 1000,   30, 5,  5};
        int[] columns = {   10, 300,    100,    20, 5,      20, 6,  3};
        float[] values = {  1,  2,      3,      5,  4,      5,  3,  2};
        ListMatrix uniqueList = new ListMatrix().init(rows, columns, values, true)
                .sort(true, ListMatrix.MODE_AGGREGATE_DUPLICATE);

        int[] expectedRows = {      1,  2,      2,      5,      5,  30,     1000};
        int[] expectedColumns = {   10, 100,    300,    3,      6,  20,     5};
        // (30, 20) values must be aggregated to 10
        float[] expectedValues = {  1,  3,      2,      2,      3,  10,      4};

        Assert.assertArrayEquals("rows must be sorted", expectedRows, uniqueList.getRows());
        Assert.assertArrayEquals("columns must be sorted per row", expectedColumns, uniqueList.getColumns());
        Assert.assertArrayEquals(expectedValues, uniqueList.getValues(), (float) 0.0001);
    }

    @Test
    public void testListMatrixNormalize(){
        int[] rows = {      3,  2,      2,      1000};
        int[] columns = {   10, 300,    100,    2};
        float[] values = {  1,  2,      3,      4};
        ListMatrix list = new ListMatrix().init(rows, columns, values, false).normalize(true);
        ListMatrix listShared = new ListMatrix().init(rows, columns, values, true).normalize(true);
        ListMatrix unNormalized = list.unNormalize(true);
        int[] expectedRows = {          0,  1,      1,      2};
        int[] expectedColumns = {       0,  1,      2,      3};
        int[] expectedColumnsShared = { 3,  4,      5,      1};
        float[] expectedValues = {      1,  2,      3,      4};

        Assert.assertArrayEquals(expectedRows, list.getRows());
        Assert.assertArrayEquals(expectedColumns, list.getColumns());
        int forRow = 0, forColumn = 1;
        Assert.assertEquals(2, list.getToNormal()[forRow].get(1000)); // 1000 -> 2
        Assert.assertEquals(1000, list.getToRaw()[forRow][2]); // 2 -> 1000
        Assert.assertEquals(1, list.getToNormal()[forColumn].get(300)); // 300 -> 1
        Assert.assertEquals(300, list.getToRaw()[forColumn][1]); // 1 -> 300
        Assert.assertArrayEquals(expectedValues, list.getValues(), (float) 0.0001);
        // check normalized columns in shared mode
        Assert.assertArrayEquals(expectedColumnsShared, listShared.getColumns());
        // check rows and columns when unNormalized back to initial ids
        Assert.assertArrayEquals(rows, unNormalized.getRows());
        Assert.assertArrayEquals(columns, unNormalized.getColumns());
    }

    @Test
    public void testListMatrixAdvancedNormalize(){
        int[] rows = {      1, 2};
        int[] columns = {   3, 1};
        float[] values = {  1, 2};
        // First normalize ordinary
        ListMatrix normalized = new ListMatrix().init(rows, columns, values, true)
                .normalize();
        // Then normalize to custom ids while keeping the old raw maps
        // meaning the final toRaw must be: 4 -> 0 -> 1, 5 -> 1 -> 2, 6 -> 2 -> 3
        OpenIntIntHashMap[] toNormal = new OpenIntIntHashMap[2];
        toNormal[0] = new OpenIntIntHashMap();
        toNormal[1] = new OpenIntIntHashMap();
        toNormal[0].put(0, 4);
        toNormal[1].put(0, 4);
        toNormal[0].put(1, 5);
        toNormal[1].put(1, 5);
        toNormal[0].put(2, 6);
        toNormal[1].put(2, 6);
        ListMatrix customNormalized = normalized.normalizeKeepRawIds(toNormal, true);

        int[] expectedToRaw = {0, 0, 0, 0, 1, 2, 3};
        Assert.assertArrayEquals(expectedToRaw, customNormalized.getToRaw()[0]);
        // Raw id "3" is expected to be mapped to custom normal id "6" instead of "2"
        Assert.assertEquals(6, customNormalized.getToNormal()[0].get(3));
    }

    /**
     * Normalization and Un-normalization is also applied before and after transpose for better coverage
     */
    @Test
    public void testListMatrixTranspose(){
        int[] rows = {      5,      1};
        int[] columns = {   1,      2};
        float[] values = {  1.0f,  2.0f};
        ListMatrix matrix = new ListMatrix().init(rows, columns, values, true).normalize();
        ListMatrix transposed = matrix.transpose(true).unNormalize(true);

        int[] expectedRows = {      1,  2};
        int[] expectedColumns = {   5,  1};

        Assert.assertArrayEquals(expectedRows, transposed.getRows());
        Assert.assertArrayEquals(expectedColumns, transposed.getColumns());
    }

    /**
     * Decomposition of a list into (row, column) partitions
     */
    @Test
    public void testListMatrixDecompose(){
        int[] rows = {      1,      1,  4};
        int[] columns = {   2,      4,  5};
        float[] values = {  1.0f,  2.0f, 3.0f};
        int[] partitions = {-1, 0, 0, -1, 1, 1}; // partition into {1, 2} and {4, 5}
        ListMatrix[] matrices = new ListMatrix().init(rows, columns, values, true).decompose(partitions);
        Assert.assertArrayEquals(new int[]{1}, matrices[0].getRows());
        Assert.assertArrayEquals(new int[]{2}, matrices[0].getColumns());
        Assert.assertArrayEquals(new int[]{4}, matrices[1].getRows());
        Assert.assertArrayEquals(new int[]{5}, matrices[1].getColumns());
    }

    /**
     * Test folding the matrix rows/columns into their corresponding partitions
     */
    @Test
    public void testListMatrixFold(){
        int[] rows = {      1,  1,  4,  6};
        int[] columns = {   2,  4,  5,  6};
        float[] values = {  1,  3,  4,  4};
        int[] partitions = {-1, 0, 1, -1, 0, 1, -1}; // partition into {1, 4} and {2, 5}, discard {6}
        ListMatrix foldedMatrix = new ListMatrix().init(rows, columns, values, true)
                .fold(partitions);
        // folded matrix ids are sorted descending by default since the original matrix is not sorted
        Assert.assertArrayEquals(new int[]{0, 0}, foldedMatrix.getRows());
        Assert.assertArrayEquals(new int[]{0, 1}, foldedMatrix.getColumns());
        Assert.assertArrayEquals("Intra- or Inter-Group values must be aggregated",
                new float[]{3, 5}, foldedMatrix.getValues(), 0.0001f);
    }

    @Test
    public void testListMatrixFilter() throws Exception{
        ListMatrix listMatrix = GraphIO.readListMatrix("testCases/threeEdges.txt", false);
        ListMatrix positive = listMatrix.filter(0, Float.POSITIVE_INFINITY);
        ListMatrix negative = listMatrix.filter(Float.NEGATIVE_INFINITY, 0);

        Assert.assertArrayEquals(new int[]{1, 1}, positive.getRows());
        Assert.assertArrayEquals(new int[]{2, 7}, positive.getColumns());
        Assert.assertArrayEquals(new float[]{10, 12}, positive.getValues(), .00001f);

        Assert.assertArrayEquals(new int[]{5}, negative.getRows());
        Assert.assertArrayEquals(new int[]{1}, negative.getColumns());
        Assert.assertArrayEquals(new float[]{-11.5f}, negative.getValues(), .00001f);
    }

    @Test
    public void testSymmetric(){
        // Returns a symmetric version of given list matrix
        int[] rows = {      1,  2,  5,  3};
        int[] columns = {   5,  1,  1,  4};
        float[] values = {  1,  4,  2,  6};
        ListMatrix symmetric = new ListMatrix().init(rows, columns, values, true)
                .symmetrize().sort();
        Assert.assertArrayEquals(new int[]{     1, 1, 2, 3, 4,  5}, symmetric.getRows());
        Assert.assertArrayEquals(new int[]{     2, 5, 1, 4, 3,  1}, symmetric.getColumns());
        Assert.assertArrayEquals(new float[]{   4, 1, 4, 6, 6,  1}, symmetric.getValues(), 0.01f);
    }

    @Test
    public void testSparseInitialize(){
        int[][] columnIndices = {{0, 1}, {2, 4, 6}};
        float[][] values = {{2, 5}, {3, 0, 7}};
        ListMatrix listMatrix = new ListMatrix().init(values, columnIndices, true, true);
        Assert.assertArrayEquals(new int[]{0, 0, 1, 1}, listMatrix.getRows());
        Assert.assertArrayEquals(new int[]{0, 1, 2, 6}, listMatrix.getColumns());
        Assert.assertArrayEquals(new float[]{2, 5, 3, 7}, listMatrix.getValues(), 0);
    }
}
