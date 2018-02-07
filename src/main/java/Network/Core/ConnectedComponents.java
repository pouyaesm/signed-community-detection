package Network.Core;

/**
 * Provides functions regarding connected components in a graph
 */
public class ConnectedComponents {

    private Graph graph;
    /**
     * components[nodeId]: connected component id of nodeId
     */
    private int[] components;

    public ConnectedComponents(){
    }

    public ConnectedComponents(Graph graph){
        init(graph);
    }

    public ConnectedComponents init(Graph graph){
        this.graph = graph;
        this.components = Util.intArray(graph.getNodeCount(), -1);
        return this;
    }

    /**
     * Mark nodes inside each connected component with a unique id
     * The definition of two nodes being "connected" is delegated to a function
     * but having a link in the graph is the minimal condition
     * @return
     */
    public ConnectedComponents execute(){
        int nodeCount = graph.getNodeCount();
        int componentId = 0;
        for(int nodeId = 0 ; nodeId < nodeCount ; nodeId++){
            if(components[nodeId] != -1){
                continue; // node is already visited recursively
            }
            components[nodeId] = componentId;
            markNeighbors(nodeId);
            componentId++; // mark the next connected component
        }
        return this;
    }

    private void markNeighbors(int nodeId){
        int[] neighbors = graph.getColumns(nodeId);
        int componentId = components[nodeId];
        /*
         * Data structure for keeping track of visited neighbors, and cleaning visits
         * for breadth first marking
         */
        int[] neighborQueue = new int[neighbors.length];
        int queueHead = 0;
        // Mark neighbors
        for(int neighborIndex = 0 ; neighborIndex < neighbors.length ; neighborIndex++){
            int neighborId = neighbors[neighborIndex];
            if(components[neighborId] == -1 && isConnected(nodeId, neighborId, neighborIndex)){
                components[neighborId] = componentId;
                neighborQueue[queueHead++] = neighborId; // for recursive call
            }
        }
        // Recursive call to queued neighbors
        for(int q = 0 ; q < queueHead ; q++){
            markNeighbors(neighborQueue[q]);
        }
    }

    /**
     * Connectedness can be specified here, other than having a link in between
     * @param nodeId
     * @param neighborId id of neighbor node
     * @param neighborIndex index of neighbor in the sparse array of nodeId
     */
    protected boolean isConnected(int nodeId, int neighborId, int neighborIndex){
        return true;
    }

    /**
     * Mark the largest component with 0 and the other nodes as -1
     * @return
     */
    public int[] getLargestComponent(){
        int largestComponentId = Util.maxId(Statistics.array(components).frequency);
        int[] largestComponent = new int[components.length];
        for(int nodeId = 0 ; nodeId < largestComponent.length ; nodeId++){
            largestComponent[nodeId] = components[nodeId] == largestComponentId ? 0 : -1;
        }
        return largestComponent;
    }

    public int[] getComponents() {
        return components;
    }

    public Graph getGraph() {
        return graph;
    }
}
