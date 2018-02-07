import network.core.Graph;
import network.core.GraphIO;
import network.optimization.CPM;
import org.junit.Assert;
import org.junit.Test;

public class DetectionTest {

    @Test
    public void testCPMDetection() throws Exception{
        Graph graph = GraphIO.readGraph("testCases/3triads.txt", true);
        CPM cpmDetector = new CPM();
        int[] partition = cpmDetector.detect(graph, 0.05f, 0.5f, 0);
        int[] expectedPartition = {0, 0, 0, 1, 1, 1, 2, 2, 2};
        Assert.assertArrayEquals(expectedPartition, partition);
    }

    /**
     * Run the parallel detection one the same graph
     * @throws Exception
     */
    @Test
    public void testParallelLouvain() throws Exception {
        Graph[] graphs = new Graph[2];
        graphs[0] = GraphIO.readGraph("testCases/3triads.txt", true);
        graphs[1] = graphs[0].clone();
        int[] expectedPartition = {0, 0, 0, 1, 1, 1, 2, 2, 2};
        CPM cpmDetector = (CPM) new CPM().setThreadCount(2);
        int[][] partitions = cpmDetector.detect(graphs, 0.05f, 0.5f, 0);
        Assert.assertArrayEquals(expectedPartition, partitions[0]);
        Assert.assertArrayEquals(expectedPartition, partitions[1]);
    }

    @Test
    public void testCPMEvaulation() throws Exception {
        Graph graph = GraphIO.readGraph("testCases/squareConflict.txt", true);
        int[] partition = {0, 0, 1, 1};

    }
}
