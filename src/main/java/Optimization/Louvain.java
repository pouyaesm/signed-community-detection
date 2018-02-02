package Optimization;

import Core.Graph;
import Core.SiGraph;

public class Louvain {
    /**
     * Greedily find a partition that optimizes the given graph
     */
    public void optimize(SiGraph graph){

        Graph pGraph = graph.getGraph(SiGraph.POSITIVE);
        int nodeCount = pGraph.getNodeCount();
        // group statistics for group cp where group(n) <> cp (groupId, pCpN, pNCp, nCpN, nNCp)
        int[][] statistics = new int[nodeCount][5];

    }
}
