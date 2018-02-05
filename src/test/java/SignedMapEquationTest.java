import Network.Core.Graph;
import Network.Core.GraphIO;
import Network.Optimization.CPMapParameters;
import Network.SignedMapEquation.SiMapStatistics;
import Network.SignedMapEquation.SiMap;
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
        SiMapStatistics statistics = SiMap.reweight(graph, partition);
        // Check transitions and neighbors of node "0" ("1" in graph file)
        int[] expectedNeighbors = {1, 3, 4};
        float[] expectedTransitions = {0.1875f, 0.1875f, 0.25f};
        Assert.assertArrayEquals(expectedNeighbors, statistics.transition.getColumns(0));
        Assert.assertArrayEquals(expectedTransitions, statistics.transition.getValues(0), 0);
    }

    @Test
    public void testEvaluation() throws Exception{
        Graph graph = GraphIO.readGraph("testCases/infoMap.txt", true);
        int[] bestPartition = {0, 0, 0, 0, 0, 0, 1, 1, 1, 1, 1, 1, 1, 2, 2, 2, 2, 2, 2, 2, 2, 3, 3, 3, 3};
        CPMapParameters parameters = new CPMapParameters();
        parameters.USE_RECORDED = false;
        parameters.TELEPORT_TO_NODE = false;
        parameters.TAU = 0.01f;
        double descriptionLength = SiMap.evaluate(graph, bestPartition, parameters);
        Assert.assertEquals(3.0616, descriptionLength, 0.0001f);
    }
}
