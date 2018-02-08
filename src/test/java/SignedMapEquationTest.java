import network.core.Graph;
import network.core.GraphIO;
import network.core.ListMatrix;
import network.optimization.CPMapParameters;
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
        int[] expectedNeighbors = {1, 2, 3};
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
        statistics.transition = (Graph) new Graph().init(
                new ListMatrix().init(rows, columns, transitions, true).normalize());
        statistics.negativeTeleport = new double[]{0, 0, 0};
        statistics.teleport = new double[]{0.3333, 0.3333, 0.3333};
        int[] partition = {0,   1,  1};
        float tau = 0.1f;
        statistics = Stationary.visitProbabilities(statistics, partition, tau);
        Assert.assertArrayEquals(new double[]{0.3333, 0.3333, 0.3333}
        , statistics.nodeRecorded, 0.0001f);
        Assert.assertArrayEquals(new double[]{0.1666, 0.3333, 0.5000}
                , statistics.nodeUnRecorded, 0.0001f);
        Assert.assertArrayEquals(new double[]{0.3222, 0.1722}
                , statistics.groupRecorded, 0.0001f);
        Assert.assertArrayEquals(new double[]{0.3333, 0.1666}
                , statistics.groupUnRecorded, 0.0001f);
    }

    @Test
    public void testEvaluation() throws Exception{
        Graph graph = GraphIO.readGraph("testCases/infoMap.txt", true);
        int[] bestPartition = {0, 0, 0, 0, 0, 0, 1, 1, 1, 1, 1, 1, 1, 2, 2, 2, 2, 2, 2, 2, 2, 3, 3, 3, 3};
        CPMapParameters parameters = new CPMapParameters();

        // Link Recorded (best recommended setting)
        parameters.USE_RECORDED = false;
        parameters.TELEPORT_TO_NODE = false;
        parameters.TAU = 0.15f;
        double descriptionLength = SiMap.evaluate(graph, bestPartition, parameters);
        Assert.assertEquals(2.9934, descriptionLength, 0.0001f);
        // Node UnRecorded (naive-worst setting)
        parameters.USE_RECORDED = true;
        parameters.TELEPORT_TO_NODE = true;
        parameters.TAU = 0.00001f; // in recorded setting, this value must be as small as possible
        descriptionLength = SiMap.evaluate(graph, bestPartition, parameters);
        Assert.assertEquals(3.246, descriptionLength, 0.0001f);
    }
}
