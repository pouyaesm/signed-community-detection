import Core.ListMatrix;
import Core.SparseMatrix;
import org.junit.Assert;
import org.junit.Test;

public class ListMatrixTests {

    @Test
    public void testListMatrixSortUniquely(){
        int[] rows = {      1,  2,      2,      1000,   30, 30, 5,  5};
        int[] columns = {   10, 300,    100,    5,      20, 20, 6,  3};
        float[] values = {  1,  2,      3,      4,      5,  5,  3,  2};
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
        ListMatrix list = new ListMatrix().init(rows, columns, values, false)
                .normalize(true);
        ListMatrix listShared = new ListMatrix().init(rows, columns, values, true)
                .normalize(true);
        ListMatrix unNormalized = list.unNormalize(true);
        int[] expectedRows = {          0,  1,      1,      2};
        int[] expectedColumns = {       0,  1,      2,      3};
        int[] expectedColumnsShared = { 3,  4,      5,      1};
        float[] expectedValues = {      1,  2,      3,      4};

        Assert.assertArrayEquals(expectedRows, list.getRows());
        Assert.assertArrayEquals(expectedColumns, list.getColumns());
        int forRow = 0, forColumn = 1;
        Assert.assertEquals(2, list.getToNormal()[forRow][1000]); // 1000 -> 2
        Assert.assertEquals(1000, list.getToRaw()[forRow][2]); // 2 -> 1000
        Assert.assertEquals(1, list.getToNormal()[forColumn][300]); // 300 -> 1
        Assert.assertEquals(300, list.getToRaw()[forColumn][1]); // 1 -> 300
        Assert.assertArrayEquals(expectedValues, list.getValues(), (float) 0.0001);
        // check normalized columns in shared mode
        Assert.assertArrayEquals(expectedColumnsShared, listShared.getColumns());
        // check rows and columns when unNormalized back to initial ids
        Assert.assertArrayEquals(rows, unNormalized.getRows());
        Assert.assertArrayEquals(columns, unNormalized.getColumns());
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
        ListMatrix foldedMatrix = new ListMatrix().init(rows, columns, values, true).fold(partitions);
        System.out.println(foldedMatrix.toString());
        // folded matrix ids are sorted descending by default since the original matrix is not sorted
        Assert.assertArrayEquals(new int[]{0, 0}, foldedMatrix.getRows());
        Assert.assertArrayEquals(new int[]{1, 0}, foldedMatrix.getColumns());
        Assert.assertArrayEquals("Intra- or Inter-Group values must be aggregated",
                new float[]{5, 3}, foldedMatrix.getValues(), 0.0001f);
    }
}
