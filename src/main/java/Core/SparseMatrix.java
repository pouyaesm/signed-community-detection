package Core;

/**
 * Sparse column matrix
 */
public class SparseMatrix {
    /**
     * Input List matrix
     */
    private ListMatrix listMatrix;

    /**
     * Matrix values per non-null cell
     */
    float[][] values;

    /**
     * column count per size
     */
    int[] rowSizes;

    /**
     * Column indices per row
     */
    int[][] columnIndices;

    public SparseMatrix(){

    }

    /**
     *
     * @param listMatrix
     */
    public SparseMatrix(ListMatrix listMatrix){
        init(listMatrix);
    }

    /**
     * Constructs a sparse matrix, where sparsity is imposed on columns
     * Assumption: there must be no duplicate (row, column) in the input
     * @param listMatrix
     */
    private SparseMatrix init(ListMatrix listMatrix){
        if(!listMatrix.isSorted() || !listMatrix.isUnique() || !listMatrix.isNormalized()){
            try {
                throw new Exception("ListMatrix must be sorted, unique, and normalized.");
            } catch (Exception e) {
                e.printStackTrace();
            }
            return null;
        }
        this.listMatrix = listMatrix;
        int[] rows = listMatrix.getRows();
        int[] columns = listMatrix.getColumns();
        float[] values = listMatrix.getValues();
        // build the internal data structure based on normalized ids
        int rowCount = listMatrix.getRowCount();
        rowSizes = new int[rowCount];
        columnIndices = new int[rowCount][];
        this.values = new float[rowCount][];
        for(int p = 0 ; p < rows.length ; p++){
            rowSizes[rows[p]]++;
        }
        // create column index, sparse values, and occupied counter per row
        int[] occupied = new int[rowCount];
        for(int r = 0; r < rowSizes.length ; r++){
            int rowSize = rowSizes[r];
            columnIndices[r] = new int[rowSize];
            this.values[r] = new float[rowSize];
            occupied[r] = 0;
        }
        for(int p = 0 ; p < rows.length ; p++){
            int rowId = rows[p];
            this.columnIndices[rowId][occupied[rowId]] = columns[p];
            this.values[rowId][occupied[rowId]++] = values[p];
        }
        return this;
    }


    public float getValue(int row, int column){
        int rowId = listMatrix.getToNormal()[0][row];
        int columnId = listMatrix.getToNormal()[1][column];
        int[] columns = columnIndices[rowId];
        for(int c = 0 ; c < columns.length ; c++){
            if(columns[c] == columnId){
                return values[rowId][c];
            }
        }
        return Integer.MIN_VALUE; // not found
    }

    /**
     * Transpose the sparse matrix
     * @return
     */
    public SparseMatrix transpose(boolean clone){
        ListMatrix transposedList = getListMatrix().transpose(clone);
        return clone ? new SparseMatrix(transposedList) : init(transposedList);
    }

    /**
     * Transpose the sparse matrix with cloning as default
     * @return
     */
    public SparseMatrix transpose(){
        return transpose(true);
    }

    /**
     * Get full matrix
     * @return
     */
    public float[][] getFull(){
        float[][] matrix = new float[getListMatrix().getRowCount()][getListMatrix().getColumnCount()];
        for(int r = 0 ; r < values.length ; r++){
            int[] columns = columnIndices[r]; // columns of r-th row
            for(int c = 0 ; c < values[r].length ; c++){
                matrix[r][columns[c]] = values[r][c];
            }
        }
        return matrix;
    }

    public ListMatrix getListMatrix() {
        return listMatrix;
    }

    @Override
    public Object clone(){
        SparseMatrix clone = new SparseMatrix();
        clone.listMatrix = (ListMatrix) getListMatrix().clone();
        clone.rowSizes = rowSizes.clone();
        clone.values = new float[values.length][];
        clone.columnIndices = new int[values.length][];
        for(int r = 0 ; r < values.length ; r++){
            clone.values[r] = values[r].clone();
            clone.columnIndices[r] = columnIndices[r].clone();
        }
        return clone;
    }
}
