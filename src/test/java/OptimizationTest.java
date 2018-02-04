import Network.Core.Graph;
import Network.Core.GraphReader;
import Network.Core.ListMatrix;
import Network.Optimization.CPM;
import Network.Optimization.RosvallBergstrom;
import org.junit.Assert;
import org.junit.Test;

public class OptimizationTest {

    @Test
    public void testCPMOptimization() throws Exception{
        Graph graph = GraphReader.readGraph("testCases/3triads.txt", true, 12);
        CPM cpmDetector = new CPM();
        int[] partition = cpmDetector.detect(graph, 0.05f, 0.5f, 0);
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
}
