package Core;

public class Util {
    /**
     * Maximum of an integer in all given arrays
     * @param arrays
     * @return
     */
    public static int max(int[]...arrays) {
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
    public static int min(int[]...arrays) {
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
    public static int uniqueCount(int discardedValue, int valueOffset, int size, int[]...values){
        boolean[] visitedValues = new boolean[size];
        int uniqueCount = 0;
        for(int a = 0 ; a < values.length ; a++) {
            for (int i = 0; i < values[a].length; i++) {
                int adjustedValue = values[a][i] + valueOffset;
                if (!visitedValues[adjustedValue] && adjustedValue != discardedValue) {
                    uniqueCount++;
                    visitedValues[adjustedValue] = true;
                }
            }
        }
        return uniqueCount;
    }

    public static int uniqueCount(int[]...values) {
        int minValue = Util.min(values);
        int maxValue = Util.max(values);
        return uniqueCount(Integer.MIN_VALUE,  - minValue, maxValue - minValue + 1, values);
    }

    public static int uniqueCount(int[] values){
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
    public static int count(int valueToCount, int[]...arrays) {
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

    public static int count(int valueToCount, int[] values) {
        int[][] arrays = new int[1][];
        arrays[0] = values;
        return count(valueToCount, arrays);
    }

    /**
     * Return normalizedIds[ids[i]] e [0...L-1]
     * @param ids
     * @return
     */
    public static int[] normalizeIds(int[] ids) {
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
    public static boolean contains(int value, int[]...arrays) {
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

    public static boolean contains(int value, int[] values) {
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
    public static int[] normalizeIds(int[]... ids) {
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
                if (rawId >= 0 && toNormal[rawId] == -1) {
                    toNormal[rawId] = counter++; // starts from 0
                }
            }
        }
        return toNormal;
    }

    /**
     * Return a permutation of 0...size
     * It is biased (good enough) but fast
     * @param size
     */
    public static int[] permute(int size){
        int[] permutation = new int[size];
        for(int p = 0; p < permutation.length ; p++){
            permutation[p] = p;
        }
        // Swap index "p" with some position before (inclusive)
        for(int select, p = 1 ; p < permutation.length ; p++){
            select = (int) (Math.random() * (p + 1));
            permutation[p] = permutation[select];
            permutation[select] = p;
        }
        return permutation;
    }

    /**
     * Return full matrix of three triads mutually connected to each other via a negative link
     * forming a triad of triads
     */
    public static float[][] getThreeTriads(){
        float[][] matrix = {
                {0, 1,  1,  0,  0,  0,  0,  0,  0,  0,  0,  0}, // 0
                {1, 0,  1,  -1, 0,  0,  0,  0,  0,  0,  0,  0}, // 1
                {1, 1,  0,  0,  0,  0,  -1, 0,  0,  0,  0,  0}, // 2
                {0, -1, 0,  0,  1,  1,  0,  0,  0,  0,  0,  0}, // 3
                {0, 0,  0,  1,  1,  1,  0,  0,  0,  0,  0,  0}, // 4
                {0, 0,  0,  1,  1,  0,  0,  -1, 0,  0,  0,  0}, // 5
                {0, 0,  -1, 0,  0,  0,  0,  1,  1,  0,  0,  0}, // 6
                {0, 0,  -1, 0,  0,  0,  0,  1,  1,  0,  0,  0}, // 7
        };
        return matrix;
    }
}
