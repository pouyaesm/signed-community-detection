package network.core;

import org.junit.Assert;
import org.junit.Test;

public class ConnectedComponentsTest {

    @Test
    public void testConnectedComponentsTest(){
        // a triangle {1, 2, 3}, a pair {4, 5}, and a single node {7}
        int[] rows = {      1, 1, 2, 4, 6};
        int[] columns = {   2, 3, 3, 5, 6};
        float[] values = {  1, 1, 1, 1, 1};
        Graph graph = new Graph(new ListMatrix().init(rows, columns, values, true)
                .symmetrize().normalize());
        ConnectedComponents connectedComponents = new ConnectedComponents(graph).execute();
        int[] components = connectedComponents.getComponents();
        int[] expected = {0, 0, 0, 1, 1, 2};
        Assert.assertArrayEquals(expected, components);
        Assert.assertArrayEquals(new int[]{1, 1, 1, 0, 0, 0},
                connectedComponents.getLargestComponent());
        // Test connected co groups
        int[] partition = {0, 0, 1, 2, 2, 2}; // 3 goes out of the triad, 2 comes inside the pair
        int[] expectedPartition = {0, 0, 1, 2, 2, 3};
        int[] newPartition = new ConnectedCoGroups(graph, partition).execute().getComponents();
        Assert.assertArrayEquals(expectedPartition, newPartition);
    }
}
