package network.core;

import cern.colt.map.OpenIntIntHashMap;
import cern.jet.math.Mult;

import java.util.*;

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
     * Maximum node id
     */
    private int nodeMaxId;

    /**
     * Sum of sub-graph edge counts
     */

    private int edgeCount;

    /**
     * If at least one graph has an edge
     */
    private boolean isEmpty;

    public MultiGraph(){
        super();
        init();
    }

    public MultiGraph(Graph graph){
        super(graph);
        init();
    }

    public MultiGraph(MultiGraph graph){
        super(graph);
        graphs = graph.graphs;
        isEmpty = graph.isEmpty;
        nodeMaxId = graph.nodeMaxId;
        nodeCount = graph.nodeCount;
        edgeCount = graph.edgeCount;
    }

    public MultiGraph init(){
        graphs =  new HashMap<>();
        isEmpty = true;
        nodeMaxId = -1;
        return this;
    }

    /**
     * @param partition
     * @return
     */
    @Override
    public MultiGraph[] decompose(int[] partition){
        // Collect the row and column id of all type-graphs to
        // do a unified node id normalization
        int[][] ids = new int[2 * graphs.size()][]; // row and column list of each type-graph
        Iterator iterator = graphs.entrySet().iterator();
        int index = 0;
        while (iterator.hasNext()){
            Graph graph = (Graph) ((Map.Entry) iterator.next()).getValue();
            ids[index++] = graph.getRows();
            ids[index++] = graph.getColumns();
        }
        OpenIntIntHashMap[] mapToNormal = new OpenIntIntHashMap[2];
        mapToNormal[ROW] = Util.normalizeIds(ids);
        mapToNormal[COL] = (OpenIntIntHashMap) mapToNormal[ROW].clone();
        // Decompose each type-graph based on the unified normalization
        HashMap<Integer, Graph[]> subGraphs = new HashMap<>(graphs.size());
        iterator = graphs.entrySet().iterator();
        int groupCount = 0;
        while (iterator.hasNext()){
            Map.Entry graphEntry = (Map.Entry) iterator.next();
            int typeId = (int) graphEntry.getKey();
            Graph graph = (Graph) graphEntry.getValue();
            //--------------------------------
            Graph[] decomposedGraphs = graph.decompose(partition, mapToNormal);
            //--------------------------------
            subGraphs.put(typeId, decomposedGraphs);
            groupCount = decomposedGraphs.length; // re-assigned redundantly!
        }
        // Put all types of each group into one multi-graph
        MultiGraph[] multiGraphs = new MultiGraph[groupCount];
        for(int m = 0 ; m < multiGraphs.length ; m++){
            multiGraphs[m] = new MultiGraph();
        }
        iterator = subGraphs.entrySet().iterator();
        while (iterator.hasNext()){
            Map.Entry graphEntry = (Map.Entry) iterator.next();
            int typeId = (int) graphEntry.getKey();
            Graph[] typeSubGraphs = (Graph[]) graphEntry.getValue();
            // add sub graph of type 'typeId' to multi-graph 'groupId'
            for(int groupId = 0 ; groupId < typeSubGraphs.length ; groupId++){
                multiGraphs[groupId].addGraph(typeId, typeSubGraphs[groupId]);
            }
        }
        // Set attributes of multi-graph for decomposed multi-graphs
        // Assumption: raw id of sub-multiGraphs points to the same ids as this multiGraph
        setAttributesInto(multiGraphs);
        return multiGraphs;
    }

    /**
     * Transpose each graph type
     * @return
     */
    @Override
    public MultiGraph transpose(boolean clone){
        Iterator iterator = graphs.entrySet().iterator();
        // For cloning, only keep the attributes
        MultiGraph transposedMultiGraph = clone ?
                (MultiGraph) new MultiGraph().setAttributes(cloneAttributes()) : this;
        while (iterator.hasNext()){
            Map.Entry graphEntry = (Map.Entry) iterator.next();
            int typeId = (int) graphEntry.getKey();
            Graph typeGraph = (Graph) graphEntry.getValue();
            // Add transpose of each type graph to multiGraph
            // If clone = false, each type replaces previous one
            transposedMultiGraph.addGraph(typeId, new Graph(typeGraph.transpose(clone)));
        }
        return transposedMultiGraph;
    }

    @Override
    public MultiGraph fold(int[] partition) {
        Iterator iterator = graphs.entrySet().iterator();
        MultiGraph foldedMultiGraph = new MultiGraph();
        while (iterator.hasNext()){
            Map.Entry graphEntry = (Map.Entry) iterator.next();
            int typeId = (int) graphEntry.getKey();
            Graph typeGraph = (Graph) graphEntry.getValue();
            // Add transpose of each type graph to multiGraph
            foldedMultiGraph.addGraph(typeId, typeGraph.fold(partition));
        }
        // Aggregate the attributes of multiGraph into folded multi-graph
        foldedMultiGraph.setAttributes(aggregateAttributes(partition, foldedMultiGraph));
        return foldedMultiGraph;
    }

    public MultiGraph addGraph(int typeId, Graph graph){
        boolean reAdded = graphs.containsKey(typeId);
        graphs.put(typeId, graph);
        if(graph == null || reAdded) return this;
        nodeCount = Math.max(graph.getNodeCount(), nodeCount);
        edgeCount += graph.getEdgeCount();
        isEmpty = isEmpty && graph.isEmpty();
        // Take the largest id map as the representative
        int currentMapSize = getToRaw() == null ? 0 : getToRaw()[0].length;
        int graphMapSize = graph.getToRaw() == null ? 0 : graph.getToRaw()[0].length;
        if(currentMapSize < graphMapSize){
            setToRaw(graph.getToRaw());
            setToNormal(graph.getToNormal());
            nodeMaxId = graphMapSize  - 1; // raw array supports maximum node id as input
        }
        return this;
    }

    public Graph getGraph(int typeId) {
        return graphs.get(typeId);
    }

    @Override
    public MultiGraph clone() {
        MultiGraph clone = new MultiGraph(super.clone());
        clone.graphs = (HashMap<Integer, Graph>) graphs.clone();

        clone.isEmpty = isEmpty;
        clone.edgeCount = edgeCount;
        clone.nodeCount = nodeCount;
        clone.nodeMaxId = nodeMaxId;
        return clone;
    }

    /**
     * Return true if at least one sub-graph has edge
     * @return
     */
    @Override
    public boolean isEmpty(){
        return isEmpty;
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
    public int getNodeMaxId() {
        return nodeMaxId;
    }
}
