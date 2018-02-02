package Optimization;

import Core.Graph;
import Core.ListMatrix;
import Core.SiGraph;

import static Core.ListMatrix.ROW;

/**
 * Optimization procedure proposed by M. Rosvall and C. T. Bergstrom
 * Paper: 2009 Fast stochastic and recursive search algorithm
 * This adds a recursive refinement procedure to the original Louvain optimization algorithm
 */
public class RosvallBergstrom {

    /**
     * Find the best partition of the graph
     * @param graph
     * @param refineCount number of refinements after the first application of louvain method
     * @return
     */
    protected int[] partition(Graph graph, int refineCount){
        if(!graph.hasEdge()){
            return null; // no edge to perform partitioning
        }
        int[] initialPartition = new int[graph.getNodeCount()];
        // Initially place each node in a separate group
        for(int p = 0 ; p < initialPartition.length ; p++){
            initialPartition[p] = p;
        }
        // Do the graph folding until objective stops improving
        int [] partition = louvain(graph, initialPartition, 1000000);
        for(int r = 0 ; r < refineCount ; r++){
            // Run the recursive submodule movement to improve the optimization
            int[] refinedPartition = refine(graph, partition);
            // Run single node movement upon refined partitions
            partition = louvain(graph, refinedPartition, 1000000);
        }
        return partition;
    }

    protected int[] refine(Graph graph, int[] initialPartition){
        Graph[] subGraphs = graph.decompose(initialPartition);
        int[] partition = new int[initialPartition.length];
        return partition;
    }

    /**
     * Perform louvain optimization on the graph with the given initial partition
     * @param graph
     * @param initialPartition
     * @param rebuildCount
     * @return
     */
    protected int[] louvain(Graph graph, int[] initialPartition, int rebuildCount){
        /**
         * A node n could be an aggregation of multiple nodes folded into on super node
         * pNC: positive weight from node n to group c, where group(n) = c
         * pCN: positive weight from group c to node n, where group(n) = c
         * pNCp : positive weight from node n to group c, where group(n) <> cp
         * pCpN : positive weight from group cp to node n, where group(n) <> cp
         * pin : positive in-weight of node n
         * pout : positive out-weight of node n
         * pself : positive self-loop node n
         * n*** : negative weight ...
         */
        int[] partition = initialPartition.clone();
        Graph transpose = (Graph) graph.transpose(true);
        float improvement = greedy(graph, transpose, partition);
        if(improvement <= 0.0 || rebuildCount == 0.0){
            // No further improvement was made by coarse-grain
            // or no further coarse-grain is needed
            return partition;
        }
        // Rebuild the network of communities:
        // Fold negative & positive sub-graphs separately according to partition
        Graph foldedGraph = graph.fold(partition);
        // At least 1% decrease in network size is expected
        double sizeRatio = (double) foldedGraph.getNodeCount() / graph.getNodeCount();
        if(sizeRatio > 0.99){
            return partition;
        }
        // Recursive louvain optimization, partition the network of groups
        int[] superPartition = louvain(foldedGraph,
                getInitialPartition(foldedGraph.getNodeCount()), rebuildCount - 1);
        /**
         * Node with groupId = g in the current level gets groupId = map[g] after coarsening
         * e.g. a node x is in group 10, group 10 is node 0 in folded graph
         * node 0 gets super-group 4, so node x is in group 4
         */
        int[] superGroup = new int[graph.getNodeCount()];
        ListMatrix foldedMatrix = foldedGraph.getListMatrix();
        int[] superNodeToGroup = foldedMatrix.getToRaw()[ROW];
        for(int superNodeId = 0 ; superNodeId < superPartition.length ; superNodeId++){
            // groupId of foldedGroup before being folded-normalized into a superNode
            int groupId = superNodeToGroup[superNodeId];
            superGroup[groupId] = superPartition[superNodeId];
        }
        // Change group id of node x with the corresponding superGroup of group id
        for(int nodeId = 0 ; nodeId < partition.length ; nodeId++){
            partition[nodeId] = superGroup[partition[nodeId]];
        }
        return partition;
    }

    /**
     * Greedy optimization per folding that is used in louvain,
     * Greedily moves nodes into best neighbor communities until convergence
     * @param graph
     * @param transpose transpose of graph for faster traverse on columns
     * @param partition this is the initial partition, changes are applied on this
     * @return improvement in objective function
     */
    protected float greedy(Graph graph, Graph transpose, int[] partition){
        // It is implemented according to each objective function
        return -1;
    }

    /**
     * Local change in the objective function by moving nodes between groups
     * @param parameters
     * @return
     */
    protected float localChange(ObjectiveParameters parameters){
        return 0;
    }

    /**
     * Returns an initial partition with each node in a separate group
     * @param size
     * @return
     */
    protected int[] getInitialPartition(int size){
        int[] partition = new int[size];
        for(int p = 0 ; p < partition.length ; p++){
            partition[p] = p;
        }
        return partition;
    }
}
