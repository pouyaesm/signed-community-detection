package Core;

public class ListMatrix extends BaseMatrix{

    public static final int MODE_REMOVE_DUPLICATE = 1; // 0001
    public static final int MODE_AGGREGATE_DUPLICATE = 3; // 0011
    public static final int MODE_NOT_CLONE = 4; // 0100
    public static final int MODE_CLONE = 8; // 1000
    public static final int ROW = 0; // index of row related data
    public static final int COL = 1; // index of column related data

    private int[] rows;
    private int[] columns;
    private float[] values;
    /**
     * Maps raw id to normalized id
     * toNormal[0] is for rows and toNormal[1] is for columns
     */
    protected int[][] toNormal;
    /**
     * Maps normalized id to a raw id
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
     * Mode used for sorting the List
     */
    private int sortMode;

    /**
     * Are row and column ids are increasing (ascending) or not
     */
    private boolean isIdAscending;

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
        this.isIdShared = isIdShared;
        // calculate number of unique row and column ids
        calculateCounts(isIdShared);
        return this;
    }

    /**
     * Initialize with a full matrix
     * @param matrix
     * @return
     */
    public ListMatrix init(float[][] matrix, boolean isIdShared){
        int nonZeroCount = 0;
        for(int r = 0 ; r < matrix.length ; r++){
            for(int c = 0 ; c < matrix.length ; c++){
                if(matrix[r][c] != 0.0){
                    nonZeroCount++;
                }
            }
        }
        int[] rows = new int[nonZeroCount];
        int[] columns = new int[nonZeroCount];
        float[] values = new float[nonZeroCount];
        int insertAt = 0;
        for(int r = 0 ; r < matrix.length ; r++){
            for(int c = 0 ; c < matrix.length ; c++){
                float value = matrix[r][c];
                if(value != 0.0){
                    rows[insertAt] = r;
                    columns[insertAt] = c;
                    values[insertAt] = value;
                    insertAt++;
                }
            }
        }
        setRows(rows);
        setColumns(columns);
        setValues(values);
        this.isIdShared = isIdShared;
        setStatus(true, true, true, true, MODE_NOT_CLONE);
        // calculate number of unique row and column ids
        calculateCounts(isIdShared);
        return this;
    }

    /**
     * Return a ListMatrix sorted first by row id and then by column id,
     * with optional removal of duplicate (row, column)s
     * @param isIdAscending
     * @param sortMode remove duplicates or not clone the list matrix
     * @return
     */
    public ListMatrix sort(boolean isIdAscending, int sortMode){
        boolean clone = sortMode == MODE_CLONE ||
                (sortMode & MODE_REMOVE_DUPLICATE) != 0;
        // first sort the rows, then sort columns per row
        QuickSort qSortRow = new QuickSort();
        int[] rows = qSortRow.sort(getRows(), !isIdAscending, clone);
        int[] columns = qSortRow.permute(getColumns(), clone);
        float[] values = qSortRow.permute(getValues(), clone);
        // sort columns per row
        // traverse the sorted rows to sort the columns per row
        QuickSort qSortColumn = new QuickSort();
        int duplication = 0;
        for(int start = 0, currentRowId = rows[start], p = 1; p < rows.length + 1; p++){
            boolean isRowChanged = p == rows.length || currentRowId != rows[p];
            if(isRowChanged){
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
                currentRowId = p < rows.length ? rows[p] : -1;
            }
        }
        boolean isUnique = duplication == 0;
        if(isUnique || (sortMode & MODE_REMOVE_DUPLICATE) == 0){
            if(clone){
                ListMatrix listMatrix = new ListMatrix().init(rows, columns, values, isIdShared())
                        .setStatus(true, isUnique, isNormalized(), isIdAscending, sortMode);
                return listMatrix;
            }else{
                setStatus(true, isUnique, isNormalized(), isIdAscending, sortMode);
                return this;
            }
        }
        // remove/aggregate the duplicated pair
        int uniqueCount = rows.length - duplication;
        int[] uRows = new int[uniqueCount];
        int[] uColumns = new int[uniqueCount];
        float[] uValues = new float[uniqueCount];
        int uniqueIndex = 0;
        for(int p = 0 ; p < rows.length ; p++){
            int row = rows[p];
            int column = columns[p];
            if(p > 0 && row == rows[p - 1] && column == columns[p - 1]){
                if(sortMode == MODE_AGGREGATE_DUPLICATE){
                    // aggregate the duplicate value with corresponding values
                    uValues[uniqueIndex - 1] += values[p];
                }
                continue; // pair is a duplicate
            }
            uRows[uniqueIndex] = rows[p];
            uColumns[uniqueIndex] = columns[p];
            uValues[uniqueIndex] = values[p];
            uniqueIndex++;
        }
        ListMatrix listMatrix = new ListMatrix().init(uRows, uColumns, uValues, isIdShared())
                .setStatus(true, true, isNormalized(), isIdAscending(), getSortMode());
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
        if(isIdShared()){
            toNormal[ROW] = toNormal[COL] = Util.normalizeIds(getRows(), getColumns());
            toRaw[ROW] = toRaw[COL] = new int[Util.max(toNormal[ROW]) + 1];
        }else{
            toNormal[ROW] = Util.normalizeIds(getRows());
            toNormal[COL] = Util.normalizeIds(getColumns());
            toRaw[ROW] = new int[Util.max(toNormal[ROW]) + 1];
            toRaw[COL] = new int[Util.max(toNormal[COL]) + 1];
        }
        // Change row and column ids from raw to normal
        for(int p = 0 ; p < rows.length ; p++){
            rows[p] = toNormal[ROW][getRows()[p]];
        }
        for(int p = 0 ; p < rows.length ; p++){
            columns[p] = toNormal[COL][getColumns()[p]];
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
                    .setStatus(isSorted(), isUnique(), true, isIdAscending(), getSortMode());
        }else{
            setMaps(toNormal, toRaw);
            setStatus(isSorted(), isUnique(), true, isIdAscending(), getSortMode());
            return this;
        }
    }

    /**
     * Normalize the row and column ids to 0...L-1 without missing values
     * Also map the normalized ids back to existing raw ids (if any) before normalization,
     * This is useful for hierarchical matrices which shared raw ids
     * @param clone
     * @param keepOldRawIds
     * @return
     */
    public ListMatrix normalize(boolean clone, boolean keepOldRawIds){
        int[][] oldRawIds = getToRaw();
        ListMatrix normalizedList = normalize(clone);
        if(oldRawIds == null || !keepOldRawIds){
            return normalizedList;
        }
        // Example: [3, 4, 5] -> [0, 1, 2], [1, 2] -> [0, 1]
        // We want to map [0, 1] back to [4, 5] instead of [1, 2]
        int[][] newRawIds = normalizedList.getToRaw();
        for(int dim = 0 ; dim < 2 ; dim++){
            for(int normalizedId = 0 ; normalizedId < newRawIds[dim].length ; normalizedId++){
                int newRawId = newRawIds[dim][normalizedId];
                newRawIds[dim][normalizedId] = oldRawIds[dim][newRawId];
            }
        }
        return normalizedList;
    }

    /**
     * Un-normalize a list into previous un-normalized state
     * @param clone
     * @return
     */
    public ListMatrix unNormalize(boolean clone){
        if(!isNormalized()){
            try {
                throw new Exception("ListMatrix was not normalized before");
            } catch (Exception e) {
                e.printStackTrace();
            }
            return null;
        }
        int[] uRows = clone ? new int[rows.length] : rows;
        int[] uColumns = clone ? new int[columns.length] : columns;
        for(int p = 0 ; p < rows.length ; p++){
            uRows[p] = toRaw[ROW][rows[p]];
            uColumns[p] = toRaw[COL][columns[p]];
        }
        ListMatrix unNormalizedList = clone ? new ListMatrix() : this;
        if(clone){
            unNormalizedList.init(uRows, uColumns, getValues().clone(), isIdShared());
        }else{
            setMaps(null, null); // remove the normalization maps
        }
        // unNormalization could break the id order if the list was normalized before being sorted
        unNormalizedList.setStatus(false, isUnique(), false, isIdAscending(), getSortMode());
        return unNormalizedList;
    }

    /**
     * Transpose the matrix list by switching rows with columns
     * @param clone
     * @return
     */
    public ListMatrix transpose(boolean clone){
        int[] tRows = clone ? new int[rows.length] : columns;
        int[] tColumns = clone ? new int[rows.length] : rows;
        ListMatrix transposedList;
        if(clone) {
            for (int p = 0; p < rows.length; p++) {
                tRows[p] = columns[p];
                tColumns[p] = rows[p];
            }
            transposedList = new ListMatrix()
                    .init(tRows, tColumns, getValues().clone(), isIdShared());
        }else{
            transposedList = this;
            rows = tRows;
            columns = tColumns;
            int rowCountTemp = getRowCount();
            rowCount = getColumnCount();
            columnCount = rowCountTemp;
        }
        // resort the rows and columns since they are swapped
        if(isSorted()){
            transposedList.sort(isIdAscending(), getSortMode());
        }
        // swap toNormal and toRaw id maps between row and column
        if(isNormalized()){
            int[] toNormalRowTemp = getToNormal()[ROW];
            int[] toRawRowTemp = getToRaw()[ROW];
            transposedList.toNormal = new int[2][];
            transposedList.toRaw = new int[2][];
            transposedList.toNormal[ROW] = getToNormal()[COL];
            transposedList.toNormal[COL] = toNormalRowTemp;
            transposedList.toRaw[ROW] = getToRaw()[COL];
            transposedList.toRaw[COL] = toRawRowTemp;
        }
        transposedList.setStatus(isSorted(), isUnique(), isNormalized(), isIdAscending(), getSortMode());
        return transposedList;
    }

    /**
     * Decompose the list into K lists partitioned by the input,
     * negative partitions are regarded as discarding the corresponding row or column,
     * partitions are assumed to be normalized into 0...K-1,
     * Of course pairs between different partitions will be ignored
     * @param partition partitions[i] = k means placing i-th row and in list k
     * @return
     */
    public ListMatrix[] decompose(int[] partition){
        PartitionStatistics statistics = MatrixStatistics.partitionStatistics(partition, this);
        int[] allRows = getRows();
        int[] allColumns = getColumns();
        float[] allValues = getValues();
        int groupCount = statistics.groupCount;
        // Instantiate lists per dimension per partitions
        int[][] rows = new int[groupCount][];
        int[][] columns = new int[groupCount][];
        float[][] values = new float[groupCount][];
        int[] occupied = new int[groupCount]; // No. pairs occupying positions per partitions
        for(int pr = 0 ; pr < groupCount ; pr++){
            int groupSize = statistics.groupSizes[pr];
            if(groupSize > 0) {
                rows[pr] = new int[groupSize];
                columns[pr] = new int[groupSize];
                values[pr] = new float[groupSize];
            }
        }
        // Fill in lists with row column indices
        for(int p = 0 ; p < allRows.length ; p++){
            int row = allRows[p];
            int column = allColumns[p];
            int groupId = partition[row];
            if(groupId >= 0 && groupId == partition[column]){
                int pos = occupied[groupId];
                rows[groupId][pos] = row;
                columns[groupId][pos] = column;
                values[groupId][pos] = allValues[p];
                occupied[groupId]++;
            }
        }
        // Decomposition of a sorted/unique/ascending list remains sorted/unique/ascending
        // Normal/Row mappings remain the same after decomposition, so they are set for all
        // partitions as a SHARED resource
        ListMatrix[] lists = new ListMatrix[groupCount];
        for(int pr = 0 ; pr < rows.length ; pr++){
            lists[pr] = new ListMatrix()
                    .init(rows[pr], columns[pr], values[pr], isIdShared())
                    .setStatus(isSorted(), isUnique(), false, isIdAscending(), getSortMode())
                    .setMaps(getToNormal(), getToRaw()); // shared maps
        }
        return lists;
    }

    /**
     * Fold the rows/columns and their pairs inside each partition,
     * Values are aggregated inside a partition or between two partitions,
     * GroupIds are assumed to be normalized to 0...K-1
     * @param partition partitions[i] = k means placing i-th row and column in list k
     * @return
     */
    public ListMatrix fold(int[] partition) {
        int[] allRows = getRows();
        int[] allColumns = getColumns();
        float[] allValues = getValues();
        // Number of pairs with no discarded row/column
        PartitionStatistics statistics = MatrixStatistics.partitionStatistics(partition, this);
        int validPairs = allRows.length - statistics.discardedCount;
        int[] rows = new int[validPairs];
        int[] columns = new int[validPairs];
        float[] values = new float[validPairs];
        int[] normalGroupId = Util.normalizeIds(partition);
        int[][] toRaw = new int[2][]; // map id of row/columns to their raw (unNormalized) group id
        int[][] toNormal = new int[2][]; // raw group ids to normalized group ids used as row/column ids
        toRaw[ROW] = toRaw[COL] = new int[statistics.groupCount];
        toNormal[ROW] = new int[statistics.maxGroupId + 1];
        toNormal[COL] = new int[statistics.maxGroupId + 1];
        int insertAt = 0;
        for(int p = 0 ; p < allRows.length ; p++){
            int rowRawGroupId = partition[allRows[p]];
            int columnRawGroupId = partition[allColumns[p]];
            if(rowRawGroupId < 0 || columnRawGroupId < 0){
                continue; // pair is discarded by the partition
            }
            int rowGroupId = normalGroupId[rowRawGroupId];
            int columnGroupId = normalGroupId[columnRawGroupId];
            rows[insertAt] = rowGroupId;
            columns[insertAt] = columnGroupId;
            values[insertAt] = allValues[p];
            toRaw[ROW][rowGroupId] = rowRawGroupId;
            toRaw[COL][columnGroupId] = columnRawGroupId;
            toNormal[ROW][rowRawGroupId] = rowGroupId;
            toNormal[COL][columnRawGroupId] = columnGroupId;
            insertAt++;
        }
        ListMatrix foldedMatrix = new ListMatrix().init(rows, columns, values, isIdShared())
                .sort(isIdAscending(), MODE_AGGREGATE_DUPLICATE)
                .setMaps(toNormal, toRaw)
                .setStatus(true, true, true, isIdAscending(), MODE_AGGREGATE_DUPLICATE);
        return foldedMatrix;
    }
//    /**
//     * Decompose the list into K lists partitioned by the input
//     * @param partitions partitions[i] = k means placing i-th row and column in list k
//     * @return
//     */
//    public ListMatrix[] decompose(int[] partitions){
//        int[][] partitions = new int[2][];
//        partitions[0] = partitions[1] = partitions;
//        return decompose(partitions);
//    }

    protected ListMatrix setStatus(boolean isSorted, boolean isUnique
            , boolean isNormalized, boolean isIdAscending, int sortMode){
        this.isSorted = isSorted;
        this.isUnique = isUnique;
        this.isNormalized = isNormalized;
        this.isIdAscending = isIdAscending;
        this.sortMode = sortMode;
        return this;
    }

    /**
     * Default sort as ascending and remove duplicates (+ clone)
     * @return
     */
    public ListMatrix sort(){
        return sort(true, MODE_REMOVE_DUPLICATE);
    }

    /**
     * Default normalization without cloning
     * @return
     */
    public ListMatrix normalize(){
        return normalize(false);
    }

    /**
     * Default unNormalization without cloning
     * @return
     */
    public ListMatrix unNormalize(){
        return unNormalize(false);
    }

    /**
     * Default transpose without cloning
     * @return
     */
    public ListMatrix transpose(){
        return transpose(false);
    }

    @Override
    public Object clone(){
        int[] rows = getRows().clone();
        int[] columns = getColumns().clone();
        float[] values = getValues().clone();
        ListMatrix listMatrix = new ListMatrix().init(rows, columns, values, isIdShared())
                .setStatus(isSorted(), isUnique(), isNormalized(), isIdAscending(), getSortMode());
        listMatrix.toRaw = new int[2][];
        listMatrix.toNormal = new int[2][];
        for(int dim = 0 ; dim < 2 ; dim++){
            listMatrix.toRaw[dim] = toRaw[dim].clone();
            listMatrix.toNormal[dim] = toNormal[dim].clone();
        }
        return listMatrix;
    }

    /**
     * Calculate and set the unique number of row and column counts
     */
    private void calculateCounts(boolean isIdShared){
        if(isIdShared){
            this.rowCount = this.columnCount = Util.uniqueCount(rows, columns);
        }else{
            this.rowCount = Util.uniqueCount(rows);
            this.columnCount = Util.uniqueCount(columns);
        }
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

    @Override
    public BaseMatrix newInstance() {
        return new ListMatrix();
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

    public boolean isIdAscending() {
        return isIdAscending;
    }

    public int getSortMode() {
        return sortMode;
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
