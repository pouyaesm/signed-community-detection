package network.core;

import network.utils.QuickSort;
import cern.colt.list.IntArrayList;
import cern.colt.map.OpenIntIntHashMap;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

public class ListMatrix extends AbstractMatrix {

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
    protected OpenIntIntHashMap[] toNormal;
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

    private int minRowId;
    private int maxRowId;

    private int minColumnId;
    private int maxColumnId;

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

    public ListMatrix init(float[][] values, int[][] columnIndices, boolean isIdShared, boolean isNormalized){
        int nonZeroCount = 0;
        for(int r = 0 ; r < values.length ; r++){
            for(int c = 0 ; c < values[r].length ; c++){
                float value = values[r][c];
                if(value != 0.0){
                    nonZeroCount++;
                }
            }
        }
        int[] rowsList = new int[nonZeroCount];
        int[] columnsList = new int[nonZeroCount];
        float[] valuesList = new float[nonZeroCount];
        int insertAt = 0;
        for(int r = 0 ; r < values.length ; r++){
            float[] rowValues = values[r];
            int[] rowColumns = columnIndices[r];
            for(int c = 0 ; c < rowValues.length ; c++){
                float value = rowValues[c];
                if(value != 0.0){
                    rowsList[insertAt] = r;
                    columnsList[insertAt] = rowColumns[c];
                    valuesList[insertAt] = value;
                    insertAt++;
                }
            }
        }
        setRows(rowsList);
        setColumns(columnsList);
        setValues(valuesList);
        this.isIdShared = isIdShared;
        // There is no guarantee about the structure of sparse matrix
        setStatus(false, false, isNormalized, false, MODE_NOT_CLONE);
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
        // first sort the rows, then sort columns per row
        QuickSort qSort = new QuickSort(getRows()).sort(!isIdAscending);
        int[] indices = qSort.getIndices();
        qSort.setValues(columns); // now coninue sorting indices based on column values
        // sort columns per row
        // traverse the sorted rows to sort the columns per row
        int duplication = 0;
        for(int start = 0, currentRowId = rows[indices[start]], p = 1; p < rows.length + 1; p++){
            boolean isRowChanged = p == rows.length || currentRowId != rows[indices[p]];
            if(isRowChanged){
                // sort columns of currentRowId that has just been traversed
                qSort.sort(start, p - 1, !isIdAscending);
                // check for duplicated (row, column) among currently sorted pairs
                for(int check = start + 1 ; check < p ; check++){
                    if(columns[indices[check]] == columns[indices[check - 1]]){
                        duplication++;
                    }
                }
                start = p;
                currentRowId = p < rows.length ? rows[indices[p]] : -1;
            }
        }
        boolean isUnique = duplication == 0;
        boolean clone = sortMode == MODE_CLONE;

        // Permute the order of lists based on the sorted indices
        int[] rows = qSort.permute(clone ? getRows().clone() : getRows());
        int[] columns = qSort.permute(clone ? getColumns().clone() : getColumns());
        float[] values = qSort.permute(clone ? getValues().clone() : getValues());

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
        OpenIntIntHashMap[] toNormal = new OpenIntIntHashMap[2]; // for rows and columns
        int[][] toRaw = new int[2][];
        if(isIdShared()){
            toNormal[ROW] = Util.normalizeIds(getRows(), getColumns());
            toNormal[COL] = (OpenIntIntHashMap) toNormal[ROW].clone();
            toRaw[ROW] = new int[toNormal[ROW].size()];
            toRaw[COL] = toRaw[ROW].clone();
        }else{
            toNormal[ROW] = Util.normalizeIds(getRows());
            toNormal[COL] = Util.normalizeIds(getColumns());
            toRaw[ROW] = new int[toNormal[ROW].size()]; // assuming ids are mapped to 0...N-1
            toRaw[COL] = new int[toNormal[COL].size()];
        }
        // Change row and column ids from raw to normal
        for(int p = 0 ; p < rows.length ; p++){
            rows[p] = toNormal[ROW].get(getRows()[p]);
        }
        for(int p = 0 ; p < rows.length ; p++){
            columns[p] = toNormal[COL].get(getColumns()[p]);
        }
        // Construct the toRaw id mapper for rows and columns
        for(int dim = 0 ; dim < 2 ; dim++){
            final int[] toRawDim = toRaw[dim];
            IntArrayList rawIds = toNormal[dim].keys();
            for(int rawId, i = 0 ; i < rawIds.size() ; i++){
                rawId = rawIds.get(i);
                toRawDim[toNormal[dim].get(rawId)] = rawId;
//                toNormal[dim].forEach((rawId, normalId) -> );
            }
//            toNormal[dim].forEach((rawId, normalId) -> toRawDim[normalId] = rawId);
        }
        // Return the value
        if(clone){
            return new ListMatrix().init(rows, columns, getValues().clone(), isIdShared())
                    .setMaps(toNormal, toRaw)
                    .setStatus(isSorted(), isUnique(), true, isIdAscending(), getSortMode());
        }else{
            // Change (min, max) value of row and column ids to normalized values
            minRowId = minColumnId = 0;
            maxRowId = toRaw[ROW].length - 1;
            maxColumnId = toRaw[COL].length - 1;
            setMaps(toNormal, toRaw);
            setStatus(isSorted(), isUnique(), true, isIdAscending(), getSortMode());
            return this;
        }
    }

    /**
     * Normalize the row and column ids to 0...L-1 without missing values
     * Also map the normalized ids back to given raw ids (if any),
     * This is useful for hierarchical matrices which may point to the same entity ids
     * @param clone
     * @param mapToOldIds
     * @return
     */
    public ListMatrix normalize(boolean clone, boolean mapToOldIds){
        int[][] oldRawIds = getToRaw();
        ListMatrix normalizedList = normalize(clone);
        if(oldRawIds == null || !mapToOldIds){
            return normalizedList;
        }
        // Example: [3, 4, 5] -> [0, 1, 2], then sub matrix [1, 2] -> [0, 1]
        // We want to map [0, 1] back to [4, 5] instead of [1, 2]
        int[][] newRawIds = normalizedList.getToRaw();
        OpenIntIntHashMap[] newNormalIds = normalizedList.getToNormal();
        newNormalIds[ROW].clear();
        newNormalIds[COL].clear();
        for(int dim = 0 ; dim < 2 ; dim++){
            for(int normalizedId = 0 ; normalizedId < newRawIds[dim].length ; normalizedId++){
                int oldRawId = oldRawIds[dim][newRawIds[dim][normalizedId]];
                newRawIds[dim][normalizedId] = oldRawId;
                newNormalIds[dim].put(oldRawId, normalizedId);
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
        // swap toNormal and toRaw id maps between row and column
        if(isNormalized()){
            OpenIntIntHashMap toNormalRowTemp = getToNormal()[ROW];
            int[] toRawRowTemp = getToRaw()[ROW];
            transposedList.toNormal = new OpenIntIntHashMap[2];
            transposedList.toRaw = new int[2][];
            transposedList.toNormal[ROW] = getToNormal()[COL];
            transposedList.toNormal[COL] = toNormalRowTemp;
            transposedList.toRaw[ROW] = getToRaw()[COL];
            transposedList.toRaw[COL] = toRawRowTemp;
        }
        transposedList.setStatus(false, isUnique(), isNormalized(), isIdAscending(), getSortMode());
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
        PartitionStatistics statistics = Statistics.partition(partition, this);
        int[] allRows = getRows();
        int[] allColumns = getColumns();
        float[] allValues = getValues();
        int groupCount = statistics.groupCount;
        // Instantiate lists per dimension per partitions
        int[][] rows = new int[groupCount][];
        int[][] columns = new int[groupCount][];
        float[][] values = new float[groupCount][];
        int[] occupied = new int[groupCount]; // No. pairs occupying positions per partitions
        for(int graphId = 0 ; graphId < groupCount ; graphId++){
            int graphSize = statistics.cellCount[graphId];
            if(graphSize > 0) {
                rows[graphId] = new int[graphSize];
                columns[graphId] = new int[graphSize];
                values[graphId] = new float[graphSize];
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
        /*
            Decomposition of a sorted/unique/ascending list remains sorted/unique/ascending
            Parents' toRaw/toNormal mapping is shared with subGraphs, they must be cloned
            or recreated if a change is required, specially for toRaw as it must be changed
            per normalization
         */
        ListMatrix[] lists = new ListMatrix[groupCount];
        for(int pr = 0 ; pr < rows.length ; pr++){
            if(rows[pr] == null) continue; // a sub-graph without link
            lists[pr] = new ListMatrix()
                    .init(rows[pr], columns[pr], values[pr], isIdShared())
                    .setStatus(isSorted(), isUnique(), false, isIdAscending(), getSortMode())
                    .setMaps(getToNormal(), getToRaw());
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
    @Override
    public ListMatrix fold(int[] partition) {
        int[] allRows = getRows();
        int[] allColumns = getColumns();
        float[] allValues = getValues();
        // Number of pairs with no discarded row/column
        PartitionStatistics statistics = Statistics.partition(partition, this);
        int groupCount = statistics.groupCount;
        int validPairs = allRows.length - statistics.discardedCellCount;
//        int[] rows = new int[validPairs];
//        int[] columns = new int[validPairs];
//        float[] values = new float[validPairs];
        OpenIntIntHashMap normalGroupId = Util.normalizeIds(partition);

        int estimatedPairs = (int) (validPairs * (double) groupCount / partition.length);// edge count * K/N
        HashMap<Long, Float> pairs = new HashMap<Long, Float>(estimatedPairs);
//        Map<Integer, Float> pairs = HashIntFloatMaps.newUpdatableMap();
        // map id of row/columns to their raw (unNormalized) row id
        int[][] toRaw = new int[2][];
        toRaw[ROW] = new int[groupCount];
        toRaw[COL] = new int[groupCount];
        // raw ids to normalized row/column ids (group ids)
        OpenIntIntHashMap[] toNormal = new OpenIntIntHashMap[2];
        toNormal[ROW] = normalGroupId;
        toNormal[COL] = (OpenIntIntHashMap) normalGroupId.clone();
        for(int p = 0 ; p < allRows.length ; p++){
            int rowRawGroupId = partition[allRows[p]];
            int columnRawGroupId = partition[allColumns[p]];
            if(rowRawGroupId < 0 || columnRawGroupId < 0){
                continue; // pair is discarded by the partition
            }
            int rowGroupId = normalGroupId.get(rowRawGroupId);
            int columnGroupId = normalGroupId.get(columnRawGroupId);
            long uniqueId = (long) groupCount * rowGroupId + columnGroupId;
            pairs.merge(uniqueId, allValues[p], Float::sum);
            toRaw[ROW][rowGroupId] = rowRawGroupId; // row points to its un-normalized group id
            toRaw[COL][columnGroupId] = columnRawGroupId;
        }
        // Set aggregated links based on folded groups into simple arrays
        final int[] rows = new int[pairs.size()];
        final int[] columns = new int[pairs.size()];
        final float[] values = new float[pairs.size()];
        Iterator<Map.Entry<Long, Float>> keys = pairs.entrySet().iterator();
        int insertAt = 0;
        while(keys.hasNext()){
            Map.Entry<Long, Float> entry = keys.next();
            long uniqueId = entry.getKey();
            rows[insertAt] = (int)(uniqueId / groupCount);
            columns[insertAt] = (int)(uniqueId % groupCount);
            values[insertAt] = (float) pairs.get(uniqueId);
            insertAt++;
        }
        ListMatrix foldedMatrix = new ListMatrix().init(rows, columns, values, isIdShared())
                .setMaps(toNormal, toRaw)
                .setStatus(false, true, true, isIdAscending(), MODE_NOT_CLONE);
        return foldedMatrix;
    }

    /**
     * Return a list of cells with values between the given interval
     * @param lowerBound cells strictly larger than this value
     * @param upperBound cells strictly smaller than this value
     * @return
     */
    public ListMatrix filter(float lowerBound, float upperBound){
        int count = 0; // number of valid cells
        for(int p =0 ; p < values.length ; p++){
            if(values[p] > lowerBound && values[p] < upperBound){
                count++;
            }
        }
        int[] subRows = new int[count];
        int[] subColumns = new int[count];
        float[] subValues = new float[count];
        int insertAt = 0;
        for(int p = 0 ; p < values.length ; p++){
            if(values[p] > lowerBound && values[p] < upperBound){
                subRows[insertAt] = rows[p];
                subColumns[insertAt] = columns[p];
                subValues[insertAt] = values[p];
                insertAt++;
            }
        }
        ListMatrix subList = new ListMatrix().init(subRows, subColumns, subValues, isIdShared())
                .setStatus(isSorted(), isUnique(), isNormalized(), isIdAscending(), getSortMode());
        subList.toNormal = getToNormal();
        subList.toRaw = getToRaw();
        return subList;
    }

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
     * Cells are added or removed to have value(a, b) = value(b, a)
     * @return
     */
    public ListMatrix symmetrize(){
//        Map<Integer, Integer> visitedCell = HashIntIntMaps.newUpdatableMap(2 * rows.length);
        HashMap<Long, Boolean> visitedCell = new HashMap<Long, Boolean>(2 * rows.length);
        int idRange = Util.max(rows, columns) + 1;
        int cellCount = 0;
        for(int p = 0 ; p < rows.length ; p++){
            long uniqueId = idRange * rows[p] + columns[p];
            if(!visitedCell.containsKey(uniqueId)){ // find both the cell and its mirror
                long mirrorUniqueId = idRange * columns[p] + rows[p];
                visitedCell.put(uniqueId, true);
                visitedCell.put(mirrorUniqueId, true);
                cellCount++;
            }
        }
        // Add each cell and its mirror to new list matrix
        int[] symRows = new int[2 * cellCount];
        int[] symColumns = new int[2 * cellCount];
        float[] symValues = new float[2 * cellCount];
        visitedCell.clear();
        int insertAt = 0;
        for(int p = 0 ; p < rows.length ; p++){
            long uniqueId = idRange * rows[p] + columns[p];
            if(!visitedCell.containsKey(uniqueId)){
                symRows[insertAt] = rows[p];
                symColumns[insertAt] = columns[p];
                symValues[insertAt] = values[p];
                insertAt++;
                symRows[insertAt] = columns[p];
                symColumns[insertAt] = rows[p];
                symValues[insertAt] = values[p];
                insertAt++;
                // Mark as visited
                long mirrorUniqueId = idRange * columns[p] + rows[p];
                visitedCell.put(uniqueId, true);
                visitedCell.put(mirrorUniqueId, true);
            }
        }
        // This operation breaks id sort but guarantees uniqueness of cells, and ids are not changed
        ListMatrix symmetric = new ListMatrix().init(symRows, symColumns, symValues, isIdShared())
                .setStatus(false, true, isNormalized(), false, getSortMode())
                .setMaps(getToNormal(), getToRaw());
        return symmetric;
    }

    /**
     * Default sort as ascending and not cloning
     * @return
     */
    public ListMatrix sort(){
        return sort(true, MODE_NOT_CLONE);
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
        listMatrix.toNormal = new OpenIntIntHashMap[2];
        for(int dim = 0 ; dim < 2 ; dim++){
            listMatrix.toRaw[dim] = toRaw[dim].clone();
            listMatrix.toNormal[dim] = toNormal[dim];
        }
        return listMatrix;
    }

    /**
     * Calculate and set the unique number of row and column ids
     */
    private void calculateCounts(boolean isIdShared){
        if(isIdShared){
            ArrayStatistics statistics = Statistics.array(rows, columns);
            this.rowCount = this.columnCount = statistics.uniqueCount;
            this.minRowId = this.minColumnId = statistics.minValue;
            this.maxRowId = this.maxColumnId = statistics.maxValue;
        }else{
            ArrayStatistics rowStatistics = Statistics.array(rows);
            this.rowCount = rowStatistics.uniqueCount;
            this.minRowId = rowStatistics.minValue;
            this.maxRowId = rowStatistics.maxValue;
            ArrayStatistics columnStatistics = Statistics.array(columns);
            this.columnCount = columnStatistics.uniqueCount;
            this.minColumnId = columnStatistics.minValue;
            this.maxColumnId = columnStatistics.maxValue;
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
    public AbstractMatrix newInstance() {
        return new ListMatrix();
    }

    public ListMatrix setMaps(OpenIntIntHashMap[] toNormal, int[][] toRaw){
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

    public int getMaxColumnId() {
        return maxColumnId;
    }

    public int getMaxRowId() {
        return maxRowId;
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

    public OpenIntIntHashMap[] getToNormal() {
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
