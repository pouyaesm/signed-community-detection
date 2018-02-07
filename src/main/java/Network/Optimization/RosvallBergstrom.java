package Network.Optimization;

import Network.Core.*;
import cern.colt.map.OpenIntIntHashMap;

import java.util.ArrayList;

/**
 * Network.Optimization procedure originally proposed by M. Rosvall and C. T. Bergstrom
 * Paper: 2009 Fast stochastic and recursive search algorithm
 * This adds a recursive refinement procedure to the original Louvain optimization algorithm
 */
abstract public class RosvallBergstrom extends ParallelLouvain {

    /**
     * Number of links for a sub-graph to be considered large
     * Graphs with edge groupCount more than this are processed in batch async
     */
    public final static int LARGE_SUB_GRAPH = 100000;

    /**
     * Find the best partition of the graph
     * @param graph
     * @param refineCount number of refinements after the first application of detect method
     * @return
     */
    protected int[] partition(Graph graph, int refineCount){
        Graph[] graphs = {graph};
        return partition(graphs, refineCount)[0];
    }

    /**
     * Find the best partition for each graph via parallel detection
     * @param graphs
     * @param refineCount number of refinements after the first application of detect method
     * @return
     */
    protected int[][] partition(Graph[] graphs, int refineCount){
        ParallelLouvain detector = newDetector();
        System.out.println("Louvain algorithm started");
        int[][] partition = detector.detect(graphs, 1000000);
        for(int graphId = 0 ; graphId < graphs.length ; graphId++) {
            for (int r = 0; r < refineCount; r++) {
                System.out.println("Refinement No. " + (r + 1) + " for graph size " + graphs[graphId].getNodeCount());
                // Run the recursive submodule movement to improve the optimization
                int[] refinedPartition = refine(graphs[graphId], partition[graphId]);
                // Run single node movement upon refined partitions
                partition[graphId] = detector.detect(graphs[graphId], refinedPartition, 1000000);
            }
        }
        return partition;
    }

    protected int[] refine(Graph graph, int[] initialPartition){
        // Put each partition into a separate graph for isolated louvain community detection
        // Normalize the partition since decomposition assumes normalized partitions
        Util.normalizeValues(initialPartition);
        Graph[] subGraphs = graph.decompose(initialPartition);
        ArrayList<Graph> largeGraphs = new ArrayList<>();
        int[][] subPartitions = new int[subGraphs.length][];
        // Run louvain on subGraphs separated by initialPartition
        for(int graphId = 0 ; graphId < subGraphs.length ; graphId++){
            Graph subGraph = subGraphs[graphId];
            int nodeCount = subGraph.getNodeCount();
            int edgeCount = subGraph.getEdgeCount();
            if(nodeCount < 4) { // trivial all in on group
                subPartitions[graphId] = Util.intArray(nodeCount, 0);
            } else if (edgeCount < LARGE_SUB_GRAPH){ // Initially place each node in a separate group
                subPartitions[graphId] = Util.ramp(nodeCount);
                subPartitions[graphId] = newDetector().detect(subGraph, subPartitions[graphId], 1000000);
            } else { // Add to list to be processed in batch along other large graphs (if any)
                subGraph.setId(graphId); // to be recognized in an array
                largeGraphs.add(subGraph);
            }
        }
        // Parallel batch detection of large graphs
        if(largeGraphs.size() > 0){
            int[][] partitions = newDetector().detect(largeGraphs.toArray(new Graph[0]), 1000000);
            for(int largeId = 0 ; largeId < partitions.length ; largeId++){
                int graphId = largeGraphs.get(largeId).getId();
                subPartitions[graphId] = partitions[largeId];
            }
        }
        // Recursive refinement of node groups that have been partitioned
        int[] refinedPartition = new int[initialPartition.length];
        int[] groupCounts = new int[subGraphs.length];
        // This variables are used to convert local groupId of partitions to global unique groupIds
        int globalIdOffset = 0;
        for(int graphId = 0 ; graphId < subGraphs.length ; graphId ++){
            // Number of unique sub groups in graphId
            groupCounts[graphId] = Statistics.array(subPartitions[graphId]).uniqueCount;
            if(groupCounts[graphId] <= 1) continue; // subGraph is remained un-partitioned
            Graph subGraph = subGraphs[graphId];
            // subGraph is further partitioned so refine this subGraph recursively
            subPartitions[graphId] = refine(subGraph, subPartitions[graphId]);
            // Number of groups after recursive refinement (may be unchanged)
            groupCounts[graphId] = Statistics.array(subPartitions[graphId]).uniqueCount;
            /*
                Unique groupId of nodes
                For example, partition 4: {0, 1, 2, 3} is further partitioned into 0: {0, 1} and 1: {2, 3}
                thus id 6 is assigned to sub group 4-0, and id 7 to sub group 4-1
            */
            int graphNodeCount = subGraph.getNodeCount();
            int[] subNodeToNode = subGraph.getListMatrix().getToRaw()[0];
            OpenIntIntHashMap toNormal = graph.getListMatrix().getToNormal()[0]; // rawNodeId to normalNodeId
            for(int subNodeId = 0 ; subNodeId < graphNodeCount ; subNodeId++){
                int nodeId = toNormal.get(subNodeToNode[subNodeId]);
                int globalId = subPartitions[graphId][subNodeId] + globalIdOffset;
                refinedPartition[nodeId] = globalId;
            }
            // Shift globalId more than necessary to avoid collision with the next group
            globalIdOffset += graphNodeCount;
        }
        // Termination condition
        int totalGroupCount = Util.sum(groupCounts);
        if(totalGroupCount == subGraphs.length){ // no subGraph is further partitioned
            return refinedPartition;
        }
        // Fold the refined groups and optimize the folded network starting with initialPartition
        // Create a map from refinedPartition to initial partition so as to
        // connect folded super nodes -> refinedPartition -> initialPartition
        int[] refinedToInitial = new int[graph.getNodeCount()];
        for(int nodeId = 0 ; nodeId < refinedPartition.length ; nodeId++){
            int refinedGroupId = refinedPartition[nodeId];
            // The same assignment may be executed several times but it is okay for code simplicity
            refinedToInitial[refinedGroupId] = initialPartition[nodeId];
        }
        Graph folded = graph.fold(refinedPartition);
        int[] superNodeToRefined = folded.getListMatrix().getToRaw()[0];
        int[] foldedInitialPartition = new int[folded.getNodeCount()];
        for(int superNodeId = 0 ; superNodeId < foldedInitialPartition.length ; superNodeId++){
            foldedInitialPartition[superNodeId] = refinedToInitial[superNodeToRefined[superNodeId]];
        }
        // Multiple refined partitions (multiple nodes) may go under one partition in folded graph
        // Thus, the group id of nodes must change from refined to folded
        int[] foldedPartition = newDetector().detect(folded, foldedInitialPartition, 1000000);
        int[] refinedToFolded = new int[graph.getNodeCount()];
        for(int superNodeId = 0 ; superNodeId < foldedInitialPartition.length ; superNodeId++){
            refinedToFolded[superNodeToRefined[superNodeId]] = foldedPartition[superNodeId];
        }
        // Assign groups of folded partition to their corresponding nodes
        for(int nodeId = 0 ; nodeId < refinedPartition.length ; nodeId++){
            refinedPartition[nodeId] = refinedToFolded[refinedPartition[nodeId]];
        }
        return refinedPartition;
    }

//    /**
//     * Change the group id of disconnected regions inside a group to different ids
//     * @param graph
//     * @param partition
//     */
//    public static int[] postProcess(Graph graph, int[] partition){
//        int[] postPartition = Util.intArray(partition.length, -1);
//        int groupId = 0;
//        for(int nodeId = 0 ; nodeId < postPartition.length ; nodeId++){
//            if(postPartition[nodeId] != -1){
//                continue; // group of n-th node is determined already
//            }
//            markNeighbors(nodeId, groupId, graph, partition, postPartition);
//            // Assign a new group id to the next connected component of same group
//            groupId++;
//        }
//        return postPartition;
//    }
//
//    /**
//     * Recursively set the group id of the given node for its co-group neighbors,
//     * changing the input postPartition
//     * @param nodeId
//     * @param graph
//     * @param partition
//     * @param postPartition this will be changed
//     *
//     */
//    private static void markNeighbors(int nodeId, int groupId, Graph graph, int[] partition, int[] postPartition){
//        postPartition[nodeId] = groupId; // execute the post processed group id
//        int[] neighbors = graph.getColumns(nodeId);
//        float[] linkValues = graph.getValues(nodeId);
//        for(int n = 0 ; n < neighbors.length ; n++){
//            int neighborId = neighbors[n];
//            if(postPartition[neighborId] == -1 && linkValues[n] > 0
//                    && partition[neighborId] == partition[nodeId]){
//                // Go to unprocessed neighbors with positive links and same groups
//                markNeighbors(neighborId, groupId, graph, partition, postPartition);
//            }
//        }
//    }
}
