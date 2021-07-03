package network.optimization;

import network.Shared;
import network.core.*;
import cern.colt.map.OpenIntIntHashMap;

import java.util.ArrayList;

/**
 * Package.Optimization procedure originally proposed by M. Rosvall and C. T. Bergstrom
 * Paper: 2009 Fast stochastic and recursive search algorithm
 * This adds a recursive refinement procedure to the original Louvain optimization algorithm
 */
abstract public class RosvallBergstrom extends ParallelLouvain {

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
        ParallelLouvain detector = newInstance();
        Shared.log("Louvain algorithm started");
        int[][] partition = detector.detect(graphs, 1000);
        for (int graphId = 0; graphId < graphs.length; graphId++) {
            for (int r = 0; r < refineCount; r++) {
                Shared.log("Refinement No. " + (r + 1) + " for graph size " + graphs[graphId].getNodeCount());
                // Run the recursive submodule movement to improve the optimization
                int[] refinedPartition = refine(graphs[graphId], partition[graphId]);
                // Run single node movement upon refined partitions
                partition[graphId] = detector.detect(graphs[graphId], refinedPartition, 1000);
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
            if (nodeCount < 4) { // do not refine
                subPartitions[graphId] = Util.initArray(nodeCount, 0);
            } else {
                subGraph.setId(graphId); // to be refined later
                parallelGraphs.add(subGraph);
            }
        }
        // Parallel detection of large graphs
        if (parallelGraphs.size() > 0) {
            int[][] partitions = newInstance()
                    .setThreadCount(1) // use 1 thread since it is not effective yet
                    .detect(parallelGraphs.toArray(new MultiGraph[0]), 1000);
            for (int processedId = 0; processedId < partitions.length; processedId++) {
                int graphId = parallelGraphs.get(processedId).getId();
                subPartitions[graphId] = partitions[processedId];
            }
        }
        // Recursive refinement of node groups that have been partitioned
        for (int subGraphId = 0; subGraphId < subGraphs.length; subGraphId++) {
            // Number of unique sub groups in graphId (empty graph will have 0 sub groups)
            ArrayStatistics statistics = Statistics.array(subPartitions[subGraphId]);
            int groupCount = Math.max(1, statistics.uniqueCount);
            if (groupCount == 1) continue; // subGraph is remained un-partitioned
            // subGraph is further partitioned so refine this subGraph recursively
            subPartitions[subGraphId] = refine(subGraphs[subGraphId], subPartitions[subGraphId]);
        }
        /*
            Unique groupId of nodes
            For example, if partition 4: {0, 1, 2, 3} is further partitioned into 0: {0, 1} and 1: {2, 3}
            then id 6 is assigned to sub group 4-0, and id 7 to sub group 4-1
        */
        int[] refinedPartition = Util.initArray(initialPartition.length, -1);
        int[] subGroupCount = new int[subGraphs.length];
        int globalIdOffset = 0; // For converting local groupIds of subGraphs to global Ids
        for (int subGraphId = 0; subGraphId < subGraphs.length; subGraphId++) {
            MultiGraph subGraph = subGraphs[subGraphId];
            // Number of unique sub groups in graphId (empty graph will have 0 sub groups)
            ArrayStatistics statistics = Statistics.array(subPartitions[subGraphId]);
            subGroupCount[subGraphId] = Math.max(1, statistics.uniqueCount);
            if (subGraph.isEmpty()) continue;
            int subNodeIdRange = subGraph.getNodeMaxId() + 1;
            int subGroupIdRange = statistics.maxValue + 1;
            int[] subNodeToNode = subGraph.getToRaw()[0];
            for (int subNodeId = 0; subNodeId < subNodeIdRange; subNodeId++) {
                int nodeId = subNodeToNode[subNodeId];
                int globalGroupId = subPartitions[subGraphId][subNodeId] + globalIdOffset;
                refinedPartition[nodeId] = globalGroupId;
            }
            // Shift globalId more than necessary to avoid collision with the next group
            globalIdOffset += subGroupIdRange;
        }
        // Put isolated nodes (of subGraphs with no edges) in isolated groups
        for (int nodeId = 0 ; nodeId < refinedPartition.length ; nodeId++) {
            if (refinedPartition[nodeId] < 0) {
                refinedPartition[nodeId] = globalIdOffset++;
            }
        }
        // Termination condition
        int totalGroupCount = Util.sum(subGroupCount);
        if (totalGroupCount == subGraphs.length) { // no subGraph is further partitioned
            return refinedPartition;
        }
        // Fold the refined groups and optimize the folded network
        MultiGraph folded = graph.fold(refinedPartition);
        int[] foldedNodeToRefinedPart = folded.getToRaw()[ListMatrix.ROW];
        int[] foldedInitialPartition = Util.ramp(folded.getNodeCount());
        // Multiple refined partitions (multiple nodes) may go under one partition in folded graph
        // Thus, the group id of nodes must change from refined to folded
        int[] foldedPartition = newInstance().detect(folded, foldedInitialPartition, 1000);
        int[] refinedPartToFoldedPart = new int[graph.getNodeCount()];
        for (int foldedNodeId = 0; foldedNodeId < foldedPartition.length; foldedNodeId++) {
            refinedPartToFoldedPart[foldedNodeToRefinedPart[foldedNodeId]] = foldedPartition[foldedNodeId];
        }
        // Assign groups of folded partition to their corresponding nodes
        for (int nodeId = 0; nodeId < refinedPartition.length; nodeId++) {
            refinedPartition[nodeId] = refinedPartToFoldedPart[refinedPartition[nodeId]];
        }
        return refinedPartition;
    }
}