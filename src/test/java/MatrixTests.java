import Core.ListMatrix;
import Core.SparseMatrix;
import org.junit.Assert;
import org.junit.Test;

public class MatrixTests {

    @Test
    public void testListMatrixSortUniquely(){
        int[] rows = {      1,  2,      2,      1000,   30, 30};
        int[] columns = {   10, 300,    100,    5,      20, 20};
        float[] values = {  1,  2,      3,      4,      5,  5};
        ListMatrix uniqueList = new ListMatrix().init(rows, columns, values, true)
                .sort(true, ListMatrix.MODE_REMOVE_DUPLICATE);

        int[] expectedRows = {      1,  2,      2,      30,     1000};
        int[] expectedColumns = {   10, 100,    300,    20,     5};
        float[] expectedValues = {  1,  3,      2,      5,      4};

        Assert.assertArrayEquals("rows must be sorted", expectedRows, uniqueList.getRows());
        Assert.assertArrayEquals("columns must be sorted per row", expectedColumns, uniqueList.getColumns());
        Assert.assertArrayEquals(expectedValues, uniqueList.getValues(), (float) 0.0001);
    }

    @Test
    public void testListMatrixNormalize(){
        int[] rows = {      3,  2,      2,      1000};
        int[] columns = {   10, 300,    100,    2};
        float[] values = {  1,  2,      3,      4};
        ListMatrix uniqueList = new ListMatrix().init(rows, columns, values, false)
                .normalize(true);
        ListMatrix uniqueListShared = new ListMatrix().init(rows, columns, values, true)
                .normalize(false); // do not clone for test
        int[] expectedRows = {          0,  1,      1,      2};
        int[] expectedColumns = {       0,  1,      2,      3};
        int[] expectedColumnsShared = { 3,  4,      5,      1};
        float[] expectedValues = {      1,  2,      3,      4};

        Assert.assertArrayEquals(expectedRows, uniqueList.getRows());
        Assert.assertArrayEquals(expectedColumns, uniqueList.getColumns());
        int forRow = 0, forColumn = 1;
        Assert.assertEquals(2, uniqueList.getToNormal()[forRow][1000]); // 1000 -> 2
        Assert.assertEquals(1000, uniqueList.getToRaw()[forRow][2]); // 2 -> 1000
        Assert.assertEquals(1, uniqueList.getToNormal()[forColumn][300]); // 300 -> 1
        Assert.assertEquals(300, uniqueList.getToRaw()[forColumn][1]); // 1 -> 300
        Assert.assertArrayEquals(expectedValues, uniqueList.getValues(), (float) 0.0001);

        Assert.assertArrayEquals(expectedColumnsShared, uniqueListShared.getColumns());
    }

    @Test
    public void testSparseMatrixConstruction(){
        int[] rows = {      5,      1,      1,      2};
        int[] columns = {   1,      2,      3,      5};
        float[] values = {  1.0f,  2.0f,    3.0f,   4.0f};
        ListMatrix listMatrix = new ListMatrix().init(rows, columns, values, false)
                .sort(true, ListMatrix.MODE_REMOVE_DUPLICATE)
                .normalize(false);
        ListMatrix listMatrixShared = new ListMatrix().init(rows, columns, values, true)
                .sort(true, ListMatrix.MODE_REMOVE_DUPLICATE)
                .normalize(false);
        SparseMatrix sparseMatrix = new SparseMatrix(listMatrix);
        SparseMatrix sparseMatrixShared = new SparseMatrix(listMatrixShared);
        // .sort.normalize leads to: rows: 1 -> 0, 2 -> 1, 5 -> 2, columns: 2 -> 0, 3 -> 1, 5 -> 2, 1 -> 3
        float[][] expectedFull = {{2.0f, 3.0f, 0, 0}, {0, 0, 4.0f, 0}, {0, 0, 0, 1.0f}};
        MyAssert.assertArrayEquals(expectedFull, sparseMatrix.getFull(), 0.00001f);
        // In shared mode: .sort.normalize leads to: 1 -> 0, 2 -> 1, 5 -> 2, 3 -> 3
        float[][] expectedFullShared = {{0, 2.0f, 0, 3.0f}, {0, 0, 4.0f, 0}, {1.0f, 0, 0, 0}, {0, 0, 0, 0}};
        MyAssert.assertArrayEquals(expectedFullShared, sparseMatrixShared.getFull(), 0.00001f);
    }
}
