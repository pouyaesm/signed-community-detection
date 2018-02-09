import network.core.Graph;
import network.core.GraphIO;
import network.core.ListMatrix;
import network.core.SparseMatrix;
import network.optimization.CPMapParameters;
import network.signedmapequation.ParallelStationary;
import network.signedmapequation.SiMap;
import network.signedmapequation.SiMapStatistics;
import network.signedmapequation.Stationary;
import org.junit.Assert;
import org.junit.Test;

public class SignedMapEquationTest {

    /**
     * Test re-weighting process of signed networks into positive networks based on partition
     */
    @Test
    public void testReWeight() throws Exception{
        Graph graph = GraphIO.readGraph("testCases/siMap.txt", false);
        int[] partition = {0, 0, 0, 0, 1, 2, 2}; // {0, 1, 2, 3}, {4}, {5, 6}
        SiMapStatistics statistics = SiMap.reWeight(graph, partition);
        // Check transitions and neighbors of node "0" ("1" in graph file)
        int[] expectedNeighbors = {1, 3, 4};
        float[] expectedTransitions = {0.1875f, 0.1875f, 0.25f};
        Assert.assertArrayEquals(expectedNeighbors, statistics.transition.getColumns(0));
        Assert.assertArrayEquals(expectedTransitions, statistics.transition.getValues(0), 0);

        // InfoMap positive graph reWeight test (graph must remain unchanged)
        Graph infoMapToyGraph = GraphIO.readGraph("testCases/infoMap.txt", true);
        int[] toyPartition = {0, 0, 0, 0, 0, 0, 1, 1, 1, 1, 1, 1, 1, 2, 2, 2, 2, 2, 2, 2, 2, 3, 3, 3, 3};
        SiMapStatistics toyStatistics = SiMap.reWeight(infoMapToyGraph, toyPartition);
        // Inspect node 2 on the edge of group 0 and 1
        Assert.assertArrayEquals(new int[]{0, 1, 3, 7}
                , toyStatistics.transition.getColumns(2));
        Assert.assertArrayEquals(new float[]{0.25f, 0.25f, 0.25f, 0.25f}
        , toyStatistics.transition.getValues(2), 0f);
        Assert.assertEquals(0f, toyStatistics.negativeTeleport[2], 0f);
        Assert.assertEquals(4f, toyStatistics.inWeight[2], 0.0001f);
        Assert.assertEquals(4f, toyStatistics.outWeight[2], 0.0001f);
    }

    @Test
    public void testStationaryDistribution(){
        int[] rows = {          1,      1,      2,      2,      3,      3};
        int[] columns = {       2,      3,      1,      3,      1,      2};
        float[] transitions = { 0.25f,  0.75f,   0.25f,  0.75f,  0.25f,  0.75f};
        SiMapStatistics statistics = new SiMapStatistics();
        statistics.transition = new Graph(
                new ListMatrix().init(rows, columns, transitions, true).normalize());
        statistics.negativeTeleport = new double[]{0, 0, 0};
        statistics.teleport = new double[]{0.3333, 0.3333, 0.3333};
        int[] partition = {0,   1,  1};
        float tau = 0.1f;
        statistics = new Stationary(2).visitProbabilities(statistics, partition, tau);
        Assert.assertArrayEquals(new double[]{0.2108, 0.3661, 0.4228}
        , statistics.nodeRecorded, 0.0001f);
        Assert.assertArrayEquals(new double[]{0.1972, 0.3698, 0.4327}
                , statistics.nodeUnRecorded, 0.0001f);
        Assert.assertArrayEquals(new double[]{0.2038, 0.2038}
                , statistics.groupRecorded, 0.0001f);
        Assert.assertArrayEquals(new double[]{0.2108, 0.1972}
                , statistics.groupUnRecorded, 0.0001f);
    }

    @Test
    public void testSiMapEvaluationOnPaperGraph() throws Exception{
        Graph graph = GraphIO.readGraph("testCases/infoMap.txt", true);
        int[] bestPartition = {0, 0, 0, 0, 0, 0, 1, 1, 1, 1, 1, 1, 1, 2, 2, 2, 2, 2, 2, 2, 2, 3, 3, 3, 3};
        // Link Recorded (best recommended setting)
        CPMapParameters parameters = new CPMapParameters(
                0.15f, false, false, 1);
        double descriptionLength = SiMap.evaluate(graph, bestPartition, parameters);
        Assert.assertEquals(3.2469, descriptionLength, 0.0001);
        // Node UnRecorded (naive-worst setting)
        parameters.USE_RECORDED = true;
        parameters.TELEPORT_TO_NODE = true;
        descriptionLength = SiMap.evaluate(graph, bestPartition, parameters);
        Assert.assertEquals(3.7307, descriptionLength, 0.0001f);
    }

    @Test
    public void testSiMapEvaluationOnSigned() {
        int[] rows = {      0,  0,  1,  3,  3,  3,  3,  3,  3,  4,  5};
        int[] columns = {   1,  2,  2,  0,  1,  2,  4,  5,  6,  6,  6};
        float[] values = {  1,  1,  1, -1,  1,  1,  1,  1, -1,  1,  1};
        // {0, 1, 2}, {3, 4, 5, 6}, the critical node is '3' bridging two partitions
        int[] partition = { 0, 0, 0, 2, 2, 2, 2};
        ListMatrix listMatrix = new ListMatrix().init(rows, columns, values, true).symmetrize();
        Graph graph = new Graph(listMatrix);
        CPMapParameters parameters = new CPMapParameters(
                0.1f, false, false, 1);
        double minimumDescriptionLength = SiMap.evaluate(graph, partition, parameters);
        Assert.assertEquals(2.8238, minimumDescriptionLength, 0.0001);
    }

    @Test
    public void testParallelStationaryCalculation(){
        int[] rows = {          0,   0,   0,   1,   1,   2};
        int[] columns = {       0,   1,   2,   0,   1,   2};
        float[] transit = {  .25f, .5f,.25f, .5f, .5f,   1};
        double[] dist = { .3333, .3333, .3333}; // initial distribution for multiplication
        SparseMatrix transitionMatrix = new SparseMatrix(
                new ListMatrix().init(rows, columns, transit, true));
        ParallelStationary parallel = new ParallelStationary(2);
        double[] nextDist = parallel.multiply(transitionMatrix, dist);
        double[] expectedDist = {0.25, .3333, .4166};
        Assert.assertArrayEquals(expectedDist, nextDist, 0.0001);
    }
}
