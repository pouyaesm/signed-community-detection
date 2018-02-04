package Network.Core;

import org.junit.Assert;

public class MyAssert extends Assert{
    public static void assertArrayEquals(float[][] expecteds, float[][] actuals, float delta) {
        assertArrayEquals("", expecteds, actuals, delta);
    }

    public static void assertArrayEquals(String message, float[][] expecteds, float[][] actuals, float delta) {
        assertEquals(message, expecteds.length, actuals.length);
        for(int i = 0 ; i < expecteds.length ; i++){
            assertArrayEquals(message, expecteds[i], actuals[i], delta);
        }
    }
}
