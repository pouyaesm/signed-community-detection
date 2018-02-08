package network.core;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

/**
 * A graph consisting of multiple types of graphs
 */
public class MultiGraph extends Graph {
    private HashMap<Integer, Graph> graphs;

    /**
     * Number of node counts in (one of) sub-graphs
     */
    private int nodeCount;

    /**
     * Sum of sub-graph edge counts
     */

    private int edgeCount;

    /**
     * If at least one graph has an edge
     */
    private boolean hasEdge;

    public MultiGraph(){
        super();
        graphs =  new HashMap<>();
        nodeCount = 0;
        hasEdge = false;
    }

    /**
     * @param partition
     * @return
     */
    @Override
    public MultiGraph[] decompose(int[] partition){
        // Decompose each graph type into sub graphs
        Iterator iterator = graphs.entrySet().iterator();
        HashMap<Integer, Graph[]> subGraphs = new HashMap<>(graphs.size());
        int groupCount = 0;
        while (iterator.hasNext()){
            Map.Entry graphEntry = (Map.Entry) iterator.next();
            int typeId = (int) graphEntry.getKey();
            Graph graph = (Graph) graphEntry.getValue();
            Graph[] decomposedGraphs = graph.decompose(partition);
            subGraphs.put(typeId, decomposedGraphs);
            groupCount = decomposedGraphs.length; // reassigned redundantly!
        }
        // Put all types of each group into one multi-graph
        MultiGraph[] multiGraphs = new MultiGraph[groupCount];
        for(int m = 0 ; m < multiGraphs.length ; m++){
            multiGraphs[m] = newInstance();
        }
        iterator = subGraphs.entrySet().iterator();
        while (iterator.hasNext()){
            Map.Entry graphEntry = (Map.Entry) iterator.next();
            int typeId = (int) graphEntry.getKey();
            Graph[] typeSubGraphs = (Graph[]) graphEntry.getValue();
            // add sub graph of type 'typeId' to multi-graph of corresponding group
            for(int groupId = 0 ; groupId < typeSubGraphs.length ; groupId++){
                multiGraphs[groupId].addGraph(typeId, typeSubGraphs[groupId]);
            }
        }
        return multiGraphs;
    }

    /**
     * Transpose each graph type
     * @return
     */
    @Override
    public MultiGraph transpose(boolean clone){
        Iterator iterator = graphs.entrySet().iterator();
        MultiGraph transposedMultiGraph = newInstance();
        while (iterator.hasNext()){
            Map.Entry graphEntry = (Map.Entry) iterator.next();
            int typeId = (int) graphEntry.getKey();
            Graph typeGraph = (Graph) graphEntry.getValue();
            // Add transpose of each type graph to multiGraph
            transposedMultiGraph.addGraph(typeId, (Graph) typeGraph.transpose(clone));
        }
        return transposedMultiGraph;
    }

    @Override
    public MultiGraph fold(int[] partition) {
        Iterator iterator = graphs.entrySet().iterator();
        MultiGraph foldedMultiGraph = newInstance();
        while (iterator.hasNext()){
            Map.Entry graphEntry = (Map.Entry) iterator.next();
            int typeId = (int) graphEntry.getKey();
            Graph typeGraph = (Graph) graphEntry.getValue();
            // Add transpose of each type graph to multiGraph
            foldedMultiGraph.addGraph(typeId, typeGraph.fold(partition));
        }
        return foldedMultiGraph;
    }

    public MultiGraph addGraph(int typeId, Graph graph){
        graphs.put(typeId, graph);
        nodeCount = Math.max(graph.getNodeCount(), nodeCount);
        edgeCount += graph.getEdgeCount();
        hasEdge = hasEdge || graph.hasEdge();
        return this;
    }

    public Graph getGraph(int typeId) {
        return graphs.get(typeId);
    }

    /**
     * Return true if at least one sub-graph has edge
     * @return
     */
    @Override
    public boolean hasEdge(){
        return hasEdge;
    }

    @Override
    public int getNodeCount() {
        return nodeCount;
    }

    @Override
    public int getEdgeCount() {
        return edgeCount;
    }

    @Override
    public MultiGraph newInstance() {
        return new MultiGraph();
    }
}
