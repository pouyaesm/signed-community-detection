package network.core;
import cern.colt.map.OpenIntIntHashMap;
import network.utils.QuickSort;
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

        QuickSort quickSort = new QuickSort(values);
        // sort increasing (swap when smaller index is larger), skip the first element to test partial sorting
        quickSort.sort(1, values.length - 1, false);
        int[] sortedValues = quickSort.permute(values.clone()); // apply permutation on a copy of values
        quickSort.permute(auxiliaryValues); // apply permutation on auxiliaryValues

        Assert.assertArrayEquals(expectedIndices, quickSort.getIndices());
        Assert.assertArrayEquals("values must not change by sort", expectedValues, values);
        Assert.assertArrayEquals(expectedSortedValues, sortedValues);
        Assert.assertArrayEquals("auxiliary values must change by permute",
                expectedAuxiliaryValues, auxiliaryValues, 0.00001f);

        // Repeated values: 1, 1, 3, 3, 4, 5, 5, 6
        int[] repeated = {3, 4, 3, 1, 1, 5, 6, 5};
        QuickSort moreQuickSort = new QuickSort(repeated);
        repeated = moreQuickSort.sort(false).permute(repeated);
        Assert.assertArrayEquals(new int[]{1, 1, 3, 3, 4, 5, 5, 6}, repeated);
    }

    @Test
    public void testNormalizeIds(){
        int[] ids1 = {0, 3, 5, 3, 3, 1, 1, 0};
        int[] ids2 = {1, 3, 3, 6};
        OpenIntIntHashMap normalizedIds = Util.normalizeIds(ids1, ids2);
        // expected: 0 -> 0, 3 -> 1, 5 -> 2, 1 -> 3, 6 -> 4
        Assert.assertEquals(0, normalizedIds.get(0));
        Assert.assertEquals(1, normalizedIds.get(3));
        Assert.assertEquals(2, normalizedIds.get(5));
        Assert.assertEquals(3, normalizedIds.get(1));
        Assert.assertEquals(4, normalizedIds.get(6));
        Assert.assertEquals(0, normalizedIds.get(7));
    }

    @Test
    public void testNormalizeValues(){
        int[] values = {3, 5, 3, 3, 1, 1, -20};
        int[] expected = {0, 1, 0, 0, 2, 2, 3};
        Util.normalizeValues(values);
        Assert.assertArrayEquals(expected, values);
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
                50, Statistics.array(permute).uniqueCount);
    }

    @Test
    public void testArrayArithmetic(){
        float[] values = {-1, 0, 2, 10};
        Assert.assertEquals(11, Util.sum(values), 0f);
    }
}
