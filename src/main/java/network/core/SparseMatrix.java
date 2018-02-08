package network.core;

import cern.colt.map.OpenIntIntHashMap;

/**
 * Sparse column matrix
 */
public class SparseMatrix extends ListMatrix {

    /**
     * Matrix values per non-null cell
     */
    float[][] sparseValues;


    /**
     * Column indices per row
     */
    int[][] columnIndices;

    public SparseMatrix(){

    }

    public SparseMatrix(ListMatrix listMatrix){
        super(listMatrix);
    }



    /**
     * Build the sparse data structure based on the list of (row, column, value)
     * Assumption: there must be no duplicate (row, column) in the inputs
     * @return
     */
    @Override
    public void onMatrixBuilt() {
        super.onMatrixBuilt();
        if(getRows() == null) return;
        // Populate the sparse data structure with list data
        int[] rows = getRows();
        int[] columns = getColumns();
        float[] values = getValues();
        // build the internal data structure based on normalized ids
        int rowIdRange = Math.max(0, getMaxRowId() + 1); // matrix may be empty
        int[] rowSizes = new int[rowIdRange];
        for(int rowId : rows){
            rowSizes[rowId]++;
        }
        // create column index, sparse values, and occupied counter per row
        this.columnIndices = new int[rowIdRange][];
        this.sparseValues = new float[rowIdRange][];
        int[] occupied = new int[rowIdRange];
        for(int r = 0; r < rowIdRange ; r++){
            int rowSize = rowSizes[r];
            this.columnIndices[r] = new int[rowSize];
            this.sparseValues[r] = new float[rowSize];
            occupied[r] = 0;
        }
        for(int p = 0 ; p < rows.length ; p++){
            int rowId = rows[p];
            this.columnIndices[rowId][occupied[rowId]] = columns[p];
            this.sparseValues[rowId][occupied[rowId]++] = values[p];
        }
    }

    public float getValue(int row, int column){
        int rowId = getToNormal()[0].get(row);
        int columnId = getToNormal()[1].get(column);
        int[] columns = columnIndices[rowId];
        for(int c = 0 ; c < columns.length ; c++){
            if(columns[c] == columnId){
                return sparseValues[rowId][c];
            }
        }
        return Integer.MIN_VALUE; // not found
    }

    /**
     * Decompose the sparse matrix into given partitions
     * Partitions are assumed to be normalized into 0..K-1
     */
    @Override
    public SparseMatrix[] decompose(int[] partition){
        return decompose(partition, null);
    }

    /**
     * Decompose the sparse matrix into given partitions
     * using the provided normalization map
     * @param mapToNormal node ids are changed according to this map
     * @param partition
     * @return
     */
    @Override
    public SparseMatrix[] decompose(int[] partition, OpenIntIntHashMap[] mapToNormal){
        ListMatrix[] decomposed = super.decompose(partition);
        SparseMatrix[] matrices = new SparseMatrix[decomposed.length];
        for(int m = 0 ; m < decomposed.length ; m++){
            if(decomposed[m] == null) continue;
            matrices[m] = (SparseMatrix) newInstance()
                    .init(decomposed[m].normalizeKeepRawIds(mapToNormal, false));
        }
        return matrices;
    }

    /**
     * Get full matrix
     * @return
     */
    public float[][] getFull(){
        float[][] matrix = new float[getRowCount()][getColumnCount()];
        for(int r = 0 ; r < sparseValues.length ; r++){
            int[] columns = columnIndices[r]; // columns of r-th row
            for(int c = 0 ; c < sparseValues[r].length ; c++){
                matrix[r][columns[c]] = sparseValues[r][c];
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
        return sparseValues[rowId];
    }

    /**
     * Get sparse cell values
     * @return
     */
    public float[][] getSparseValues(){
        return sparseValues;
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
    public int[][] getSparseColumns(){
        return columnIndices;
    }

    @Override
    public SparseMatrix clone(){
        SparseMatrix clone = (SparseMatrix) super.clone();
        clone.sparseValues = new float[sparseValues.length][];
        clone.columnIndices = new int[sparseValues.length][];
        for(int r = 0 ; r < sparseValues.length ; r++){
            clone.sparseValues[r] = sparseValues[r].clone();
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
