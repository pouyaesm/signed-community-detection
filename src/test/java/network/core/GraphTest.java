package network.core;

import org.junit.Assert;
import org.junit.Test;

import static network.core.SiGraph.NEGATIVE;
import static network.core.SiGraph.POSITIVE;

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
        int[] rows = {      1, 2, 4};
        int[] columns = {   2, 3, 4};
        float[] values = {  1, -1, -1};
        ListMatrix listMatrix = new ListMatrix().init(rows, columns, values, true);
        Graph positive = new Graph(listMatrix.filter(0, Integer.MAX_VALUE));
        Graph negative = new Graph(listMatrix.filter(Integer.MIN_VALUE, 0));
        int POS = 0, NEG = 1;
        MultiGraph multiGraph = new MultiGraph()
                .addGraph(POS, positive).addGraph(NEG, negative);
        int[] partition = {-1, 0, 0, 0, 1}; //id 0 not exists, {1, 2, 3}, {4}
        MultiGraph[] decomposed = multiGraph.decompose(partition);
        // Check raw id of positive type (0) of partition '0'
        Assert.assertArrayEquals(new int[]{1, 2}, decomposed[0].getGraph(POS).getToRaw()[0]);
        Assert.assertArrayEquals(new int[]{1, 2, 3}, decomposed[0].getGraph(NEG).getToRaw()[0]);
        // MultiGraph to raw must match its most complete type-graph
        Assert.assertArrayEquals(new int[]{1, 2, 3}, decomposed[0].getToRaw()[0]);
        // Check raw id of negative type (1) of second partition '1'
        Assert.assertArrayEquals(new int[]{4}, decomposed[1].getGraph(NEG).getToRaw()[0]);
    }

    @Test
    public void testMultiGraphFold(){
        int[] rows = {      1, 2, 3, 3};
        int[] columns = {   2, 3, 3, 4};
        float[] values = { -1,-1,-1, 1};
        ListMatrix listMatrix = new ListMatrix().init(rows, columns, values, true);
        Graph positive = new Graph(listMatrix.filter(0, Integer.MAX_VALUE));
        Graph negative = new Graph(listMatrix.filter(Integer.MIN_VALUE, 0));
        int POS = 0, NEG = 1;
        MultiGraph multiGraph = new MultiGraph()
                .addGraph(POS, positive).addGraph(NEG, negative);
        int[] partition = {-1, 5, 6, 6, 7}; //id 0 not exists, 5:{1}, 6:{2, 3}, 7:{4}
        MultiGraph foldedGraph = multiGraph.fold(partition);
        // Expected the folded graph to be w(0, 1) = -1, w(1, 1) = -2, w(1, 2) = 1
        // where 5 <-> 0, 6 <-> 1, 7 <-> 2
        // Check raw id of positive type (0) of partition '0'
        Assert.assertArrayEquals(new int[]{5, 6, 7}, foldedGraph.getToRaw()[0]);
        Assert.assertArrayEquals(new int[]{1, 0}, foldedGraph.getGraph(NEG).getRows());
        Assert.assertArrayEquals(new int[]{1, 1}, foldedGraph.getGraph(NEG).getColumns());
        Assert.assertArrayEquals(new float[]{-2f, -1f}, foldedGraph.getGraph(NEG).getValues(), 0.01f);
        Assert.assertArrayEquals(new int[]{1}, foldedGraph.getGraph(POS).getRows());
        Assert.assertArrayEquals(new int[]{2}, foldedGraph.getGraph(POS).getColumns());
        Assert.assertArrayEquals(new float[]{1f}, foldedGraph.getGraph(POS).getValues(), 0.01f);
    }

    @Test
    public void testMultiGraphNormalizeKeepRawIds(){
        int[] rows = {      5,  5};
        int[] columns = {   6,  7};
        float[] values = {  1, -1};
        SiGraph graph = new SiGraph(new Graph(
                new ListMatrix().init(rows, columns, values, true))
        );
        graph.normalizeKeepRawIds();
        Graph positive = graph.getGraph(POSITIVE);
        Graph negative = graph.getGraph(NEGATIVE);
        // (5, 6) and (5, 7) links are expected to be normalized to (0, 1) and (0, 2)
        Assert.assertArrayEquals(new int[]{1}, positive.getColumns());
        Assert.assertArrayEquals(new int[]{5, 6}, positive.getToRaw()[0]);
        Assert.assertArrayEquals(new int[]{2}, negative.getColumns());
        Assert.assertArrayEquals(new int[]{5, 6, 7}, negative.getToRaw()[0]);
        Assert.assertEquals("Max node id is expected to be normalized too",
                2, graph.getNodeMaxId());
        Assert.assertEquals(3, graph.getNodeCount());
    }
}
