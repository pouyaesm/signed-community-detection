package Core;

public class Util {
    /**
     * Maximum of an integer in all given arrays
     * @param arrays
     * @return
     */
    public synchronized static int max(int[]...arrays) {
        int max = Integer.MIN_VALUE;
        for(int[] array : arrays)
        for (int value : array) {
            if (value > max) {
                max = value;
            }
        }
        return max;
    }

    /**
     * Minimum of an integer in all given arrays
     * @param arrays
     * @return
     */
    public synchronized static int min(int[]...arrays) {
        int min = Integer.MAX_VALUE;
        for(int[] array : arrays)
            for (int value : array) {
                if (value < min) {
                    min = value;
                }
            }
        return min;
    }

    /**
     * Number of unique values in all arrays
     * @param values
     * @return
     */
    public synchronized static int uniqueCount(int[]...values){
        int minValue = Util.min(values);
        int maxValue = Util.max(values);
        boolean[] visitedValues = new boolean[maxValue - minValue + 1];
        int uniqueCount = 0;
        for(int a = 0 ; a < values.length ; a++) {
            for (int i = 0; i < values[a].length; i++) {
                int adjustedValue = values[a][i] - minValue;
                if (!visitedValues[adjustedValue]) {
                    uniqueCount++;
                    visitedValues[adjustedValue] = true;
                }
            }
        }
        return uniqueCount;
    }

    public synchronized static int uniqueCount(int[] values){
        int[][] arrays = new int[1][];
        arrays[0] = values;
        return uniqueCount(arrays);
    }

    /**
     * Return number of zero values
     *
     * @param arrays
     * @return
     */
    public synchronized static int count(int valueToCount, int[]...arrays) {
        int count = 0;
        for(int[] array : arrays) {
            for (int value : array) {
                if (valueToCount == value) {
                    count++;
                }
            }
        }
        return count;
    }

    public synchronized static int count(int valueToCount, int[] values) {
        int[][] arrays = new int[1][];
        arrays[0] = values;
        return count(valueToCount, arrays);
    }

    /**
     * Return normalizedIds[ids[i]] e [0...L-1]
     * @param ids
     * @return
     */
    public synchronized static int[] normalizeIds(int[] ids) {
        int[][] idsArray = new int[1][ids.length];
        idsArray[0] = ids;
        return normalizeIds(idsArray);
    }

    /**
     * Check if arrays contain the value
     * @param value value to search
     * @param arrays arrays to search among them
     * @return
     */
    public synchronized static boolean contains(int value, int[]...arrays) {
        int count = 0;
        for(int[] array : arrays) {
            for (int v : array) {
                if (value == v) {
                    return true;
                }
            }
        }
        return false;
    }

    public synchronized static boolean contains(int value, int[] values) {
        int[][] arrays = new int[1][];
        arrays[0] = values;
        return contains(value, arrays);
    }

    /**
     * Normalize ids into 0...L-1 without missing values,
     * assuming all id arrays are representative of the same concept,
     * thus id[a][i] = 3 points to the same concept "3" as id[b][j] = 3 in the b-th array
     *
     * @param ids
     * @return
     */
    public synchronized static int[] normalizeIds(int[]... ids) {
        int maxRawId = Integer.MIN_VALUE;
        // maximum raw (un-normalized) id found among all the arrays
        for (int a = 0; a < ids.length; a++) {
            maxRawId = Math.max(Util.max(ids[a]), maxRawId);
        }
        int[] toNormal = new int[maxRawId + 1];
        // initialize normal ids to -1 so as to distinguish them from the first normal index "0"
        for(int rawIndex = 0 ; rawIndex < toNormal.length ; rawIndex++){
            toNormal[rawIndex] = -1;
        }
        int counter = 0;
        for (int a = 0; a < ids.length; a++) {
            for (int i = 0; i < ids[a].length; i++) {
                int rawId = ids[a][i];
                if (toNormal[rawId] == -1) {
                    toNormal[rawId] = counter++; // starts from 0
                }
            }
        }
        return toNormal;
    }
}
