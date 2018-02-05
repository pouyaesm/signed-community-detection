package Network.Optimization;

import Network.Core.*;

import static Network.Core.ListMatrix.ROW;

/**
 * Network.Optimization procedure proposed by M. Rosvall and C. T. Bergstrom
 * Paper: 2009 Fast stochastic and recursive search algorithm
 * This adds a recursive refinement procedure to the original Louvain optimization algorithm
 */
abstract public class RosvallBergstrom extends ParallelLouvain {

    /**
     * Find the best partition of the graph
     * @param graph
     * @param refineCount number of refinements after the first application of detect method
     * @return
     */
    protected int[] partition(Graph graph, int refineCount){
        Graph[] graphs = {graph};
        return partition(graphs, refineCount, 1)[0];
    }

    /**
     * Find the best partition for each graph via parallel detection
     * @param graphs
     * @param refineCount number of refinements after the first application of detect method
     * @return
     */
    protected int[][] partition(Graph[] graphs, int refineCount, int threadCount){
        ParallelLouvain detector = newDetector();
        int[][] partition = detector.detect(graphs, 1000000, threadCount);
        for(int graphId = 0 ; graphId < graphs.length ; graphId++) {
            for (int r = 0; r < refineCount; r++) {
                // Run the recursive submodule movement to improve the optimization
                int[] refinedPartition = refine(graphs[graphId], partition[graphId]);
                // Run single node movement upon refined partitions
                partition[graphId] = detector.detect(graphs[graphId], refinedPartition, 1000000);
            }
        }
        return partition;
    }

    protected int[] refine(Graph graph, int[] initialPartition){
        // Put each partition into a separate graph for isolated community detection
        Graph[] subGraphs = graph.decompose(initialPartition);
        int[] partition = new int[initialPartition.length];
        /*
            Unique sub groupId of nodes
            For example, partition 4: {0, 1, 2, 3} is further partitioned into 0: {0, 1} and 1: {2, 3}
            thus id 6 is assigned to sub group 4-0, and id 7 to sub group 4-1
         */
        int[] SubGroupId = new int[partition.length];
        // Map sub global ids to their super group id
        int[] SubGroupIdToGroupId = new int[partition.length];
        // this variables are used to convert local groupId of partitions to global unique groupIds
        int globalId, globalIdOffset;
        for(int g = 0 ; g < subGraphs.length ; g++){
            int nodeCount = subGraphs[g].getNodeCount();
            int[] subPartition;
            if(nodeCount < 4){ // trivial all in on group
                subPartition = Util.intArray(nodeCount, 0);
                continue;
            }else {
                // Initially place each node in a separate group
                subPartition = Louvain.getInitialPartition(nodeCount);

            }

        }
        return partition;
    }

    /**
     * Evaluate the quality of partition given the objective parameters
     * @param graph
     * @param partition
     * @param parameters
     * @return
     */
    public float evaluate(Graph graph, int[] partition, ObjectiveParameters parameters){
        return 0;
    }

    /**
     * Change the group id of disconnected regions inside a group to different ids
     * @param graph
     * @param partition
     */
    public static int[] postProcess(Graph graph, int[] partition){
        int[] postPartition = Util.intArray(partition.length, -1);
        int groupId = 0;
        for(int nodeId = 0 ; nodeId < postPartition.length ; nodeId++){
            if(postPartition[nodeId] != -1){
                continue; // group of n-th node is determined already
            }
            markNeighbors(nodeId, groupId, graph, partition, postPartition);
            // Assign a new group id to the next connected component of same group
            groupId++;
        }
        return postPartition;
    }

    /**
     * Recursively set the group id of the given node for its co-group neighbors,
     * changing the input postPartition
     * @param nodeId
     * @param graph
     * @param partition
     * @param postPartition this will be changed
     *
     */
    private static void markNeighbors(int nodeId, int groupId, Graph graph, int[] partition, int[] postPartition){
        postPartition[nodeId] = groupId; // assign the post processed group id
        int[] neighbors = graph.getColumns(nodeId);
        float[] linkValues = graph.getValues(nodeId);
        for(int n = 0 ; n < neighbors.length ; n++){
            int neighborId = neighbors[n];
            if(postPartition[neighborId] == -1 && linkValues[n] > 0
                    && partition[neighborId] == partition[nodeId]){
                // Go to unprocessed neighbors with positive links and same groups
                markNeighbors(neighborId, groupId, graph, partition, postPartition);
            }
        }
    }
}
