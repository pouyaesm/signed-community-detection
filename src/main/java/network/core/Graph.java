package network.core;

import cern.colt.map.OpenIntIntHashMap;

import static network.core.ListMatrix.ROW;

public class Graph extends SparseMatrix {
    /**
     * Attributes per node
     */
    float[][] attributes;

    public Graph(ListMatrix listMatrix){
        super(listMatrix);
    }

    /**
     * Decompose graph based on the partition, normalize node ids of each sub-graph
     * Partitions are assumed to be normalized into 0..K-1
     * @param partition
     * @return
     */
    @Override
    public Graph[] decompose(int[] partition){
        SparseMatrix[] sparseMatrices = super.decompose(partition);
        Graph[] subGraphs = new Graph[sparseMatrices.length];
        for(int pr = 0 ; pr < subGraphs.length; pr++){
            subGraphs[pr] = (Graph) sparseMatrices[pr];
            if(!hasAttributes() || !subGraphs[pr].hasEdge()){
                continue; // no node attributes to copy or sub-graph is empty
            }
            ListMatrix subMatrix = subGraphs[pr].getListMatrix();
            // Copy node attributes to new partitions
            int subNodeCount = subMatrix.getRowCount();
            float[][] attributes = new float[subNodeCount][];
            OpenIntIntHashMap toNormal = getListMatrix().getToNormal()[0];
            for(int normalizedId = 0 ; normalizedId < subNodeCount ; normalizedId++){
                /*
                    raw id of normalized nodeId of sub-graphs
                    are mapped to the same rawId as their parent graph
                    so their raw id can be mapped back to nodeIds of this parent
                    using parent's toNormal
                 */
                int rawId = subMatrix.getToRaw()[0][normalizedId];
                attributes[normalizedId] = getAttributes()[toNormal.get(rawId)].clone();
            }
            subGraphs[pr].setAttributes(attributes);
        }
        return subGraphs;
    }

    /**
     * Fold the graph based on the partition, aggregate node attributes
     * @param partition
     * @return
     */
    @Override
    public Graph fold(int[] partition){
        Graph folded = (Graph) super.fold(partition);
        if (attributes == null) {
            return folded; // no attributes to aggregate
        }
        // Aggregate the attributes of nodes into their superNode
        ListMatrix foldedList = folded.getListMatrix();
        int groupIdRange = foldedList.getMaxRowId() + 1;
        int attributeCount = this.attributes[0].length;
        float[][] attributes = new float[groupIdRange][attributeCount];
        // Aggregate attribute of nodes into their group node
        // Assumption: superNodes are normalized version of their groupIds in partition
        OpenIntIntHashMap groupToSuperGroup = foldedList.getToNormal()[ROW];
        for(int nodeId = 0 ; nodeId < partition.length ; nodeId++){
            int superGroupId = groupToSuperGroup.get(partition[nodeId]);
            for(int attr = 0 ; attr < attributeCount ; attr++){
                attributes[superGroupId][attr] += this.attributes[nodeId][attr];
            }
        }
        folded.setAttributes(attributes);
        return folded;
    }

    /**
     * Returns the transition probability of going from "row" to "column"
     * @return
     */
    public Graph getTransitionProbability(){
        float[][] probabilities = new float[values.length][];
        for(int nodeId = 0 ; nodeId < values.length ; nodeId++){
            int[] neighbors = columnIndices[nodeId];
            probabilities[nodeId] = new float[neighbors.length];
            float totalOutLink = 0;
            for(int n = 0 ; n < neighbors.length ; n++){
                float value = values[nodeId][n];
                if(value > 0) {
                    totalOutLink += value;
                }
            }
            for(int n = 0 ; n < neighbors.length ; n++){
                float value = values[nodeId][n];
                if(value > 0) {
                    probabilities[nodeId][n] = value / totalOutLink;
                }else if(value < 0){
                    probabilities[nodeId][n] = 0;
                }
            }
        }
        Graph tGraph = clone();
        tGraph.values = probabilities;
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
        Graph graph = (Graph) super.filter(lowerBound, upperBound);
        graph.setAttributes(cloneAttributes());
        return graph;
    }

    /**
     * Number of graph nodes (row groupCount = column groupCount)
     * @return
     */
    public int getNodeCount(){
        return hasList() ? getListMatrix().getRowCount() : 0;
    }

    /**
     * Return maximum node id available
     * @return
     */
    public int getNodeMaxId(){
        return hasList() ? getListMatrix().getMaxRowId() : -1;
    }

    public int getEdgeCount(){
        return hasList() ? getListMatrix().getRows().length : 0;
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

    /**
     * Does graph have edge?
     * @return
     */
    public boolean hasEdge(){
        // Each member of the list matrix is an edge
        return hasList();
    }

    @Override
    public Graph newInstance() {
        return new Graph(null);
    }

    @Override
    public Graph clone() {
        Graph graph = (Graph) super.clone();
        graph.setAttributes(cloneAttributes());
        return graph;
    }
}
