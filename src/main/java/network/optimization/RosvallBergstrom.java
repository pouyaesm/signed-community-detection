package network.optimization;

import network.Shared;
import network.core.Graph;
import network.core.MultiGraph;
import network.core.Statistics;
import network.core.Util;
import cern.colt.map.OpenIntIntHashMap;

import java.util.ArrayList;

/**
 * Package.Optimization procedure originally proposed by M. Rosvall and C. T. Bergstrom
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
     *
     * @param graph
     * @param refineCount number of refinements after the first application of detect method
     * @return
     */
    protected int[] partition(MultiGraph graph, int refineCount) {
        MultiGraph[] graphs = {graph};
        return partition(graphs, refineCount)[0];
    }

    /**
     * Find the best partition for each graph via parallel detection
     *
     * @param graphs
     * @param refineCount number of refinements after the first application of detect method
     * @return
     */
    protected int[][] partition(MultiGraph[] graphs, int refineCount) {
        ParallelLouvain detector = newDetector();
        Shared.log("Louvain algorithm started");
        int[][] partition = detector.detect(graphs, 1000000);
        for (int graphId = 0; graphId < graphs.length; graphId++) {
            for (int r = 0; r < refineCount; r++) {
                Shared.log("Refinement No. " + (r + 1) + " for graph size " + graphs[graphId].getNodeCount());
                // Run the recursive submodule movement to improve the optimization
                int[] refinedPartition = refine(graphs[graphId], partition[graphId]);
                // Run single node movement upon refined partitions
                partition[graphId] = detector.detect(graphs[graphId], refinedPartition, 1000000);
            }
        }
        return partition;
    }

    protected int[] refine(MultiGraph graph, int[] initialPartition) {
        // Put each partition into a separate graph for isolated louvain community detection
        // Normalize the partition since decomposition assumes normalized partitions
        Util.normalizeValues(initialPartition);
        MultiGraph[] subGraphs = graph.decompose(initialPartition);
        ArrayList<MultiGraph> parallelGraphs = new ArrayList<>();
        int[][] subPartitions = new int[subGraphs.length][];
        // Run louvain on subGraphs separated by initialPartition
        for (int graphId = 0; graphId < subGraphs.length; graphId++) {
            MultiGraph subGraph = subGraphs[graphId];
            int nodeCount = subGraph.getNodeCount();
            if (nodeCount < 4) { // trivial all in on group
                subPartitions[graphId] = Util.intArray(nodeCount, 0);
            } else {
                subGraph.setId(graphId); // to be recognized later
                parallelGraphs.add(subGraph);
            }
        }
        // Parallel batch detection of large graphs
        if (parallelGraphs.size() > 0) {
            int[][] partitions = newDetector().detect(parallelGraphs.toArray(new MultiGraph[0]), 1000000);
            for (int largeId = 0; largeId < partitions.length; largeId++) {
                int graphId = parallelGraphs.get(largeId).getId();
                subPartitions[graphId] = partitions[largeId];
            }
        }
        // Recursive refinement of node groups that have been partitioned
        int[] refinedPartition = new int[initialPartition.length];
        int[] groupCounts = new int[subGraphs.length];
        // This variables are used to convert local groupId of partitions to global unique groupIds
        int globalIdOffset = 0;
        for (int graphId = 0; graphId < subGraphs.length; graphId++) {
            // Number of unique sub groups in graphId
            groupCounts[graphId] = Statistics.array(subPartitions[graphId]).uniqueCount;
            if (groupCounts[graphId] <= 1) continue; // subGraph is remained un-partitioned
            MultiGraph subGraph = subGraphs[graphId];
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
            int[] subNodeToNode = subGraph.getToRaw()[0];
            OpenIntIntHashMap toNormal = graph.getToNormal()[0]; // rawNodeId to normalNodeId
            for (int subNodeId = 0; subNodeId < graphNodeCount; subNodeId++) {
                int nodeId = toNormal.get(subNodeToNode[subNodeId]);
                int globalId = subPartitions[graphId][subNodeId] + globalIdOffset;
                refinedPartition[nodeId] = globalId;
            }
            // Shift globalId more than necessary to avoid collision with the next group
            globalIdOffset += graphNodeCount;
        }
        // Termination condition
        int totalGroupCount = Util.sum(groupCounts);
        if (totalGroupCount == subGraphs.length) { // no subGraph is further partitioned
            return refinedPartition;
        }
        // Fold the refined groups and optimize the folded network starting with initialPartition
        // Create a maps from refinedPartition to initial partition so as to
        // connect folded super nodes -> refinedPartition -> initialPartition
        int[] refinedToInitial = new int[graph.getNodeCount()];
        for (int nodeId = 0; nodeId < refinedPartition.length; nodeId++) {
            int refinedGroupId = refinedPartition[nodeId];
            // The same assignment may be executed several times but it is okay for code simplicity
            refinedToInitial[refinedGroupId] = initialPartition[nodeId];
        }
        MultiGraph folded = graph.fold(refinedPartition);
        int[] superNodeToRefined = folded.getToRaw()[0];
        int[] foldedInitialPartition = new int[folded.getNodeCount()];
        for (int superNodeId = 0; superNodeId < foldedInitialPartition.length; superNodeId++) {
            foldedInitialPartition[superNodeId] = refinedToInitial[superNodeToRefined[superNodeId]];
        }
        // Multiple refined partitions (multiple nodes) may go under one partition in folded graph
        // Thus, the group id of nodes must change from refined to folded
        int[] foldedPartition = newDetector().detect(folded, foldedInitialPartition, 1000000);
        int[] refinedToFolded = new int[graph.getNodeCount()];
        for (int superNodeId = 0; superNodeId < foldedInitialPartition.length; superNodeId++) {
            refinedToFolded[superNodeToRefined[superNodeId]] = foldedPartition[superNodeId];
        }
        // Assign groups of folded partition to their corresponding nodes
        for (int nodeId = 0; nodeId < refinedPartition.length; nodeId++) {
            refinedPartition[nodeId] = refinedToFolded[refinedPartition[nodeId]];
        }
        return refinedPartition;
    }
}