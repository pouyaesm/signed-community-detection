package network.core;

import cern.colt.map.OpenIntIntHashMap;

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
     * Transpose each graph type
     */
    @Override
    public MultiGraph transpose(boolean clone){
        Iterator<Map.Entry<Integer, Graph>> iterator = graphs.entrySet().iterator();
        // For cloning, only keep the attributes
        MultiGraph transposedMultiGraph = clone ?
                (MultiGraph) new MultiGraph().setAttributes(cloneAttributes()) : this;
        while (iterator.hasNext()){
            Map.Entry<Integer, Graph> graphEntry = iterator.next();
            int typeId = graphEntry.getKey();
            Graph typeGraph = graphEntry.getValue();
            // Add transpose of each type graph to multiGraph
            // If clone = false, each type replaces previous one
            transposedMultiGraph.addGraph(typeId, new Graph(typeGraph.transpose(clone)));
        }
        return transposedMultiGraph;
    }

    @Override
    public MultiGraph fold(int[] partition) {
        Iterator<Map.Entry<Integer, Graph>> iterator = graphs.entrySet().iterator();
        MultiGraph foldedMultiGraph = new MultiGraph();
        ArrayList<Graph> foldedTypes = new ArrayList<>(graphs.size());
        while (iterator.hasNext()){
            Map.Entry<Integer, Graph> graphEntry = iterator.next();
            int typeId = graphEntry.getKey();
            Graph typeGraph = graphEntry.getValue();
            foldedTypes.add((Graph) typeGraph.fold(partition).setId(typeId));
        }
        // Add each folded type graph to multiGraph
        for(Graph foldedType : foldedTypes){
            foldedMultiGraph.addGraph(foldedType.getId(), foldedType);
        }

        // Aggregate the attributes of multiGraph into folded multi-graph
        foldedMultiGraph.setAttributes(aggregateAttributes(partition, foldedMultiGraph));
        return foldedMultiGraph;
    }


    @Override
    public MultiGraph[] decompose(int[] partition){
        // Decompose each type-graph based on the unified normalization
        HashMap<Integer, Graph[]> subGraphs = new HashMap<>(graphs.size());
        Iterator iterator = graphs.entrySet().iterator();
        int groupCount = 0;
        while (iterator.hasNext()){
            Map.Entry graphEntry = (Map.Entry) iterator.next();
            int typeId = (int) graphEntry.getKey();
            Graph typeGraph = (Graph) graphEntry.getValue();
            // Graph is decomposed but normalization and sparse data construction is postponed
            // until all type-graphs of each sub-graph get ready
            // so as to map all of them with a single id mapper per sub-graph
            Graph[] decomposedType = typeGraph.decompose(partition, null);
            subGraphs.put(typeId, decomposedType);
            groupCount = decomposedType.length; // re-assigned redundantly!
        }
        // Put all types of each group into one multi-graph

        MultiGraph[] multiGraphs = new MultiGraph[groupCount];
        for(int m = 0 ; m < multiGraphs.length ; m++) multiGraphs[m] = new MultiGraph();
        iterator = subGraphs.entrySet().iterator();
        while (iterator.hasNext()){
            Map.Entry graphEntry = (Map.Entry) iterator.next();
            int typeId = (int) graphEntry.getKey();
            Graph[] typeSubGraphs = (Graph[]) graphEntry.getValue();
            // add sub graph of type 'typeId' to all multi-graph 'groupId'
            for(int groupId = 0 ; groupId < groupCount ; groupId++){
                multiGraphs[groupId].addGraph(typeId, typeSubGraphs[groupId]);
            }
        }
        // Normalize each multi-graph with a shared mapper across its type graphs
        for (MultiGraph multiGraph : multiGraphs) {
            multiGraph.normalize();
        }
        // Set attributes of multi-graph to its decomposed sub graphs
        copyAttributesTo(multiGraphs);
        return multiGraphs;
    }

    /**
     * Normalize type graphs of given multiGraph
     * based on row and column ids of all types combined
     * to have a shared node id among all type graphs
     */
    public MultiGraph normalize(){
        this.nodeMaxId = -1; // to be re-evaluated after normalization
        // row and column ids of all types combined
        int[][] ids = new int[2 * graphs.size()][];
        Iterator<Map.Entry<Integer, Graph>> iterator = graphs.entrySet().iterator();
        int index = 0;
        while(iterator.hasNext()){
            Map.Entry<Integer, Graph> typeGraph = iterator.next();
            Graph graph = typeGraph.getValue();
            ids[index++] = graph.getRows();
            ids[index++] = graph.getColumns();
        }
        // Normalize ids
        OpenIntIntHashMap[] mapToNormal = new OpenIntIntHashMap[2];
        mapToNormal[ROW] = Util.normalizeIds(ids);
        mapToNormal[COL] = (OpenIntIntHashMap) mapToNormal[ROW].clone();
        // Reconstruct the type graphs based on normalized lists
        ArrayList<Graph> newGraphs = new ArrayList<>(graphs.size());
        iterator = graphs.entrySet().iterator();
        while(iterator.hasNext()){
            Map.Entry<Integer, Graph> typeGraph = iterator.next();
            int typeId = typeGraph.getKey();
            ListMatrix normalizedList = (typeGraph.getValue())
                    .normalize(mapToNormal, null,false);
            newGraphs.add((Graph) new Graph(normalizedList).setId(typeId));
        }
        for(Graph newGraph : newGraphs){
            addGraph(newGraph.getId(), newGraph);
        }
        nodeCount = mapToNormal[ROW].size();
        return this;
    }

    /**
     * Normalize type graphs of given multiGraph
     * based on row and column ids of all types combined
     * to have a shared node id among all type graphs
     */
    public MultiGraph normalizeKeepRawIds(){
        this.nodeMaxId = -1; // to be re-evaluated after normalization
        // row and column ids of all types combined
        int[][] ids = new int[2 * graphs.size()][];
        Iterator<Map.Entry<Integer, Graph>> iterator = graphs.entrySet().iterator();
        int index = 0;
        while(iterator.hasNext()){
            Map.Entry<Integer, Graph> typeGraph = iterator.next();
            Graph graph = typeGraph.getValue();
            ids[index++] = graph.getRows();
            ids[index++] = graph.getColumns();
        }
        // Normalize ids
        OpenIntIntHashMap[] mapToNormal = new OpenIntIntHashMap[2];
        mapToNormal[ROW] = Util.normalizeIds(ids);
        mapToNormal[COL] = (OpenIntIntHashMap) mapToNormal[ROW].clone();
        // Reconstruct the type graphs based on normalized lists
        ArrayList<Graph> newGraphs = new ArrayList<>(graphs.size());
        iterator = graphs.entrySet().iterator();
        while(iterator.hasNext()){
            Map.Entry<Integer, Graph> typeGraph = iterator.next();
            int typeId = typeGraph.getKey();
            ListMatrix normalizedList = (typeGraph.getValue())
                    .normalizeKeepRawIds(mapToNormal, false);
            newGraphs.add((Graph) new Graph(normalizedList).setId(typeId));
        }
        for(Graph newGraph : newGraphs){
            addGraph(newGraph.getId(), newGraph);
        }
        nodeCount = mapToNormal[ROW].size();
        return this;
    }

    public MultiGraph addGraph(int typeId, Graph graph){
        boolean reAdded = graphs.containsKey(typeId);
        graphs.put(typeId, graph);
        if(graph == null) return this;
        nodeCount = Math.max(nodeCount, graph.getNodeMaxId() + 1);
        isEmpty = isEmpty && graph.isEmpty();
        if(!reAdded) edgeCount += graph.getEdgeCount();
        // Take the largest id map of type graphs as the representative
        if(nodeMaxId < graph.getNodeMaxId()){
            setToRaw(graph.getToRaw());
            setToNormal(graph.getToNormal());
            nodeMaxId = Math.max(nodeMaxId, graph.getNodeMaxId());
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
