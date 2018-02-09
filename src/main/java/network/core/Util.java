package network.core;

import cern.colt.list.IntArrayList;
import cern.colt.map.OpenIntIntHashMap;

public class Util {
    /**
     * Maximum of an integer in all given arrays
     * @param arrays
     * @return
     */
    public static int max(int[]...arrays) {
        int max = Integer.MIN_VALUE;
        for(int[] array : arrays) {
            if (array == null) continue;
            for (int value : array) {
                if (value > max) {
                    max = value;
                }
            }
        }
        return max;
    }

    /**
     * Id of Maximum value
     * @param values
     * @return
     */
    public static int maxId(int[] values) {
        int max = Integer.MIN_VALUE;
        int maxId = -1;
        if (values == null) return maxId;
        for(int i = 0 ; i < values.length ; i++){
            if (values[i] > max) {
                max = values[maxId = i];
            }
        }
        return maxId;
    }

    /**
     * Minimum of an integer in all given arrays
     * @param arrays
     * @return
     */
    public static int min(int[]...arrays) {
        int min = Integer.MAX_VALUE;
        for(int[] array : arrays) {
            if (array == null) continue;
            for (int value : array) {
                if (value < min) {
                    min = value;
                }
            }
        }
        return min;
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
    public static OpenIntIntHashMap normalizeIds(int[] ids) {
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
     * Return normalized values, changes [0, 2, 4] to [0, 1, 2]
     * Changes the imported array
     * @param values
     * @return
     */
    public static int[] normalizeValues(int[] values) {
        OpenIntIntHashMap normalIds = new OpenIntIntHashMap(values.length);
        int minValue = min(values);
        for(int i = 0 ; i < values.length ; i++){
            // shift values to [1, ...) to align maps's returned 0 value to notFound
            int value = values[i] - minValue + 1;
            int normalId = normalIds.get(value);
            if(normalId > 0){
                values[i] = normalId - 1;
            }else{
                normalId = normalIds.size() + 1;
                values[i] = normalId - 1;
                normalIds.put(value, normalId);
            }
        }
        return values;
    }


    /**
     * Normalize ids into 0...L-1 without missing values,
     * assuming all id arrays are representative of the same concept,
     * thus id[a][i] = 3 points to the same concept "3" as id[b][j] = 3 in the b-th array
     *
     * @param ids
     * @return
     */
    public static OpenIntIntHashMap normalizeIds(int[]... ids) {
        int uniqueCount = Statistics.array(ids).uniqueCount;
        OpenIntIntHashMap toNormal = new OpenIntIntHashMap(uniqueCount);
        int counter = 0;
        for (int[] id : ids) {
            for (int i = 0; i < id.length; i++) {
                int rawId = id[i];
                if (rawId >= 0 && !toNormal.containsKey(rawId)) {
                    toNormal.put(rawId, counter++); // starts from 0
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
     * Sum of values
     * @param values
     * @return
     */
    public static int sum(int[] values){
        int sum = 0;
        for(float value : values){
            sum += value;
        }
        return sum;
    }

    /**
     * Sum of values
     * @param values
     * @return
     */
    public static float sum(float[] values){
        float sum = 0;
        for(float value : values){
            sum += value;
        }
        return sum;
    }

    /**
     * Sum of values
     * @param values
     * @return
     */
    public static double sum(double[] values){
        float sum = 0;
        for(double value : values){
            sum += value;
        }
        return sum;
    }

    /**
     * Replace values in [find - delta, find + delta] with the replace value
     * @param values
     * @param find
     * @param replace
     * @param delta
     * @return
     */
    public static void replace(double[] values, double find, double replace, double delta){
        for(int v = 0 ; v < values.length ; v++){
            if(values[v] >= find - delta && values[v] <= find + delta){
                values[v] = replace;
            }
        }
    }

    /**
     * Return base 2 logarithm
     * @param value
     * @return
     */
    public static double log2(double value){
        return Math.log(value)/Math.log(2);
    }

    /**
     * Return base 2 logarithm
     * @param values
     * @return
     */
    public static double[] log2(double[] values){
        double[] logs = new double[values.length];
        for(int i = 0 ; i < values.length ; i++){
            logs[i] = Math.log(values[i])/Math.log(2);
        }
        return logs;
    }

    /**
     * Initialize the array with the given value
     * @param arraySize
     * @param value
     * @return
     */
    public static float[] floatArray(int arraySize, float value){
        float[] array = new float[arraySize];
        for(int a = 0 ; a < array.length ; a++){
            array[a] = value;
        }
        return array;
    }

    /**
     * Initialize the array with the given value
     * @param arraySize
     * @param value
     * @return
     */
    public static int[] intArray(int arraySize, int value){
        int[] array = new int[arraySize];
        for(int a = 0 ; a < array.length ; a++){
            array[a] = value;
        }
        return array;
    }

    /**
     * Initialize the array with the given value
     * @param arraySize
     * @param value
     * @return
     */
    public static double[] doubleArray(int arraySize, double value){
        double[] array = new double[arraySize];
        for(int a = 0 ; a < array.length ; a++){
            array[a] = value;
        }
        return array;
    }

    /**
     * Aggregate values based on their group ids
     * @param values
     * @param group
     * @return aggregated values per group
     */
    public static double[] aggregate(double[] values, int[] group){
        double[] aggregate = new double[Util.max(group) + 1];
        for(int i = 0 ; i < values.length ; i++){
            aggregate[group[i]] += values[i];
        }
        return aggregate;
    }

    /**
     * Element wise sum of two arrays
     * @param array1
     * @param array2
     * @return
     */
    public static double[] sum(double[] array1, double[] array2){
        if(array1.length != array2.length){
            return null;
        }
        double[] values = new double[array1.length];
        for(int i = 0 ; i < array1.length ; i++){
            values[i] = array1[i] + array2[i];
        }
        return values;
    }

    /**
     * Element wise dot of two arrays
     * @param array1
     * @param array2
     * @return
     */
    public static double dot(double[] array1, double[] array2){
        if(array1.length != array2.length){
            return Double.NEGATIVE_INFINITY;
        }
        double value = 0;
        for(int i = 0 ; i < array1.length ; i++){
            value += array1[i] * array2[i];
        }
        return value;
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

    /**
     * Is number
     * @param value
     * @return
     */
    public static boolean isNumber(String value){
        try {
            Double.parseDouble(value);
            return true;
        }catch (Exception exp){
            return false;
        }
    }

    /**
     * Clone the maps object
     * @param maps
     * @return
     */
//    public static Map<Integer, Integer> clone(Map<Integer, Integer> maps){
//        OpenIntIntHashMap clone = HashIntIntMaps.newUpdatableMap(maps.size());
//        maps.forEach(clone::put);
//        return clone;
//    }

    /**
     * Returns an array of 0 to N-1 values
     * @param size
     * @return
     */
    public static int[] ramp(int size){
        int[] partition = new int[size];
        for(int p = 0 ; p < partition.length ; p++){
            partition[p] = p;
        }
        return partition;
    }

    /**
     * Convert pairs (key, value) to array[key] = value
     * @param hashMap
     * @return
     */
    public static int[] toArray(OpenIntIntHashMap hashMap){
        int maxKey = 0;
        IntArrayList keys = hashMap.keys();
        for(int i = 0 ; i < keys.size() ; i++){
            int key = keys.get(i);
            if(key > maxKey) maxKey = key;
        }
        int[] array = new int[maxKey + 1];
        for(int i = 0 ; i < keys.size() ; i++){
            int key = keys.get(i);
            array[key] = hashMap.get(key);
        }
        return array;
    }
}
