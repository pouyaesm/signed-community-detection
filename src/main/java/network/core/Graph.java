package network.core;

import cern.colt.map.OpenIntIntHashMap;

public class Graph extends SparseMatrix {
    /**
     * Attributes per node
     */
    float[][] attributes;

    public Graph(){

    }

    public Graph(ListMatrix listMatrix){
        super(listMatrix);
    }

    public Graph(SparseMatrix sparseMatrix){
        super(sparseMatrix);
    }

    public Graph(Graph graph){
        super(graph);
        this.attributes = graph.getAttributes();
    }

    @Override
    public Graph[] decompose(int[] partition){
        return decompose(partition, null);
    }

    /**
     * Decompose graph based on the partition, normalize node ids of each sub-graph
     * Partitions are assumed to be normalized into 0..K-1
     * @param partition
     * @param mapToNormal
     * @return
     */
    @Override
    public Graph[] decompose(int[] partition, OpenIntIntHashMap[] mapToNormal){
        SparseMatrix[] sparseMatrices = super.decompose(partition, mapToNormal);
        Graph[] subGraphs = new Graph[sparseMatrices.length];
        // Convert array types
        for(int pr = 0 ; pr < subGraphs.length; pr++) {
            subGraphs[pr] = new Graph(sparseMatrices[pr]);
        }
        // Copy node attributes to new partitions
        copyAttributesTo(subGraphs);
        return subGraphs;
    }

    /**
     * Clone the attributes of graph into given graphs
     * based on shared normalized node ids between the graph and input graphs array
     * @param graphs
     */
    public void copyAttributesTo(Graph[] graphs){
        if(!hasAttributes()) return;
        for(int pr = 0 ; pr < graphs.length; pr++){
            if(graphs[pr] == null || graphs[pr].isEmpty()) continue;
            Graph graph = graphs[pr];
            int subNodeIdRange = graph.getNodeMaxId() + 1;
            float[][] attributes = new float[subNodeIdRange][];
            OpenIntIntHashMap toNormal = graph.getToNormal()[0];
            int[] toRaw = graph.getToRaw()[0];
            for(int normalizedId = 0 ; normalizedId < subNodeIdRange ; normalizedId++){
                /*
                    raw id of normalized nodeId of sub-graphs
                    are mapped to the same rawId as their parent graph
                    so their raw id can be mapped back to nodeIds of this parent
                    using parent's maps
                 */
                int rawId = toRaw[normalizedId];
                attributes[normalizedId] = getAttributes()[toNormal.get(rawId)].clone();
            }
            graph.setAttributes(attributes);
        }
    }
    /**
     * Fold the graph based on the partition, aggregate node attributes
     * @param partition
     * @return
     */
    @Override
    public Graph fold(int[] partition){
        Graph folded = new Graph(super.fold(partition));
        if (attributes == null) {
            return folded; // no attributes to aggregate
        }
        // Aggregate the attributes of nodes into their superNode
        folded.setAttributes(aggregateAttributes(partition, folded));
        return folded;
    }

    /**
     * Return aggregated attributes based on the partition and the folded graph
     * @param partition
     * @param foldedGraph
     * @return
     */
    public float[][] aggregateAttributes(int[] partition, Graph foldedGraph){
        if(!hasAttributes()) return null;
        int groupIdRange = foldedGraph.getNodeMaxId() + 1;
        int attributeCount = this.attributes[0].length;
        float[][] attributes = new float[groupIdRange][attributeCount];
        // Aggregate attribute of nodes into their group node
        // Assumption: superNodes are normalized version of their groupIds in partition
        OpenIntIntHashMap groupToSuperGroup = foldedGraph.getToNormal()[ROW];
        for(int nodeId = 0 ; nodeId < partition.length ; nodeId++){
            int superGroupId = groupToSuperGroup.get(partition[nodeId]);
            for(int attr = 0 ; attr < attributeCount ; attr++){
                attributes[superGroupId][attr] += this.attributes[nodeId][attr];
            }
        }
        return attributes;
    }
    /**
     * Returns the transition probability of going from "row" to "column"
     * @return
     */
    public Graph getTransitionProbability(){
        float[][] probabilities = new float[sparseValues.length][];
        for(int nodeId = 0 ; nodeId < sparseValues.length ; nodeId++){
            int[] neighbors = columnIndices[nodeId];
            probabilities[nodeId] = new float[neighbors.length];
            float totalOutLink = 0;
            for(int n = 0 ; n < neighbors.length ; n++){
                float value = sparseValues[nodeId][n];
                if(value > 0) {
                    totalOutLink += value;
                }
            }
            for(int n = 0 ; n < neighbors.length ; n++){
                float value = sparseValues[nodeId][n];
                if(value > 0) {
                    probabilities[nodeId][n] = value / totalOutLink;
                }else if(value < 0){
                    probabilities[nodeId][n] = 0;
                }
            }
        }
        Graph tGraph = clone();
        tGraph.sparseValues = probabilities;
        return tGraph;
    }

    /**
     * Return sparse matrix of only filtered values
     * @param lowerBound
     * @param upperBound
     * @see ListMatrix#filter(float, float)
     * @return
     */
    public Graph filter(float lowerBound, float upperBound){
        Graph graph = new Graph(super.filter(lowerBound, upperBound));
        graph.setAttributes(cloneAttributes());
        return graph;
    }

    /**
     * Number of graph nodes (row groupCount = column groupCount)
     * @return
     */
    public int getNodeCount(){
        return getRowCount();
    }

    /**
     * Return maximum node id available
     * @return
     */
    public int getNodeMaxId(){
        return getMaxRowId();
    }

    public int getEdgeCount(){
        return isEmpty() ? 0 : getRows().length;
    }

    public Graph setAttributes(float[][] attributes) {
        this.attributes = attributes;
        return this;
    }

    public float[][] getAttributes() {
        return attributes;
    }

    /**
     * Clone the attributes data
     * @return
     */
    public float[][] cloneAttributes(){
        if(attributes == null){
            return null;
        }
        float[][] clone = new float[attributes.length][];
        for(int a = 0 ; a < attributes.length ; a++){
            clone[a] = attributes[a] != null ? attributes[a].clone() : null;
        }
        return clone;
    }

    public boolean hasAttributes(){
        return attributes != null;
    }


    @Override
    public Graph clone() {
        Graph graph = new Graph(super.clone());
        graph.setAttributes(cloneAttributes());
        return graph;
    }
}
