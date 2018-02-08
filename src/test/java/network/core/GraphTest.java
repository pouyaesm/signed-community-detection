package network.core;

import org.junit.Assert;
import org.junit.Test;

public class GraphTest {

    @Test
    public void testTransitionProbability() throws Exception{
        Graph graph = GraphIO.readGraph("testCases/triad.txt", true);
        Graph transition = graph.getTransitionProbability();
        float[][] expectedWeights = {{0.25f, 0.75f}, {0.5f, 0.5f}, {0.75f, 0.25f}};
        MyAssert.assertArrayEquals(expectedWeights, transition.getSparseValues());
    }

    @Test
    public void testMultiGraphDecompose(){
        int[] rows = {      1, 1, 3};
        int[] columns = {   2, 3, 4};
        float[] values = {  1, -1, -1};
        ListMatrix listMatrix = new ListMatrix().init(rows, columns, values, true);
        Graph positive = new Graph(listMatrix.filter(0, Integer.MAX_VALUE));
        Graph negative = new Graph(listMatrix.filter(Integer.MIN_VALUE, 0));
        int POS = 0, NEG = 1;
        MultiGraph multiGraph = new MultiGraph()
                .addGraph(POS, positive).addGraph(NEG, negative);
        int[] partition = {-1, 0, 0, 1, 1}; //id 0 not exists, {1, 2}, {3, 4}
        MultiGraph[] decomposed = multiGraph.decompose(partition);
        // Check raw id of positive type (0) of partition '0'
        Assert.assertArrayEquals(new int[]{1, 2}, decomposed[0].getGraph(POS).getToRaw()[0]);
        // Check raw id of negative type (1) of second partition '1'
        Assert.assertArrayEquals(new int[]{1, 2, 3, 4}, decomposed[1].getGraph(NEG).getToRaw()[0]);
        // MultiGraph to raw must match its most complete type-graph
        Assert.assertArrayEquals(new int[]{1, 2, 3, 4}, decomposed[1].getToRaw()[0]);
    }
}
