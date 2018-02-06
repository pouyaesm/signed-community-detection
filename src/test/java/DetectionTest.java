import Network.Core.Graph;
import Network.Core.GraphIO;
import Network.Core.ListMatrix;
import Network.Optimization.CPM;
import Network.Optimization.RosvallBergstrom;
import org.junit.Assert;
import org.junit.Test;

public class DetectionTest {

    @Test
    public void testCPMDetection() throws Exception{
        Graph graph = GraphIO.readGraph("testCases/3triads.txt", true);
        CPM cpmDetector = new CPM();
        int[] partition = cpmDetector.detect(graph, 0.05f, 0.5f, 5);
        int[] expectedPartition = {0, 0, 0, 1, 1, 1, 2, 2, 2};
        Assert.assertArrayEquals(expectedPartition, partition);
    }

    /**
     * This post-process re-assigns disconnected components with the same groupId into separate groups
     */
    @Test
    public void testPartitionPostProcess(){
        float[][] matrix = {
            {0, 1,  0,  0},
            {1, 0,  0,  0},
            {0, 0,  0,  1},
            {0, 0,  1,  0}};
        int[] partition = {0, 0, 0, 0}; // {1, 2}, {3, 4} disconnected components have the same groupId
        Graph graph = new Graph(new ListMatrix().init(matrix, true));
        int[] postPartition = RosvallBergstrom.postProcess(graph, partition);
        // {1, 2}, {3, 4} are supposed to be assigned to different groupIds
        Assert.assertArrayEquals(new int[]{0, 0, 1, 1}, postPartition);
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
}
