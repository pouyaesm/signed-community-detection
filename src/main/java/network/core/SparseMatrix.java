package network.core;

/**
 * Sparse column matrix
 */
public class SparseMatrix extends AbstractMatrix {

    /**
     * Input List matrix
     */
    private ListMatrix listMatrix;

    /**
     * Matrix values per non-null cell
     */
    float[][] values;


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
        if(listMatrix != null) {
            init(listMatrix);
        }
    }

    /**
     * Constructs a sparse matrix, where sparsity is imposed on columns
     * Assumption: there must be no duplicate (row, column) in the input
     * @param listMatrix
     */
    public SparseMatrix init(ListMatrix listMatrix){
        if(!listMatrix.isNormalized()){
            try {
                throw new Exception("ListMatrix must be normalized.");
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
        int rowIdRange = listMatrix.getMaxRowId() + 1;
        int[] rowSizes = new int[rowIdRange];
        columnIndices = new int[rowIdRange][];
        this.values = new float[rowIdRange][];
        for(int rowId : rows){
            rowSizes[rowId]++;
        }
        // create column index, sparse values, and occupied counter per row
        int[] occupied = new int[rowIdRange];
        for(int r = 0; r < rowIdRange ; r++){
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
        int rowId = listMatrix.getToNormal()[0].get(row);
        int columnId = listMatrix.getToNormal()[1].get(column);
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
    @Override
    public SparseMatrix transpose(boolean clone){
        ListMatrix transposedList = getListMatrix().transpose(clone);
        return clone ? newInstance().init(transposedList) : init(transposedList);
    }

    /**
     * Fold the sparse matrix for the given partition
     */
    @Override
    public SparseMatrix fold(int[] partition){
        ListMatrix foldedList = getListMatrix().fold(partition);
        return newInstance().init(foldedList);
    }

    /**
     * Decompose the sparse matrix into given partitions
     * Partitions are assumed to be normalized into 0..K-1
     */
    @Override
    public SparseMatrix[] decompose(int[] partition){
        ListMatrix[] decomposedLists = getListMatrix().decompose(partition);
        SparseMatrix[] matrices = new SparseMatrix[decomposedLists.length];
        for(int m = 0 ; m < decomposedLists.length ; m++){
            matrices[m] = decomposedLists[m] != null ?
                    newInstance().init(decomposedLists[m].normalize(false, true))
                    : newInstance(); // empty matrix
        }
        return matrices;
    }

    /**
     * Return sparse matrix of only filtered values
     * @param lowerBound
     * @param upperBound
     * @see ListMatrix#filter(float, float)
     * @return
     */
    public SparseMatrix filter(float lowerBound, float upperBound){
        return newInstance().init(getListMatrix().filter(lowerBound, upperBound));
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

    /**
     * Get sparse cell values of given row id
     * @param rowId
     * @return
     */
    public float[] getValues(int rowId){
        return values[rowId];
    }

    /**
     * Get sparse cell values
     * @return
     */
    public float[][] getValues(){
        return values;
    }

    /**
     * Get column indices of given row id
     * @param rowId
     * @return
     */
    public int[] getColumns(int rowId){
        return columnIndices[rowId];
    }

    /**
     * Get column indices
     * @return
     */
    public int[][] getColumns(){
        return columnIndices;
    }

    public ListMatrix getListMatrix() {
        return listMatrix;
    }

    /**
     * Whether matrix has a non-empty list of cells
     * @return
     */
    public boolean hasList(){
        return getListMatrix() != null && getListMatrix().getRowCount() > 0;
    }

    @Override
    public Object clone(){
        SparseMatrix clone = newInstance();
        clone.listMatrix = (ListMatrix) getListMatrix().clone();
        clone.values = new float[values.length][];
        clone.columnIndices = new int[values.length][];
        for(int r = 0 ; r < values.length ; r++){
            clone.values[r] = values[r].clone();
            clone.columnIndices[r] = columnIndices[r].clone();
        }
        return clone;
    }

    /**
     * This is implemented by subclasses for instantiations
     * @return
     */
    @Override
    public SparseMatrix newInstance(){
        return new SparseMatrix();
    }
}
