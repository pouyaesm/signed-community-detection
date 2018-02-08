package network.core;

import cern.colt.list.IntArrayList;
import network.utils.QuickSort;
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
     * maps[0] is for rows and maps[1] is for columns
     */
    protected OpenIntIntHashMap[] toNormal;
    /**
     * Maps normalized id to a raw id
     * toRaw[0] is for rows and toRow[1] is for columns
     */
    protected int[][] toRaw;

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
     * Number of unique row ids
     */
    protected int rowCount;
    /**
     * Number of unique column ids
     */
    protected int columnCount;

    public ListMatrix(){

    }

    public ListMatrix(ListMatrix listMatrix){
        init(listMatrix);
    }

    /**
     * Core initialization method used in all other types of initializations
     * @param rows
     * @param columns
     * @param values
     * @param isIdShared
     * @return
     */
    public ListMatrix init(int[] rows, int[] columns, float[] values, boolean isIdShared){
        setRows(rows);
        setColumns(columns);
        setValues(values);
        this.isIdShared = isIdShared;
        calculateStatistics(isIdShared); // calculate number of unique row and column ids
        onMatrixBuilt(); // to let extended classes build their data structures on top
        return this;
    }

    public ListMatrix init(ListMatrix list){
        setMaps(list.getToNormal(), list.getToRaw());
        setStatus(list.isSorted(), list.isUnique(), list.isNormalized(),
                list.isIdAscending(), list.getSortMode());
        return init(list.getRows(),
                list.getColumns(), list.getValues(), list.isIdShared());
    }
    /**
     * @param rowColumns rowColumns[p] = [row, column]
     * @param values
     */
    public ListMatrix init(int[][] rowColumns, float[] values, boolean isIdShared){
        int[] rows = new int[rowColumns.length];
        int[] columns = new int[rowColumns.length];
        for(int p = 0 ; p < rowColumns.length ; p++){
            rows[p] = rowColumns[p][0];
            columns[p] = rowColumns[p][1];
        }
        return init(rows, columns, values, isIdShared);
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
        setStatus(true, true, true, true, MODE_NOT_CLONE);
        return init(rows, columns, values, isIdShared);
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
        // There is no guarantee about the structure of sparse matrix
        return init(rowsList, columnsList, valuesList, isIdShared);
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
                ListMatrix listMatrix = newInstance().init(rows, columns, values, isIdShared())
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
        ListMatrix listMatrix = newInstance().init(uRows, uColumns, uValues, isIdShared())
                .setStatus(true, true, isNormalized(), isIdAscending(), getSortMode());
        return listMatrix;
    }

    /**
     * Normalize the row and column ids according to the given normalization vectors
     * @param toNormal maps raw id to normalized id, it will be built if not provided
     * @param mapToRaw maps normalized ids back to raw ids, it will be built if not provided
     * @param clone
     * @return
     */
    public ListMatrix normalize(OpenIntIntHashMap[] mapToNormal, int[][] mapToRaw, boolean clone){
        if(isEmpty()) return clone ? clone() : this;
        int[] rows = clone ? new int[getRows().length] : getRows();
        int[] columns = clone ? new int[getColumns().length] : getColumns();
        // Create normalization data structure
        OpenIntIntHashMap[] toNormal = new OpenIntIntHashMap[2];// for rows and columns
        if(mapToNormal == null) {
            if(isIdShared()) {
                toNormal[ROW] = Util.normalizeIds(getRows(), getColumns());
                toNormal[COL] = (OpenIntIntHashMap) toNormal[ROW].clone();
            }else{
                toNormal[ROW] = Util.normalizeIds(getRows());
                toNormal[COL] = Util.normalizeIds(getColumns());
            }
        }else{
            toNormal = clone ? mapToNormal.clone() : mapToNormal;
        }
        int minRowId = Integer.MAX_VALUE;
        int maxRowId = Integer.MIN_VALUE;
        // Change row ids from raw to normal
        for(int rowId, p = 0 ; p < rows.length ; p++){
            rows[p] = rowId = toNormal[ROW].get(getRows()[p]);
            if(rowId < minRowId){
                minRowId = rowId;
            }
            if(rowId > maxRowId){
                maxRowId = rowId;
            }
        }
        // Change column ids from raw to normal
        int minColumnId = Integer.MAX_VALUE;
        int maxColumnId = Integer.MIN_VALUE;
        for(int columnId, p = 0 ; p < columns.length ; p++){
            columns[p] = columnId = toNormal[COL].get(getColumns()[p]);
            if(columnId < minColumnId){
                minColumnId = columnId;
            }
            if(columnId > maxColumnId){
                maxColumnId = columnId;
            }
        }
        // Unify max and min ids if row and column ids are shared
        if(isIdShared()){
            minRowId = minColumnId = Math.min(minColumnId, minRowId);
            maxRowId = maxColumnId = Math.max(maxRowId, maxColumnId);
        }
        // Create data structure for mapping normalized ids back to raw ids
        int[][] toRaw;
        if(mapToRaw == null){
            toRaw = new int[2][];
            toRaw[ROW] = new int[maxRowId + 1];
            toRaw[COL] = new int[maxColumnId + 1];
        }else{
            toRaw = clone ? mapToRaw.clone() : mapToRaw;
        }
        // Construct the toRaw id mapper from all toNormal elements
        for(int dim = 0 ; dim < 2 ; dim++){
            final int[] toRawDim = toRaw[dim];
            int maxId = dim == ROW ? maxRowId : maxColumnId;
            IntArrayList rawIds = toNormal[dim].keys();
            for(int rawId, i = 0 ; i < rawIds.size() ; i++){
                rawId = rawIds.get(i);
                // check if the extracted (raw, normal) pair is present in the list
                // because toNormal may contain maps out of this graph's scope
                int normalId = toNormal[dim].get(rawId);
                if(normalId <= maxId) {
                    toRawDim[normalId] = rawId;
                }
            }
        }


        // Return the value
        if(clone){
            return newInstance().init(rows, columns, getValues().clone(), isIdShared())
                    .setMaps(toNormal, toRaw)
                    .setStatus(isSorted(), isUnique(), true, isIdAscending(), getSortMode());
        }else{
            // Change (min, max) value of row and column ids to normalized values
            this.minRowId = minRowId;
            this.minColumnId = minColumnId;
            this.maxRowId = maxRowId;
            this.maxColumnId = maxColumnId;
            setMaps(toNormal, toRaw);
            setStatus(isSorted(), isUnique(), true, isIdAscending(), getSortMode());
            return this;
        }
    }

    /**
     * Normalize the ids with provided maps, but maps back to previous raw ids (if any)
     * @param mapToNormal used as the raw id to normal id mapper (will not be cloned)
     * @param clone
     * @return
     */
    public ListMatrix normalizeKeepRawIds(OpenIntIntHashMap[] mapToNormal, boolean clone){
        if(isEmpty()) return clone ? clone() : this;
        int[][] oldRawIds = getToRaw();
        ListMatrix normalizedList = normalize(mapToNormal, null, clone);
        if(oldRawIds == null){
            return normalizedList;
        }
        // Example: [3, 4, 5] -> [0, 1, 2], then sub matrix [1, 2] -> [0, 1]
        // We want to maps [0, 1] back to [4, 5] instead of [1, 2]
        int[][] newRawIds = normalizedList.getToRaw();
        OpenIntIntHashMap[] newNormalIds = normalizedList.getToNormal();
        newNormalIds[ROW].clear();
        newNormalIds[COL].clear();
        for(int dim = 0 ; dim < 2 ; dim++){
            int minId = dim == ROW ? normalizedList.getMinRowId() : normalizedList.getMinColumnId();
            int maxId = dim == ROW ? normalizedList.getMaxRowId() : normalizedList.getMaxColumnId();
            for(int normalizedId = minId ; normalizedId <= maxId ; normalizedId++){
                int oldRawId = oldRawIds[dim][newRawIds[dim][normalizedId]];
                newRawIds[dim][normalizedId] = oldRawId;
                newNormalIds[dim].put(oldRawId, normalizedId);
            }
        }
        return normalizedList;
    }

    public ListMatrix normalize(boolean clone){
        return normalize(null, null, clone);
    }

    public ListMatrix normalizeKeepRawIds(boolean clone){
        return normalizeKeepRawIds(null, clone);
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
        ListMatrix unNormalizedList = clone ? newInstance() : this;
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
        if(isEmpty()) return clone ? clone() : this;
        int[] tRows = clone ? new int[rows.length] : columns;
        int[] tColumns = clone ? new int[rows.length] : rows;
        ListMatrix transposedList;
        if(clone) {
            for (int p = 0; p < rows.length; p++) {
                tRows[p] = columns[p];
                tColumns[p] = rows[p];
            }
            transposedList = newInstance()
                    .init(tRows, tColumns, getValues().clone(), isIdShared());
        }else{
            transposedList = this;
            rows = tRows;
            columns = tColumns;
            int rowCountTemp = getRowCount();
            rowCount = getColumnCount();
            columnCount = rowCountTemp;
        }
        // swap maps and toRaw id maps between row and column
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
     * partitions are ASSUMED to be normalized into 0...K-1,
     * Of course pairs between different partitions will be ignored
     * @param partition partitions[i] = k means placing i-th row and in list k
     * @return
     */
    public ListMatrix[] decompose(int[] partition){
        if(isEmpty()) return new ListMatrix[0];
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
            int nodeCount = statistics.size[graphId];
            if(nodeCount > 0) { // a matrix may have zero occupied cell
                int cellCount = statistics.cellCount[graphId];
                rows[graphId] = new int[cellCount];
                columns[graphId] = new int[cellCount];
                values[graphId] = new float[cellCount];
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
            Parents' toRaw/maps mapping is shared with subGraphs, they must be cloned
            or recreated if a change is required, specially for toRaw as it must be changed
            per normalization
         */
        ListMatrix[] lists = new ListMatrix[groupCount];
        for(int pr = 0 ; pr < rows.length ; pr++){
            lists[pr] = newInstance()
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
        if(isEmpty()) return clone();
        int[] allRows = getRows();
        int[] allColumns = getColumns();
        float[] allValues = getValues();
        // Number of pairs with no discarded row/column
        PartitionStatistics statistics = Statistics.partition(partition, this);
        int groupCount = statistics.groupCount;
        int validPairs = allRows.length - statistics.discardedCellCount;
        OpenIntIntHashMap normalGroupId = Util.normalizeIds(partition);

        int estimatedPairs = (int) (validPairs * (double) groupCount / partition.length);// edge count * K/N
        HashMap<Long, Float> pairs = new HashMap<Long, Float>(estimatedPairs);
        // maps id of row/columns to their raw (unNormalized) row id
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
        ListMatrix foldedMatrix = newInstance().init(rows, columns, values, isIdShared())
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
        if(isEmpty()) return clone();
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
        ListMatrix subList = newInstance().init(subRows, subColumns, subValues, isIdShared())
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
        if(isEmpty()) return clone();
        HashMap<Long, Boolean> visitedCell = new HashMap<Long, Boolean>(2 * rows.length);
        int idRange = Util.max(rows, columns) + 1;
        int cellCount = 0;
        int diagonalCount = 0;
        for(int p = 0 ; p < rows.length ; p++){
            long uniqueId = idRange * rows[p] + columns[p];
            if(!visitedCell.containsKey(uniqueId)){ // find both the cell and its mirror
                visitedCell.put(uniqueId, true);
                cellCount++;
                if(rows[p] == columns[p]){
                    diagonalCount++;
                    continue;
                }
                long mirrorUniqueId = idRange * columns[p] + rows[p];
                visitedCell.put(mirrorUniqueId, true);
            }
        }
        // Add each cell and its mirror to new list matrix
        int symCellCount = 2 * cellCount - diagonalCount; // count diagonal cells only once
        int[] symRows = new int[symCellCount];
        int[] symColumns = new int[symCellCount];
        float[] symValues = new float[symCellCount];
        visitedCell.clear();
        int insertAt = 0;
        for(int p = 0 ; p < rows.length ; p++){
            long uniqueId = idRange * rows[p] + columns[p];
            if(!visitedCell.containsKey(uniqueId)){
                symRows[insertAt] = rows[p];
                symColumns[insertAt] = columns[p];
                symValues[insertAt] = values[p];
                insertAt++;
                if(rows[p] == columns[p]) continue; // row = column
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
        ListMatrix symmetric = newInstance().init(symRows, symColumns, symValues, isIdShared())
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
    public ListMatrix clone(){
        if(isEmpty()) return newInstance();
        int[] rows = getRows().clone();
        int[] columns = getColumns().clone();
        float[] values = getValues().clone();
        ListMatrix listMatrix = newInstance().init(rows, columns, values, isIdShared())
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
    private void calculateStatistics(boolean isIdShared){
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
    public ListMatrix newInstance() {
        return new ListMatrix();
    }

    @Override
    public void onMatrixBuilt() {

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

    /**
     * Whether list has no element
     * @return
     */
    public boolean isEmpty(){
        return  getRowCount() <= 0;
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

    public int getMinColumnId() {
        return minColumnId;
    }

    public int getMinRowId() {
        return minRowId;
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
