package network.core;

/**
 * Marks disconnected nodes inside a group separately
 */
public class ConnectedCoGroups extends ConnectedComponents {

    /**
     * Node group ids
     */
    int[] partition;

    public ConnectedCoGroups(Graph graph, int[] partition){
        super(graph);
        this.partition = partition;
    }

    /**
     * Neighbor nodes are considered connected if reside in the same group
     * and have a positive link
     * @param nodeId
     * @param neighborId id of neighbor node
     * @param neighborIndex index of neighbor in the sparse array of nodeId
     * @return
     */
    @Override
    protected boolean isConnected(int nodeId, int neighborId, int neighborIndex) {
        float linkValue = getGraph().getValues(nodeId)[neighborIndex];
        return linkValue > 0 && partition[nodeId] == partition[neighborId];
    }
}
