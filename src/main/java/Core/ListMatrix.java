package Core;

public class ListMatrix {

    public static final int MODE_REMOVE_DUPLICATE = 1;
    public static final int MODE_NOT_CLONE = 2;
    public static final int MODE_CLONE = 3;

    private int[] rows;
    private int[] columns;
    private float[] values;
    /**
     * Maps raw id to normalized id
     * toNormal[0] is for rows and toNormal[1] is for columns
     */
    protected int[][] toNormal;
    /**
     * Maps normalized id to raw id
     * toRaw[0] is for rows and toRow[1] is for columns
     */
    protected int[][] toRaw;

    /**
     * Number of unique row ids
     */
    protected int rowCount;
    /**
     * Number of unique column ids
     */
    protected int columnCount;

    /**
     * true if row and column are representative of the same entity
     */
    private boolean isIdShared;

    /**
     * True of row indices are sorted, and column indices are sorted per row
     */
    private boolean isSorted;

    /**
     * If matrix is checked and no duplicate (row, column) is found
     */
    private boolean isUnique;

    /**
     * If row and column indices are normalized to 0...L-1 without missing values
     */
    private boolean isNormalized;

    /**
     * @param rowColumns rowColumns[p] = [row, column]
     * @param values
     */
    public void init(int[][] rowColumns, float[] values, boolean isIdShared){
        int[] rows = new int[rowColumns.length];
        int[] columns = new int[rowColumns.length];
        for(int p = 0 ; p < rowColumns.length ; p++){
            rows[p] = rowColumns[p][0];
            columns[p] = rowColumns[p][1];
        }
        init(rows, columns, values, isIdShared);
    }

    public ListMatrix init(int[] rows, int[] columns, float[] values, boolean isIdShared){
        setRows(rows);
        setColumns(columns);
        setValues(values);
        // calculate number of unique row and column ids
        this.isIdShared = isIdShared;
        if(isIdShared){
            this.rowCount = this.columnCount = Util.uniqueCount(rows, columns);
        }else{
            this.rowCount = Util.uniqueCount(rows);
            this.columnCount = Util.uniqueCount(columns);
        }
        return this;
    }

    /**
     * Return a ListMatrix sorted first by row id and then by column id,
     * with optional removal of duplicate (row, column)s
     * @param isIdAscending
     * @param mode remove duplicates or not clone the list matrix
     * @return
     */
    public ListMatrix sort(boolean isIdAscending, int mode){
        boolean clone = mode == MODE_CLONE || mode == MODE_REMOVE_DUPLICATE;
        // first sort the rows, then sort columns per row
        QuickSort qSortRow = new QuickSort();
        int[] rows = qSortRow.sort(getRows(), !isIdAscending, clone);
        int[] columns = qSortRow.permute(getColumns(), clone);
        float[] values = qSortRow.permute(getValues(), clone);
        // sort columns per row
        int start = 0;
        int currentRowId = rows[start];
        // traverse the sorted rows to sort the columns per row
        QuickSort qSortColumn = new QuickSort();
        int duplication = 0;
        for(int p = 1; p < rows.length; p++){
            if(currentRowId != rows[p]){
                // sort columns of currentRowId that has just been traversed
                qSortColumn.sort(columns, start, p - 1, !isIdAscending, false);
                // apply the permutation on rows and values too
                qSortColumn.permute(rows, false);
                qSortColumn.permute(values, false);
                // check for duplicated (row, column) among currently sorted pairs
                for(int check = start + 1 ; check < p ; check++){
                    if(columns[check] == columns[check - 1]){
                        duplication++;
                    }
                }
                start = p;
                currentRowId = rows[p];
            }
        }
        boolean isUnique = duplication == 0;
        if(isUnique || mode != MODE_REMOVE_DUPLICATE){
            if(clone){
                ListMatrix listMatrix = new ListMatrix().init(rows, columns, values, isIdShared())
                        .setStatus(true, isUnique, isNormalized());
                return listMatrix;
            }else{
                setStatus(true, isUnique, isNormalized());
                return this;
            }
        }
        // remove the duplicated pair
        int uniqueCount = rows.length - duplication;
        int[] uRows = new int[uniqueCount];
        int[] uColumns = new int[uniqueCount];
        float[] uValues = new float[uniqueCount];
        int uniqueIndex = 0;
        for(int p = 0 ; p < rows.length ; p++){
            int row = rows[p];
            int column = columns[p];
            if(p > 0 && row == rows[p - 1] && column == columns[p - 1]){
                continue; // pair is a duplicate
            }
            uRows[uniqueIndex] = rows[p];
            uColumns[uniqueIndex] = columns[p];
            uValues[uniqueIndex] = values[p];
            uniqueIndex++;
        }
        ListMatrix listMatrix = new ListMatrix().init(uRows, uColumns, uValues, isIdShared())
                .setStatus(true, true, isNormalized());
        return listMatrix;
    }


    /**
     * Normalize the row and column ids to 0...L-1 without missing values
     * @param clone
     * @return
     */
    public ListMatrix normalize(boolean clone){
        int[] rows = clone ? new int[getRows().length] : getRows();
        int[] columns = clone ? new int[getColumns().length] : getColumns();
        // Normalize ids
        int[][] toNormal = new int[2][]; // for rows and columns
        int[][] toRaw = new int[2][];
        int forRow = 0;
        int forColumn = 1;
        if(isIdShared()){
            toNormal[forRow] = toNormal[forColumn] = Util.normalizeIds(getRows(), getColumns());
            toRaw[forRow] = toRaw[forColumn] = new int[Util.max(toNormal[forRow]) + 1];
        }else{
            toNormal[forRow] = Util.normalizeIds(getRows());
            toNormal[forColumn] = Util.normalizeIds(getColumns());
            toRaw[forRow] = new int[Util.max(toNormal[forRow]) + 1];
            toRaw[forColumn] = new int[Util.max(toNormal[forColumn]) + 1];
        }
        // Change row and column ids from raw to normal
        for(int p = 0 ; p < rows.length ; p++){
            rows[p] = toNormal[forRow][getRows()[p]];
        }
        for(int p = 0 ; p < rows.length ; p++){
            columns[p] = toNormal[forColumn][getColumns()[p]];
        }
        // Construct the toRaw id mapper for rows and columns
        for(int dim = 0 ; dim < 2 ; dim++){
            for(int rawId = 0 ; rawId < toNormal[dim].length ; rawId++){
                int normalId = toNormal[dim][rawId];
                if(normalId != -1){
                    toRaw[dim][normalId] = rawId;
                }
            }
        }
        // Return the value
        if(clone){
            return new ListMatrix().init(rows, columns, getValues().clone(), isIdShared())
                    .setMaps(toNormal, toRaw)
                    .setStatus(isSorted(), isUnique(), true);
        }else{
            setMaps(toNormal, toRaw);
            setStatus(isSorted(), isUnique(), true);
            return this;
        }
    }

    protected ListMatrix setStatus(boolean isSorted, boolean isUnique, boolean isNormalized){
        this.isSorted = isSorted;
        this.isUnique = isUnique;
        this.isNormalized = isNormalized;
        return this;
    }

    @Override
    public Object clone(){
        int[] rows = getRows().clone();
        int[] columns = getColumns().clone();
        float[] values = getValues().clone();
        ListMatrix listMatrix = new ListMatrix().init(rows, columns, values, isIdShared())
                .setStatus(isSorted(), isUnique(), isNormalized());
        listMatrix.toRaw = new int[2][];
        listMatrix.toNormal = new int[2][];
        for(int dim = 0 ; dim < 2 ; dim++){
            listMatrix.toRaw[dim] = toRaw[dim].clone();
            listMatrix.toNormal[dim] = toNormal[dim].clone();
        }
        return listMatrix;
    }

    @Override
    public String toString() {
        StringBuilder string = new StringBuilder("[");
        for(int p = 0; p < rows.length ; p++){
            string.append("(" + rows[p] + " " + columns[p] + " " + values[p] + ")");
            if(p < rows.length - 1){
                string.append(" ");
            }
        }
        string.append("]");
        return string.toString();
    }

    public ListMatrix setMaps(int[][] toNormal, int[][] toRaw){
        this.toNormal = toNormal;
        this.toRaw = toRaw;
        return this;
    }

    public ListMatrix setColumns(int[] columns) {
        this.columns = columns;
        return this;
    }

    public int[] getRows() {
        return rows;
    }

    public int[] getColumns() {
        return columns;
    }

    public float[] getValues() {
        return values;
    }

    public ListMatrix setRows(int[] rows) {
        this.rows = rows;
        return this;
    }

    public ListMatrix setValues(float[] values) {
        this.values = values;
        return this;
    }

    public boolean isSorted() {
        return isSorted;
    }

    public boolean isUnique() {
        return isUnique;
    }

    public boolean isNormalized() {
        return isNormalized;
    }

    public boolean isIdShared() {
        return isIdShared;
    }

    public int[][] getToNormal() {
        return toNormal;
    }

    public int[][] getToRaw() {
        return toRaw;
    }

    public int getColumnCount() {
        return columnCount;
    }

    public int getRowCount() {
        return rowCount;
    }
}
