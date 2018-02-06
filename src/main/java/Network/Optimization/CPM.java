package Network.Optimization;

import Network.Core.*;

/**
 * Graph partitioning based on Constant Potts Model objective function
 */
public class CPM extends RosvallBergstrom {
    /**
     * Relative importance of positive links compared to negative links: [0, 1]
     */
    private float alpha;

    /**
     * Resolution parameter of CPM equation
     */
    private float resolution;

    public int[] detect(Graph graph, float resolution, float alpha, int refineCount){
        Graph[] graphs = {graph};
        return detect(graphs, resolution, alpha, refineCount)[0];
    }

    public int[][] detect(Graph[] graphs, float resolution, float alpha, int refineCount){
        if(alpha < 0 || alpha > 1 || resolution < 0){
            try {
                throw new Exception("alpha must be [0, 1], and resolution > 0");
            } catch (Exception e) {
                e.printStackTrace();
            }
            return null;
        }
        setAlpha(alpha);
        setResolution(resolution);
        for(Graph graph : graphs) {
            /*
                Set the number of nodes inside each node (which is 1)
                this size will increase during the folding of nodes into one node
             */
            float [][] nodeSizes = new float[graph.getNodeCount()][1];
            for(int n = 0 ; n < nodeSizes.length ; n++){
                nodeSizes[n][0] = 1;
            }
            graph.setAttributes(nodeSizes);
        }
        int[][] bestPartition = partition(graphs,refineCount);
        for(int graphId = 0 ; graphId < graphs.length ; graphId++){
            bestPartition[graphId] = RosvallBergstrom.postProcess(graphs[graphId], bestPartition[graphId]);
        }
        return bestPartition;
    }

    @Override
    protected float greedy(Graph graph, Graph transpose, int[] partition) {
        int N = graph.getNodeCount();
        // Queue of neighbor groups and their statistics (groupId, pCpK, pKCp, nCpK, nKCp)
        float[][] groupQueue = new float[N][];
        int queueHead = 0; // Head of queue indicating the first empty cell of queue array to insert
        // groupNeighbor[ng] = q >= 0 means group ng is a neighbor of current group g
        // and it is placed in position q of queue
        int[] neighborGroupQueueIndex = new int[N];
        for(int n = 0 ; n < N ; n++){
            int groupId = partition[n];
            if(neighborGroupQueueIndex[groupId] == 0) {
                groupQueue[groupId] = new float[5];
                neighborGroupQueueIndex[groupId] = -1;
            }
        }
        // Number of nodes in each group (each node of a group may be a folded super-node)
        int[] nodeCount = new int[N];
        float[][] nodeAttributes = graph.getAttributes();
        for(int n = 0 ; n < N ; n++){
            // Node count of each super-node has been saved in the first attribute place by convention
            // in detect function
            nodeCount[partition[n]] += (int) nodeAttributes[n][0];
        }
        //----------------
        // holds group CPM statistics of each node
        CPMParameters pPositive = new CPMParameters();
        CPMParameters pNegative = new CPMParameters();
        boolean hamImproved = true; // whether objective is improved during a pass or not
        float hamChange = 0; // total change of hamiltonian objective
        float movedNodes = N; // number of moved nodes into other groups (0 if groups stay the same)
        // At least 1% node movement is expected to redo the merge pass
        while (hamImproved && (movedNodes / N) >= 0.01){
            int[] permute = Util.permute(N); // nodes will be visited in random order
            hamImproved = false;
            movedNodes = 0;
            for(int k = 0 ; k < N ; k++){
                int nodeId = permute[k];
                int groupId = partition[nodeId];
                // Number of nodes if nodeId is a folded one
                int nodeSize = (int) graph.getAttributes()[nodeId][0];
                // Initialize parameters used in objective change calculation
                pPositive.KC = pNegative.KC = 0;
                pPositive.CK = pNegative.CK = 0;
                pPositive.Kin = pNegative.Kin = 0;
                pPositive.Kout = pNegative.Kout = 0;
                pPositive.Kself = pNegative.Kself = 0;
                pPositive.NC = pNegative.NC = nodeCount[groupId];
                pPositive.Nk = pNegative.Nk = nodeSize;
                pPositive.resolution = this.resolution;
                pNegative.resolution = 0; // this is described in the paper
                // Get outward-inward neighbor groups of nodeId
                // For outward neighbors graph is used and for inward neighbors its transpose is used
                // since the sparse data structure is row-based and best suited for column traverse
                Graph outOrIn;
                int outward = 0; // index of outward direction by convention
                for(int direction = 0 ; direction < 2 ; direction++){
                    outOrIn = direction == outward ? graph : transpose;
                    if(!outOrIn.hasEdge()){
                        continue; // no edge to process
                    }
                    int[] neighbors = outOrIn.getColumns(nodeId);
                    float[] linkValues = outOrIn.getValues(nodeId);
                    for(int n = 0 ; n < neighbors.length ; n++){
                        int neighborId = neighbors[n];
                        float linkValue = linkValues[n];
                        int neighborGroupId = partition[neighborId];
                        if(nodeId == neighborId){ // self loop (will be counted two times)
                            if(linkValue > 0){
                                pPositive.Kself += linkValue / 2;
                            }else{
                                pNegative.Kself -= linkValue / 2;
                            }
                        }
                        if(groupId == neighborGroupId){ // link inside the group
                            if(direction == outward){ // from node to its group
                                if(linkValue > 0) pPositive.KC += linkValue;
                                else pNegative.KC -= linkValue;
                            }else{ // from group to node
                                if(linkValue > 0) pPositive.CK += linkValue;
                                else pNegative.CK -= linkValue;
                            }
                        }else{ // link toward neighbor groups
                            // first time this neighbor is visited ?
                            int neighborQueuePosition;
                            if(neighborGroupQueueIndex[neighborGroupId] == -1){
                                groupQueue[queueHead][0] = neighborGroupId;
                                neighborQueuePosition = neighborGroupQueueIndex[neighborGroupId] = queueHead;
                                queueHead++;
                            }else{
                                neighborQueuePosition = neighborGroupQueueIndex[neighborGroupId];
                            }
                            if(direction == outward) {  // from node to neighbor group
                                if(linkValue > 0) groupQueue[neighborQueuePosition][2] += linkValue;
                                else groupQueue[neighborQueuePosition][4] -= linkValue;
                            }else{ // neighbor group to node
                                if(linkValue > 0) groupQueue[neighborQueuePosition][1] += linkValue;
                                else groupQueue[neighborQueuePosition][3] -= linkValue;
                            }
                        }
                        if(direction == outward){
                            if(linkValue > 0) pPositive.Kout += linkValue;
                            else pNegative.Kout -= linkValue;
                        }else{
                            if(linkValue > 0) pPositive.Kin += linkValue;
                            else pNegative.Kin -= linkValue;
                        }
                    } // neighbors of nodeId
                } // graphs for inward or outward neighbors
                /*
                    In formulation of CPM object, each node set is considered in both its group
                    and the group it wants to move into,
                    so add the self-loop of node to its neighbor groups as well
                 */
                for(int queueIndex = 0 ; queueIndex < queueHead ; queueIndex++){
                    // from node to neighbor groups
                    groupQueue[queueIndex][2] += pPositive.Kself;
                    groupQueue[queueIndex][4] += pNegative.Kself;
                    // from neighbor group to node
                    groupQueue[queueIndex][1] += pPositive.Kself;
                    groupQueue[queueIndex][3] += pNegative.Kself;
                }
                // Hamiltonian objective change due to movement of nodeId to neighbor groups
                float bestChange = 0;
                int bestNeighborGroupId = -1;
                for(int queueIndex = 0 ; queueIndex < queueHead ; queueIndex++){
                    int neighborGroupId = (int) groupQueue[queueIndex][0];
                    pPositive.KCp = groupQueue[queueIndex][2];
                    pNegative.KCp = groupQueue[queueIndex][4];
                    pPositive.CpK = groupQueue[queueIndex][1];
                    pNegative.CpK = groupQueue[queueIndex][3];
                    // add node sub-node count to neighbor group temporarily for local change calculation
                    pPositive.NCp = pNegative.NCp = nodeCount[neighborGroupId] + nodeSize;
                    float pChange = localChange(pPositive);
                    float nChange = localChange(pNegative);
                    float change = this.alpha * pChange - (1 - this.alpha) * nChange;
                    if(change < bestChange){
                        bestChange = change;
                        bestNeighborGroupId = neighborGroupId;
                    }
                }
                // If a better neighbor group is found, move the node to that group
                if (bestChange < 0){
                    partition[nodeId] = bestNeighborGroupId;
                    nodeCount[groupId] -= nodeSize;
                    nodeCount[bestNeighborGroupId] += nodeSize;
                    // Hamiltonian is improved in this pass so a next pass is allowed
                    hamImproved = true;
                    hamChange += bestChange;
                    movedNodes++;
                }
                // Clear the data structures for tracking the neighbor groups of next node
                for(int queueIndex = 0 ; queueIndex < queueHead ; queueIndex++){
                    neighborGroupQueueIndex[(int) groupQueue[queueIndex][0]] = -1;
                    for(int statisticsIndex = 0 ; statisticsIndex < 5 ; statisticsIndex++){
                        groupQueue[queueIndex][statisticsIndex] = 0;
                    }
                }
                queueHead = 0; //reset queue header for next nodeId
            } // for each node of graph
        }
        return  - hamChange; // hamiltonian decrease is an improvement
    }

    @Override
    protected float localChange(ObjectiveParameters parameters) {
        CPMParameters p = (CPMParameters) parameters;
        float change = p.KC + p.CK - p.KCp - p.CpK + 2 * p.resolution * p.Nk * (p.NCp - p.NC);
        return change;
    }

    @Override
    public float evaluate(Graph graph, int[] partition, ObjectiveParameters parameters) {
        CPMParameters cpmParameters = (CPMParameters)parameters;
        ListMatrix matrix = graph.getListMatrix();
        PartitionStatistics statistics = MatrixStatistics.partitionStatistics(partition, matrix);
        int N = graph.getNodeCount();
        float positiveHamiltonian = 0;
        float negativeHamiltonian = 0;
        for(int g = 0; g < statistics.size.length ; g++){
            int nodeCount  = statistics.size[g];
            if(nodeCount > 0){
                positiveHamiltonian -= (statistics.positiveCellValue[g]
                        - cpmParameters.resolution * nodeCount * nodeCount); // E+(c) - λ N(c)^2
                negativeHamiltonian -= statistics.negativeCellValue[g]; // E-(c) - 0 * N(c)^2
            }
        }
        float hamiltonian = cpmParameters.alpha * positiveHamiltonian
                - (1 - cpmParameters.alpha) * negativeHamiltonian;
        return hamiltonian;
    }

    @Override
    public CPM newDetector() {
        return (CPM) new CPM()
                .setResolution(resolution)
                .setAlpha(alpha)
                .setThreadCount(getThreadCount());
    }

    public CPM setResolution(float resolution) {
        this.resolution = resolution;
        return this;
    }

    public CPM setAlpha(float alpha) {
        this.alpha = alpha;
        return this;
    }
}
