package Network.Core;

import org.junit.Assert;
import org.junit.jupiter.api.Test;

public class UtilTest {

    @Test
    public void testQuickSort() {
        int[] values = {100, 4, 3, 5, 1};
        float[] auxiliaryValues = {100.0f, 1.0f, 2.0f, 3.0f, 4.0f};
        int[] expectedSortedValues = {100, 1, 3, 4, 5};
        int[] expectedIndices = {0, 4, 2, 1, 3};
        int[] expectedValues = {100, 4, 3, 5, 1};
        float[] expectedAuxiliaryValues = {100.0f, 4.0f, 2.0f, 1.0f, 3.0f};

        QuickSort quickSort = new QuickSort();
        // sort increasing (swap when smaller index is larger), skip the first element to test partial sorting
        quickSort.sort(values, 1, values.length - 1, false, true);
        int[] sortedValues = quickSort.permute(values, true); // apply permutation on a copy of values
        quickSort.permute(auxiliaryValues, false); // apply permutation on auxiliaryValues
//        for(int index : quickSort.getIndices()){
//            System.out.println(index);
//        }
//        System.out.println();
//        for(int value : quickSort.getValues()){
//            System.out.println(value);
//        }
//        System.out.println();
//        for(int value : values){
//            System.out.println(value);
//        }
        Assert.assertArrayEquals(expectedIndices, quickSort.getIndices());
        Assert.assertArrayEquals("values must not change by sort", expectedValues, values);
        Assert.assertArrayEquals(expectedSortedValues, sortedValues);
        Assert.assertArrayEquals("auxiliary values must change by permute",
                expectedAuxiliaryValues, auxiliaryValues, 0.00001f);
    }

    @Test
    public void testNormalizeIds(){
        int[] ids1 = {3, 5, 3, 3, 1, 1};
        int[] ids2 = {1, 3, 3, 6};
        int[] expected = {-1, 2, -1, 0, -1, 1, 3};//3 -> 0, 5 -> 1, 1 -> 2, 6 -> 3
        int[] normalizedIds = Util.normalizeIds(ids1, ids2);
        Assert.assertArrayEquals(expected, normalizedIds);
    }

    @Test
    public void testUniqueCount(){
        int[] values = {-1, 1, 2, -1, -1, 3, -1};
        int uniqueCount = Util.uniqueCount(values);
        Assert.assertEquals(4, uniqueCount);
    }


    @Test
    public void testValueCount(){
        int[] values = {0, 1, 2, 0, 0, 3, 0};
        int countZero = Util.count(0, values);
        int countTwo = Util.count(2, values);
        Assert.assertEquals(4, countZero);
        Assert.assertEquals(1, countTwo);
    }

    @Test
    public void testPermute(){
        // Each index 0...size-1 must be present
        int[] permute = Util.permute(50);
        Assert.assertEquals("Each index must be present one and only once",
                50, Util.uniqueCount(permute));
    }

    /**
     * Minimum maximum test
     */
    public void testMinMax(){

    }
}
