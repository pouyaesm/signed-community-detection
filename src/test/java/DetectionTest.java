import network.core.Graph;
import network.core.GraphIO;
import network.core.ListMatrix;
import network.core.SiGraph;
import network.optimization.CPM;
import network.optimization.CPMParameters;
import org.junit.Assert;
import org.junit.Test;

public class DetectionTest {

    @Test
    public void testCPMDetection() throws Exception{
        SiGraph graph = new SiGraph(GraphIO.readGraph("testCases/3triads.txt", true));
        CPM cpmDetector = new CPM().setParams(new CPMParameters().setResolution(0.05f));
        int[] partition = cpmDetector.detect(graph);
        int[] expectedPartition = {0, 0, 0, 1, 1, 1, 2, 2, 2};
        Assert.assertArrayEquals(expectedPartition, partition);
    }

    @Test
    public void testCPMEvaluation() {
        int[] rows = {      0, 0, 1, 3, 3, 3, 3, 3, 4};
        int[] columns = {   1, 2, 2, 0, 1, 2, 4, 5, 5};
        float[] values = {  1, 1, 1, -1, 1, 1, 1, -1, 1};
        int[] partition = { 0, 0, 0, 2, 2, 2}; // {0, 1, 2}, {3, 4, 5}
        ListMatrix listMatrix = new ListMatrix().init(rows, columns, values, true).symmetrize();
        Graph graph = new Graph(listMatrix);
        CPMParameters parameters = new CPMParameters().setResolution(0.005);
        double hamiltonian = new CPM().evaluate(graph, partition, parameters);
        Assert.assertEquals(-3.955, hamiltonian, 0);
    }

    /**
     * Run the parallel detection one the same graph
     * @throws Exception
     */
    @Test
    public void testParallelLouvain() throws Exception {
        SiGraph[] graphs = new SiGraph[2];
        graphs[0] = new SiGraph(GraphIO.readGraph("testCases/3triads.txt", true));
        graphs[1] = graphs[0].clone();
        int[] expectedPartition = {0, 0, 0, 1, 1, 1, 2, 2, 2};
        CPM cpmDetector = (CPM) new CPM()
                .setParams(new CPMParameters().setResolution(0.05f))
                .setThreadCount(2);
        int[][] partitions = cpmDetector.detect(graphs);
        Assert.assertArrayEquals(expectedPartition, partitions[0]);
        Assert.assertArrayEquals(expectedPartition, partitions[1]);
    }
}
