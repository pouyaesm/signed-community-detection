import Core.ListMatrix;
import Core.SparseMatrix;
import org.junit.Test;

public class SparseMatrixTests {

    @Test
    public void testSparseMatrixConstruction(){
        int[] rows = {      5,      1,      1,      2};
        int[] columns = {   1,      2,      3,      5};
        float[] values = {  1.0f,  2.0f,    3.0f,   4.0f};
        ListMatrix listMatrix = new ListMatrix().init(rows, columns, values, false)
                .sort().normalize(false);
        ListMatrix listMatrixShared = new ListMatrix().init(rows, columns, values, true)
                .sort().normalize(false);
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
