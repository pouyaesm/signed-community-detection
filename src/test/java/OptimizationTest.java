import Core.Graph;
import Core.GraphReader;
import Optimization.CPM;
import org.junit.Assert;
import org.junit.Test;

public class OptimizationTest {

    @Test
    public void testCPMOptimization() throws Exception{
        Graph graph = GraphReader.readGraph("example/triadGraph.txt", 24);
        CPM cpmDetector = new CPM();
        int[] partition = cpmDetector.detect(graph, 0.05f, 0.5f, 0);
        int[] expectedPartition = {0, 0, 0, 1, 1, 1, 2, 2, 2};
        Assert.assertArrayEquals(expectedPartition, partition);
    }
}
